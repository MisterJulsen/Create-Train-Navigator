package de.mrjulsen.crn.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.web.api.ApiVersion;
import de.mrjulsen.crn.web.api.BadRequestException;
import de.mrjulsen.crn.web.api.Request;
import de.mrjulsen.crn.web.api.Response;
import de.mrjulsen.crn.web.api.EndpointRegistry;
import de.mrjulsen.crn.web.api.HttpHeader;
import de.mrjulsen.crn.web.api.HttpMethod;
import de.mrjulsen.crn.web.api.PathPattern;
import de.mrjulsen.crn.web.api.ResultShaper;

final class ApiRouter implements HttpHandler {

    private static final int READ_CHUNK_BYTES = 8192;
    private static final int GZIP_MIN_BYTES = 512;

    private final WebServerSettings settings;

    ApiRouter(WebServerSettings settings) {
        this.settings = settings;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String origin = firstHeader(exchange.getRequestHeaders(), HttpHeader.ORIGIN);
        Response response;
        try {
            response = process(exchange);
        } catch (BadRequestException e) {
            response = Response.error(HttpURLConnection.HTTP_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            CreateRailwaysNavigator.LOGGER.error("Unhandled error in CRN web API", e);
            response = Response.error(HttpURLConnection.HTTP_INTERNAL_ERROR, "Internal server error");
        }
        Cors.apply(response, settings, origin);
        try {
            write(exchange, response, settings);
        } finally {
            exchange.close();
        }
    }

    private Response process(HttpExchange exchange) throws Exception {
        String origin = firstHeader(exchange.getRequestHeaders(), HttpHeader.ORIGIN);
        String rawMethod = exchange.getRequestMethod();
        if (HttpMethod.OPTIONS.name().equalsIgnoreCase(rawMethod)) {
            return Cors.preflight(settings, origin);
        }

        Optional<HttpMethod> method = HttpMethod.fromName(rawMethod);
        if (method.isEmpty()) {
            return Response.error(HttpURLConnection.HTTP_NOT_IMPLEMENTED, "Unsupported method: " + rawMethod);
        }

        URI uri = exchange.getRequestURI();
        List<String> segments = relativeSegments(exchange);
        if (segments.isEmpty()) {
            return Response.error(HttpURLConnection.HTTP_NOT_FOUND, "Missing API version in " + uri.getPath());
        }
        Optional<ApiVersion> version = ApiVersion.fromSlug(segments.get(0));
        if (version.isEmpty()) {
            return Response.error(HttpURLConnection.HTTP_NOT_FOUND, "Unknown API version: " + segments.get(0));
        }
        List<String> endpointSegments = segments.subList(1, segments.size());

        EndpointRegistry.Match match = EndpointRegistry.resolve(version.get(), method.get(), endpointSegments);
        switch (match.status()) {
            case NOT_FOUND:
                return Response.error(HttpURLConnection.HTTP_NOT_FOUND, "No endpoint for " + uri.getPath());
            case METHOD_NOT_ALLOWED:
                return Response.error(HttpURLConnection.HTTP_BAD_METHOD, "Method not allowed: " + rawMethod);
            default:
                break;
        }

        byte[] body = readBody(exchange.getRequestBody());
        if (body == null) {
            return Response.error(HttpURLConnection.HTTP_ENTITY_TOO_LARGE, "Request body too large");
        }

        Request request = new Request(
            method.get(),
            uri.getPath(),
            match.pathParameters(),
            parseQuery(uri.getRawQuery()),
            copyHeaders(exchange.getRequestHeaders()),
            body,
            exchange.getRemoteAddress()
        );

        Response response = match.route().handler().handle(request);
        if (response == null) {
            return Response.noContent();
        }
        ResultShaper.apply(request, response);
        return response;
    }

    private List<String> relativeSegments(HttpExchange exchange) {
        String contextPath = exchange.getHttpContext().getPath();
        String fullPath = exchange.getRequestURI().getPath();
        String remainder = fullPath.length() > contextPath.length()
            ? fullPath.substring(contextPath.length())
            : "";
        return PathPattern.split(remainder);
    }

    private byte[] readBody(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[READ_CHUNK_BYTES];
        int total = 0;
        int read;
        while ((read = in.read(chunk)) != -1) {
            total += read;
            if (total > settings.maxRequestBytes()) {
                return null;
            }
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }

    private static Map<String, List<String>> parseQuery(String rawQuery) {
        Map<String, List<String>> query = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return query;
        }
        for (String pair : rawQuery.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int separator = pair.indexOf('=');
            String key = separator >= 0 ? pair.substring(0, separator) : pair;
            String value = separator >= 0 ? pair.substring(separator + 1) : "";
            query.computeIfAbsent(urlDecode(key), k -> new ArrayList<>()).add(urlDecode(value));
        }
        return query;
    }

    private static Map<String, List<String>> copyHeaders(Headers headers) {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        headers.forEach((key, values) -> copy.put(key, List.copyOf(values)));
        return copy;
    }

    private static String firstHeader(Headers headers, String name) {
        List<String> values = headers.get(name);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private static String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static void write(HttpExchange exchange, Response response, WebServerSettings settings) throws IOException {
        Headers responseHeaders = exchange.getResponseHeaders();
        response.headers().forEach(responseHeaders::set);
        if (response.contentType() != null) {
            responseHeaders.set(HttpHeader.CONTENT_TYPE, response.contentType());
        }

        byte[] body = response.body();
        if (body == null || body.length == 0) {
            exchange.sendResponseHeaders(response.status(), -1);
            return;
        }
        if (settings.gzipEnabled() && body.length >= GZIP_MIN_BYTES && acceptsGzip(exchange)) {
            body = gzip(body);
            responseHeaders.set(HttpHeader.CONTENT_ENCODING, "gzip");
            responseHeaders.add(HttpHeader.VARY, HttpHeader.ACCEPT_ENCODING);
        }
        exchange.sendResponseHeaders(response.status(), body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static boolean acceptsGzip(HttpExchange exchange) {
        List<String> values = exchange.getRequestHeaders().get(HttpHeader.ACCEPT_ENCODING);
        if (values == null) {
            return false;
        }
        for (String value : values) {
            if (value != null && value.toLowerCase().contains("gzip")) {
                return true;
            }
        }
        return false;
    }

    private static byte[] gzip(byte[] data) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(Math.max(64, data.length / 3));
        try (GZIPOutputStream gzip = new GZIPOutputStream(buffer)) {
            gzip.write(data);
        }
        return buffer.toByteArray();
    }
}
