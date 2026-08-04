package de.mrjulsen.crn.web;

import java.util.List;

import de.mrjulsen.crn.web.api.Response;
import de.mrjulsen.crn.web.api.HttpHeader;

final class Cors {

    private static final String ALLOW_ALL = "*";
    private static final String ALLOWED_METHODS = "GET, POST, PUT, PATCH, DELETE, OPTIONS";
    private static final String ALLOWED_HEADERS = HttpHeader.CONTENT_TYPE + ", " + HttpHeader.AUTHORIZATION;
    private static final String EXPOSED_HEADERS = HttpHeader.X_TOTAL_COUNT + ", " + HttpHeader.X_LIMIT + ", " + HttpHeader.X_OFFSET;
    private static final String PREFLIGHT_MAX_AGE = "3600";

    private Cors() {}

    static void apply(Response response, WebServerSettings settings, String origin) {
        String allowed = resolveAllowedOrigin(settings.corsOrigins(), origin);
        if (allowed == null) {
            return;
        }
        response.header(HttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN, allowed);
        response.header(HttpHeader.ACCESS_CONTROL_EXPOSE_HEADERS, EXPOSED_HEADERS);
        response.header(HttpHeader.VARY, HttpHeader.ORIGIN);
    }

    static Response preflight(WebServerSettings settings, String origin) {
        Response response = Response.noContent();
        apply(response, settings, origin);
        response.header(HttpHeader.ACCESS_CONTROL_ALLOW_METHODS, ALLOWED_METHODS);
        response.header(HttpHeader.ACCESS_CONTROL_ALLOW_HEADERS, ALLOWED_HEADERS);
        response.header(HttpHeader.ACCESS_CONTROL_MAX_AGE, PREFLIGHT_MAX_AGE);
        return response;
    }

    private static String resolveAllowedOrigin(List<String> allowedOrigins, String origin) {
        if (origin == null || allowedOrigins.isEmpty()) {
            return null;
        }
        if (allowedOrigins.contains(ALLOW_ALL)) {
            return ALLOW_ALL;
        }
        return allowedOrigins.contains(origin) ? origin : null;
    }
}
