package de.mrjulsen.crn.web;

import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jetty.http.pathmap.MatchedResource;
import org.eclipse.jetty.http.pathmap.PathMappings;
import org.eclipse.jetty.http.pathmap.PathSpec;
import org.eclipse.jetty.http.pathmap.UriTemplatePathSpec;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.web.api.ApiResult;
import de.mrjulsen.crn.web.api.BadRequestException;
import de.mrjulsen.crn.web.api.EndpointRegistry;
import de.mrjulsen.crn.web.api.HttpHeader;
import de.mrjulsen.crn.web.api.HttpMethod;
import de.mrjulsen.crn.web.api.NotFoundException;
import de.mrjulsen.crn.web.api.RequestParams;
import de.mrjulsen.crn.web.api.ResultShaper;

final class ApiHandler extends Handler.Abstract {

    private final WebServerSettings settings;
    private final PathMappings<Map<HttpMethod, EndpointRegistry.Route>> mappings;

    ApiHandler(WebServerSettings settings, PathMappings<Map<HttpMethod, EndpointRegistry.Route>> mappings) {
        this.settings = settings;
        this.mappings = mappings;
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) {
        String pathInContext = Request.getPathInContext(request);
        MatchedResource<Map<HttpMethod, EndpointRegistry.Route>> matched = mappings.getMatched(pathInContext);
        if (matched == null) {
            return false;
        }
        long startNanos = System.nanoTime();
        RequestMetrics metrics = new RequestMetrics();
        metrics.queueWaitNanos = Math.max(0L, startNanos - request.getBeginNanoTime());

        Optional<HttpMethod> method = HttpMethod.fromName(request.getMethod());
        EndpointRegistry.Route route = method.map(matched.getResource()::get).orElse(null);

        ApiResult result;
        if (route == null) {
            result = ApiResult.error(HttpURLConnection.HTTP_BAD_METHOD, "Method not allowed: " + request.getMethod());
        } else {
            request.setAttribute(RequestParams.PATH_ATTRIBUTE, pathParameters(matched, pathInContext));
            try {
                long buildStart = System.nanoTime();
                result = invoke(route, request);
                metrics.buildNanos = System.nanoTime() - buildStart;
            } catch (BadRequestException e) {
                result = ApiResult.error(HttpURLConnection.HTTP_BAD_REQUEST, e.getMessage());
            } catch (NotFoundException e) {
                result = ApiResult.error(HttpURLConnection.HTTP_NOT_FOUND, e.getMessage());
            } catch (Exception e) {
                CreateRailwaysNavigator.LOGGER.error("Unhandled error in CRN web API", e);
                result = ApiResult.error(HttpURLConnection.HTTP_INTERNAL_ERROR, "Internal server error");
            }
        }

        write(request, response, callback, result, metrics, startNanos);
        return true;
    }

    private ApiResult invoke(EndpointRegistry.Route route, Request request) throws Exception {
        ApiResult result = toResult(route.handler().handle(request));
        if (result.isJson() && result.status() >= 200 && result.status() < 300) {
            Object shaped = ResultShaper.shape(RequestParams.queryFields(request), result.jsonPayload(), result::header);
            result.jsonPayload(shaped);
        }
        return result;
    }

    private static ApiResult toResult(Object payload) {
        if (payload == null) {
            return ApiResult.noContent();
        }
        if (payload instanceof ApiResult result) {
            return result;
        }
        return ApiResult.json(payload);
    }

    private static Map<String, String> pathParameters(MatchedResource<?> matched, String pathInContext) {
        PathSpec spec = matched.getPathSpec();
        if (spec instanceof UriTemplatePathSpec template) {
            return template.getPathParams(pathInContext);
        }
        return Map.of();
    }

    private void write(Request request, Response response, Callback callback, ApiResult result, RequestMetrics metrics, long startNanos) {
        result.headers().forEach((name, value) -> response.getHeaders().put(name, value));
        if (result.contentType() != null) {
            response.getHeaders().put(HttpHeader.CONTENT_TYPE, result.contentType());
        }
        response.setStatus(result.status());

        byte[] body;
        long serializeStart = System.nanoTime();
        try {
            body = result.body();
        } catch (Exception e) {
            CreateRailwaysNavigator.LOGGER.error("Failed to serialize CRN web API response", e);
            Response.writeError(request, response, callback, HttpURLConnection.HTTP_INTERNAL_ERROR);
            return;
        }
        metrics.serializeNanos = System.nanoTime() - serializeStart;
        metrics.payloadBytes = body == null ? 0 : body.length;

        logRequest(request, result, metrics, System.nanoTime() - startNanos);

        if (body == null || body.length <= 0) {
            response.write(true, ByteBuffer.allocate(0), callback);
            return;
        }
        metrics.sentBytes = body.length;
        response.write(true, ByteBuffer.wrap(body), callback);
    }

    private void logRequest(Request request, ApiResult result, RequestMetrics metrics, long totalNanos) {
        if (settings.debugTiming()) {
            CreateRailwaysNavigator.LOGGER.info("CRN web API {} {} -> {} [wait {}, build {}, serialize {}, total {}, {}]",
                request.getMethod(),
                request.getHttpURI().getPath(),
                result.status(),
                formatMillis(metrics.queueWaitNanos),
                formatMillis(metrics.buildNanos),
                formatMillis(metrics.serializeNanos),
                formatMillis(totalNanos),
                formatSize(metrics));
        } else if (settings.requestLog()) {
            CreateRailwaysNavigator.LOGGER.info("CRN web API {} {} -> {}", request.getMethod(), request.getHttpURI().getPath(), result.status());
        }
    }

    private static String formatMillis(long nanos) {
        if (nanos < 0) {
            return "n/a";
        }
        return String.format(Locale.ROOT, "%.1fms", nanos / 1000000f);
    }

    private static String formatSize(RequestMetrics metrics) {
        if (metrics.payloadBytes <= 0) {
            return "empty";
        }
        return formatBytes(metrics.payloadBytes);
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + "B";
        }
        return String.format(Locale.ROOT, "%.1fKB", bytes / 1024f);
    }
}
