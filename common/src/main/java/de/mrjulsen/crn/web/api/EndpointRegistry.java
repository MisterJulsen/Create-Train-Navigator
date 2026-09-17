package de.mrjulsen.crn.web.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The central place to register web API endpoints.
 * Register a handler under a method and a path and the router will expose it below the API root at
 * {@code /<namespace>/api/<version>/<path>}:
 *
 * <pre>{@code
 * EndpointRegistry.get("hello", new HelloEndpoint());          // GET  /.../api/v1/hello
 * EndpointRegistry.get("train/{id}", new TrainEndpoint());     // GET  /.../api/v1/train/42
 * EndpointRegistry.post("train", new CreateTrainEndpoint());   // POST /.../api/v1/train
 * }</pre>
 *
 * A path may carry {@link Route#alias(String...) aliases} that point at the same handler, and an
 * endpoint may be pinned to a specific {@link ApiVersion} (it defaults to {@link ApiVersion#latest()}):
 *
 * <pre>{@code
 * EndpointRegistry.get("hello", new HelloEndpoint()).alias("hello-world");
 * EndpointRegistry.get(ApiVersion.V1, "status", new StatusEndpoint());
 * }</pre>
 *
 * Registration happens once during mod init, before the server starts; lookups afterwards are
 * read-only, so this is safe to query from the request threads.
 */
public final class EndpointRegistry {

    public enum ResolveStatus {
        MATCHED,
        NOT_FOUND,
        METHOD_NOT_ALLOWED
    }

    public static final class Route {

        private final ApiVersion version;
        private final HttpMethod method;
        private final IEndpointHandler handler;
        private final List<PathPattern> patterns = new ArrayList<>();

        private Route(ApiVersion version, HttpMethod method, PathPattern canonical, IEndpointHandler handler) {
            this.version = version;
            this.method = method;
            this.handler = handler;
            this.patterns.add(canonical);
        }

        public Route alias(String... paths) {
            for (String path : paths) {
                PathPattern pattern = PathPattern.of(path);
                ensureUnique(version, method, pattern);
                patterns.add(pattern);
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

        public List<PathPattern> patterns() {
            return Collections.unmodifiableList(patterns);
        }

        private Optional<PatternMatch> match(List<String> pathSegments) {
            for (PathPattern pattern : patterns) {
                Optional<Map<String, String>> parameters = pattern.match(pathSegments);
                if (parameters.isPresent()) {
                    return Optional.of(new PatternMatch(pattern, parameters.get()));
                }
            }
            return Optional.empty();
        }


        private record PatternMatch(PathPattern pattern, Map<String, String> parameters) {}
    }

    public record Match(ResolveStatus status, Route route, Map<String, String> pathParameters) {

        static Match notFound() {
            return new Match(ResolveStatus.NOT_FOUND, null, Map.of());
        }

        static Match methodNotAllowed() {
            return new Match(ResolveStatus.METHOD_NOT_ALLOWED, null, Map.of());
        }

        static Match matched(Route route, Map<String, String> pathParameters) {
            return new Match(ResolveStatus.MATCHED, route, pathParameters);
        }
    }



    private static final List<Route> ROUTES = new ArrayList<>();


    private EndpointRegistry() {}

    public static Route register(ApiVersion version, HttpMethod method, String path, IEndpointHandler handler) {
        PathPattern pattern = PathPattern.of(path);
        ensureUnique(version, method, pattern);
        Route route = new Route(version, method, pattern, handler);
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


    public static Match resolve(ApiVersion version, HttpMethod method, List<String> pathSegments) {
        Route bestRoute = null;
        Map<String, String> bestParameters = null;
        int bestSpecificity = -1;
        boolean pathMatchedOtherMethod = false;
        for (Route route : ROUTES) {
            if (route.version() != version) {
                continue;
            }
            Optional<Route.PatternMatch> match = route.match(pathSegments);
            if (match.isEmpty()) {
                continue;
            }
            if (route.method() != method) {
                pathMatchedOtherMethod = true;
                continue;
            }
            int specificity = match.get().pattern().staticSegmentCount();
            if (specificity > bestSpecificity) {
                bestSpecificity = specificity;
                bestRoute = route;
                bestParameters = match.get().parameters();
            }
        }
        if (bestRoute != null) {
            return Match.matched(bestRoute, bestParameters);
        }
        return pathMatchedOtherMethod ? Match.methodNotAllowed() : Match.notFound();
    }

    public static List<Route> all() {
        return Collections.unmodifiableList(ROUTES);
    }

    public static int size() {
        return ROUTES.size();
    }

    private static void ensureUnique(ApiVersion version, HttpMethod method, PathPattern pattern) {
        for (Route route : ROUTES) {
            if (route.version() != version || route.method() != method) {
                continue;
            }
            for (PathPattern existing : route.patterns()) {
                if (existing.raw().equals(pattern.raw())) {
                    throw new IllegalStateException("Duplicate web endpoint: " + version.slug() + " " + method + " /" + pattern.raw());
                }
            }
        }
    }
}
