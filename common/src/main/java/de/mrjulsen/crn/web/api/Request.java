package de.mrjulsen.crn.web.api;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;

import de.mrjulsen.crn.api.json.JsonConvert;

public final class Request {

    private static final String BEARER_PREFIX = "Bearer ";

    private final HttpMethod method;
    private final String path;
    private final Map<String, String> pathParameters;
    private final Map<String, List<String>> queryParameters;
    private final Map<String, List<String>> headers;
    private final byte[] body;
    private final InetSocketAddress remoteAddress;

    public Request(HttpMethod method, String path, Map<String, String> pathParameters,
                   Map<String, List<String>> queryParameters, Map<String, List<String>> headers,
                   byte[] body, InetSocketAddress remoteAddress
    ) {
        this.method = method;
        this.path = path;
        this.pathParameters = Map.copyOf(pathParameters);
        this.queryParameters = Map.copyOf(queryParameters);
        this.headers = caseInsensitive(headers);
        this.body = body;
        this.remoteAddress = remoteAddress;
    }

    public HttpMethod method() {
        return method;
    }

    public String path() {
        return path;
    }

    public InetSocketAddress remoteAddress() {
        return remoteAddress;
    }

    public Optional<String> pathParameter(String name) {
        return Optional.ofNullable(pathParameters.get(name));
    }

    public <T> T pathParameter(String name, Function<String, T> parser) {
        String raw = pathParameters.get(name);
        if (raw == null) {
            throw new BadRequestException("Missing path parameter: " + name);
        }
        return convert(name, raw, parser);
    }

    public Optional<String> query(String name) {
        List<String> values = queryParameters.get(name);
        return values == null || values.isEmpty() ? Optional.empty() : Optional.of(values.get(0));
    }

    public <T> Optional<T> query(String name, Function<String, T> parser) {
        return query(name).map(raw -> convert(name, raw, parser));
    }

    public String requireQuery(String name) {
        return query(name).orElseThrow(() -> new BadRequestException("Missing required query parameter: " + name));
    }

    public <T> T requireQuery(String name, Function<String, T> parser) {
        return convert(name, requireQuery(name), parser);
    }

    public List<String> queryValues(String name) {
        List<String> out = new ArrayList<>();
        for (String value : queryParameters.getOrDefault(name, List.of())) {
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) {
                out.add(trimmed);
            }
        }
        return out;
    }

    public <T> List<T> queryValues(String name, Function<String, T> parser) {
        List<T> out = new ArrayList<>();
        for (String value : queryValues(name)) {
            out.add(convert(name, value, parser));
        }
        return out;
    }

    public List<String> requireValues(String name) {
        return requireNonEmpty(name, queryValues(name));
    }

    public <T> List<T> requireValues(String name, Function<String, T> parser) {
        return requireNonEmpty(name, queryValues(name, parser));
    }

    public boolean hasQuery(String name) {
        return queryParameters.containsKey(name);
    }

    public Optional<String> header(String name) {
        List<String> values = headers.get(name);
        return values == null || values.isEmpty() ? Optional.empty() : Optional.of(values.get(0));
    }

    public Optional<String> authorization() {
        return header(HttpHeader.AUTHORIZATION);
    }

    public Optional<String> bearerToken() {
        return authorization()
            .filter(value -> value.startsWith(BEARER_PREFIX))
            .map(value -> value.substring(BEARER_PREFIX.length()).trim());
    }

    public byte[] body() {
        return body;
    }

    public String bodyAsString() {
        return new String(body, StandardCharsets.UTF_8);
    }

    public <T> T bodyAsJson(Class<T> type) {
        return JsonConvert.fromJson(bodyAsString(), type);
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

    private static Map<String, List<String>> caseInsensitive(Map<String, List<String>> source) {
        Map<String, List<String>> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        source.forEach((key, values) -> map.put(key, List.copyOf(values)));
        return Collections.unmodifiableMap(map);
    }
}
