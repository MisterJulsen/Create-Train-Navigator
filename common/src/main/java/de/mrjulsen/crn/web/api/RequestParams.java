package de.mrjulsen.crn.web.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.Fields;

public final class RequestParams {

    public static final String PATH_ATTRIBUTE = "crn.pathParameters";
    private static final String QUERY_ATTRIBUTE = "crn.queryFields";

    private RequestParams() {}

    public static Fields queryFields(Request request) {
        Object cached = request.getAttribute(QUERY_ATTRIBUTE);
        if (cached instanceof Fields fields) {
            return fields;
        }
        Fields fields = Request.extractQueryParameters(request);
        request.setAttribute(QUERY_ATTRIBUTE, fields);
        return fields;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> pathParameters(Request request) {
        Object value = request.getAttribute(PATH_ATTRIBUTE);
        return value instanceof Map ? (Map<String, String>) value : Map.of();
    }

    public static String fullPath(Request request) {
        return request.getHttpURI().getPath();
    }

    public static String path(Request request, String name) {
        String raw = pathParameters(request).get(name);
        if (raw == null) {
            throw new BadRequestException("Missing path parameter: " + name);
        }
        return raw;
    }

    public static <T> T path(Request request, String name, Function<String, T> parser) {
        return convert(name, path(request, name), parser);
    }

    public static Optional<String> query(Request request, String name) {
        String value = queryFields(request).getValue(name);
        return value == null ? Optional.empty() : Optional.of(value);
    }

    public static <T> Optional<T> query(Request request, String name, Function<String, T> parser) {
        return query(request, name).map(raw -> convert(name, raw, parser));
    }

    public static String requireQuery(Request request, String name) {
        return query(request, name).orElseThrow(() -> new BadRequestException("Missing required query parameter: " + name));
    }

    public static <T> T requireQuery(Request request, String name, Function<String, T> parser) {
        return convert(name, requireQuery(request, name), parser);
    }

    public static List<String> queryValues(Request request, String name) {
        List<String> out = new ArrayList<>();
        for (String value : queryFields(request).getValuesOrEmpty(name)) {
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) {
                out.add(trimmed);
            }
        }
        return out;
    }

    public static <T> List<T> queryValues(Request request, String name, Function<String, T> parser) {
        List<T> out = new ArrayList<>();
        for (String value : queryValues(request, name)) {
            out.add(convert(name, value, parser));
        }
        return out;
    }

    public static List<String> requireValues(Request request, String name) {
        return requireNonEmpty(name, queryValues(request, name));
    }

    public static <T> List<T> requireValues(Request request, String name, Function<String, T> parser) {
        return requireNonEmpty(name, queryValues(request, name, parser));
    }

    public static boolean hasQuery(Request request, String name) {
        return queryFields(request).get(name) != null;
    }

    private static <T> List<T> requireNonEmpty(String name, List<T> values) {
        if (values.isEmpty()) {
            throw new BadRequestException("Missing required query parameter: " + name);
        }
        return values;
    }

    private static <T> T convert(String name, String raw, Function<String, T> parser) {
        try {
            return parser.apply(raw);
        } catch (RuntimeException e) {
            String expected = parser instanceof ParamType<?> type ? "a " + type.typeName() : "a valid value";
            throw new BadRequestException("Parameter '" + name + "' must be " + expected + ", got: " + raw);
        }
    }
}
