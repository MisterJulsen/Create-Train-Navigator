package de.mrjulsen.crn.web.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.http.pathmap.PathMappings;
import org.eclipse.jetty.http.pathmap.UriTemplatePathSpec;

public final class EndpointRegistry {

    public static final class Route {

        private final ApiVersion version;
        private final HttpMethod method;
        private final IEndpointHandler handler;
        private final List<String> paths = new ArrayList<>();

        private Route(ApiVersion version, HttpMethod method, String canonical, IEndpointHandler handler) {
            this.version = version;
            this.method = method;
            this.handler = handler;
            this.paths.add(canonical);
        }

        public Route alias(String... paths) {
            for (String path : paths) {
                String normalized = normalize(path);
                ensureUnique(version, method, normalized);
                this.paths.add(normalized);
            }
            return this;
        }

        public ApiVersion version() {
            return version;
        }

        public HttpMethod method() {
            return method;
        }

        public IEndpointHandler handler() {
            return handler;
        }

        public List<String> paths() {
            return Collections.unmodifiableList(paths);
        }
    }

    private static final List<Route> ROUTES = new ArrayList<>();

    private EndpointRegistry() {}

    public static Route register(ApiVersion version, HttpMethod method, String path, IEndpointHandler handler) {
        String normalized = normalize(path);
        ensureUnique(version, method, normalized);
        Route route = new Route(version, method, normalized, handler);
        ROUTES.add(route);
        return route;
    }

    public static Route register(HttpMethod method, String path, IEndpointHandler handler) {
        return register(ApiVersion.latest(), method, path, handler);
    }

    public static Route registerGet(String path, IEndpointHandler handler) {
        return register(HttpMethod.GET, path, handler);
    }

    public static Route registerGet(ApiVersion version, String path, IEndpointHandler handler) {
        return register(version, HttpMethod.GET, path, handler);
    }

    public static Route registerPost(String path, IEndpointHandler handler) {
        return register(HttpMethod.POST, path, handler);
    }

    public static Route registerPost(ApiVersion version, String path, IEndpointHandler handler) {
        return register(version, HttpMethod.POST, path, handler);
    }

    public static Route registerPut(String path, IEndpointHandler handler) {
        return register(HttpMethod.PUT, path, handler);
    }

    public static Route registerPut(ApiVersion version, String path, IEndpointHandler handler) {
        return register(version, HttpMethod.PUT, path, handler);
    }

    public static Route registerPatch(String path, IEndpointHandler handler) {
        return register(HttpMethod.PATCH, path, handler);
    }

    public static Route registerPatch(ApiVersion version, String path, IEndpointHandler handler) {
        return register(version, HttpMethod.PATCH, path, handler);
    }

    public static Route registerDelete(String path, IEndpointHandler handler) {
        return register(HttpMethod.DELETE, path, handler);
    }

    public static Route registerDelete(ApiVersion version, String path, IEndpointHandler handler) {
        return register(version, HttpMethod.DELETE, path, handler);
    }

    public static PathMappings<Map<HttpMethod, Route>> mappingsFor(ApiVersion version) {
        Map<String, Map<HttpMethod, Route>> byPath = new LinkedHashMap<>();
        for (Route route : ROUTES) {
            if (route.version() != version) {
                continue;
            }
            for (String path : route.paths()) {
                byPath.computeIfAbsent(path, k -> new EnumMap<>(HttpMethod.class)).put(route.method(), route);
            }
        }
        PathMappings<Map<HttpMethod, Route>> mappings = new PathMappings<>();
        byPath.forEach((path, methods) -> mappings.put(new UriTemplatePathSpec("/" + path), methods));
        return mappings;
    }

    public static List<Route> all() {
        return Collections.unmodifiableList(ROUTES);
    }

    public static int size() {
        return ROUTES.size();
    }

    private static String normalize(String path) {
        String trimmed = path.strip();
        while (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static void ensureUnique(ApiVersion version, HttpMethod method, String path) {
        for (Route route : ROUTES) {
            if (route.version() != version || route.method() != method) {
                continue;
            }
            if (route.paths().contains(path)) {
                throw new IllegalStateException("Duplicate web endpoint: " + version.slug() + " " + method + " /" + path);
            }
        }
    }
}
