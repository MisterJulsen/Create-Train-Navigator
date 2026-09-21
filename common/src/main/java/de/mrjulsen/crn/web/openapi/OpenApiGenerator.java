package de.mrjulsen.crn.web.openapi;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.web.WebServer;
import de.mrjulsen.crn.web.annotation.OpenApiDescription;
import de.mrjulsen.crn.web.annotation.QueryParam;
import de.mrjulsen.crn.web.api.ApiTagRegistry;
import de.mrjulsen.crn.web.api.ApiVersion;
import de.mrjulsen.crn.web.api.EndpointRegistry;
import de.mrjulsen.crn.web.api.GlobalParameters;
import dev.architectury.platform.Mod;
import dev.architectury.platform.Platform;

public final class OpenApiGenerator {

    public static final String OPEN_API_VERSION = "3.1.0";

    private OpenApiGenerator() {}

    public static Map<String, Object> generate(ApiVersion version) {
        SchemaGenerator schemas = new SchemaGenerator();
        Set<ApiTagRegistry.ApiTag> tags = new LinkedHashSet<>();
        Set<String> operationIds = new LinkedHashSet<>();
        Map<String, Map<String, Object>> paths = new LinkedHashMap<>();

        for (EndpointRegistry.Route route : EndpointRegistry.all()) {
            if (route.version() != version) {
                continue;
            }
            EndpointDocumentation doc = safeDoc(route.handler());
            for (String rawPath : route.paths()) {
                String path = "/" + rawPath;
                Map<String, Object> operations = paths.computeIfAbsent(path, k -> new LinkedHashMap<>());
                Map<String, Object> operation = buildOperation(route, rawPath, doc, schemas, tags, operationIds);
                operations.put(route.method().name().toLowerCase(), operation);
            }
        }

        Mod mod = Platform.isModLoaded(CreateRailwaysNavigator.MOD_ID) ? Platform.getMod(CreateRailwaysNavigator.MOD_ID) : null;
        Map<String, Object> document = new LinkedHashMap<>();
        document.put(OpenApiKeys.OPENAPI, OPEN_API_VERSION);
        if (mod != null) document.put(OpenApiKeys.INFO, getModInfo(version, mod));
        document.put(OpenApiKeys.SERVERS, getServers(version));
        document.put(OpenApiKeys.TAGS, getTags(tags));
        document.put(OpenApiKeys.PATHS, paths);
        document.put(OpenApiKeys.COMPONENTS, getComponents(schemas));
        if (mod != null) {
            Map<String, Object> externalDocs = getExternalDocs(mod);
            if (externalDocs != null) {
                document.put(OpenApiKeys.EXTERNAL_DOCS, externalDocs);
            }
        }
        return document;
    }

    private static Map<String, Object> buildOperation(EndpointRegistry.Route route, String path, EndpointDocumentation doc, SchemaGenerator schemas, Set<ApiTagRegistry.ApiTag> tags, Set<String> operationIds) {
        Map<String, Object> operation = new LinkedHashMap<>();
        operation.put(OpenApiKeys.OPERATION_ID, operationId(route.method().name(), path, operationIds));

        List<ApiTagRegistry.ApiTag> operationTags = doc != null ? doc.tags() : List.of();
        tags.addAll(operationTags);
        if (!operationTags.isEmpty()) {
            List<String> tagNames = new ArrayList<>();
            for (ApiTagRegistry.ApiTag tag : operationTags) {
                tagNames.add(tag.getName());
            }
            operation.put(OpenApiKeys.TAGS, tagNames);
        }

        if (doc != null && doc.summary() != null) {
            operation.put(OpenApiKeys.SUMMARY, doc.summary());
        }
        if (doc != null && doc.description() != null) {
            operation.put(OpenApiKeys.DESCRIPTION, doc.description());
        }
        if (doc != null && doc.deprecated()) {
            operation.put(OpenApiKeys.DEPRECATED, true);
        }

        List<Object> parameters = parameters(path, doc, schemas);
        if (!parameters.isEmpty()) {
            operation.put(OpenApiKeys.PARAMETERS, parameters);
        }

        if (doc != null && doc.requestBody() != null) {
            operation.put(OpenApiKeys.REQUEST_BODY, requestBody(doc.requestBody(), schemas));
        }

        operation.put(OpenApiKeys.RESPONSES, responses(doc, schemas));
        return operation;
    }

    private static List<Object> parameters(String path, EndpointDocumentation doc, SchemaGenerator schemas) {
        List<Object> parameters = new ArrayList<>();
        for (String name : pathParameters(path)) {
            Map<String, Object> parameter = new LinkedHashMap<>();
            parameter.put(OpenApiKeys.NAME, name);
            parameter.put(OpenApiKeys.IN, OpenApiKeys.IN_PATH);
            parameter.put(OpenApiKeys.REQUIRED, true);
            parameter.put(OpenApiKeys.SCHEMA, Schemas.typed(OpenApiKeys.TYPE_STRING));
            parameters.add(parameter);
        }
        if (doc != null && doc.queryModel() != null) {
            parameters.addAll(queryParameters(doc.queryModel(), schemas));
        }
        if (doc != null) {
            for (EndpointDocumentation.QueryParameter manual : doc.queryParams()) {
                parameters.add(manualQueryParameter(manual));
            }
        }
        if (doc != null && doc.shapeable()) {
            for (GlobalParameters shaping : GlobalParameters.values()) {
                if (!shaping.isForArrayOnly() || doc.arrayResponse()) {
                    parameters.add(ref(OpenApiKeys.PARAMETER_REF_PREFIX + shaping.component()));
                }
            }
        }
        return parameters;
    }

    private static List<Object> queryParameters(Class<?> model, SchemaGenerator schemas) {
        List<Object> parameters = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (RecordComponent component : recordComponents(model)) {
            QueryParam param = component.getAnnotation(QueryParam.class);
            if (param != null && seen.add(param.value())) {
                parameters.add(queryParameter(param, component.getGenericType(), component.getAnnotation(OpenApiDescription.class), schemas));
            }
        }
        for (Method method : model.getMethods()) {
            QueryParam param = method.getAnnotation(QueryParam.class);
            if (param == null || method.getParameterCount() != 1 || !seen.add(param.value())) {
                continue;
            }
            Parameter parameter = method.getParameters()[0];
            OpenApiDescription description = firstNonNull(method.getAnnotation(OpenApiDescription.class), parameter.getAnnotation(OpenApiDescription.class));
            parameters.add(queryParameter(param, parameter.getParameterizedType(), description, schemas));
        }
        return parameters;
    }

    private static Map<String, Object> queryParameter(QueryParam param, java.lang.reflect.Type type, OpenApiDescription description, SchemaGenerator schemas) {
        Map<String, Object> parameter = new LinkedHashMap<>();
        parameter.put(OpenApiKeys.NAME, param.value());
        parameter.put(OpenApiKeys.IN, OpenApiKeys.IN_QUERY);
        parameter.put(OpenApiKeys.REQUIRED, param.required());
        if (description != null && !description.value().isBlank()) {
            parameter.put(OpenApiKeys.DESCRIPTION, description.value());
        }
        if (description != null && !description.example().isBlank()) {
            parameter.put(OpenApiKeys.EXAMPLE, description.example());
        }
        Map<String, Object> schema = schemas.querySchema(type);
        if (description != null && !description.pattern().isBlank()) {
            leafSchema(schema).put(OpenApiKeys.PATTERN, description.pattern());
        }
        applyArrayStyle(parameter, schema);
        parameter.put(OpenApiKeys.SCHEMA, schema);
        return parameter;
    }

    private static void applyArrayStyle(Map<String, Object> parameter, Map<String, Object> schema) {
        if (OpenApiKeys.TYPE_ARRAY.equals(schema.get(OpenApiKeys.TYPE))) {
            parameter.put(OpenApiKeys.STYLE, OpenApiKeys.STYLE_FORM);
            parameter.put(OpenApiKeys.EXPLODE, true);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> leafSchema(Map<String, Object> schema) {
        Object items = schema.get(OpenApiKeys.ITEMS);
        return items instanceof Map ? leafSchema((Map<String, Object>) items) : schema;
    }

    private static Map<String, Object> manualQueryParameter(EndpointDocumentation.QueryParameter spec) {
        Map<String, Object> parameter = new LinkedHashMap<>();
        parameter.put(OpenApiKeys.NAME, spec.name());
        parameter.put(OpenApiKeys.IN, OpenApiKeys.IN_QUERY);
        parameter.put(OpenApiKeys.REQUIRED, spec.required());
        if (spec.description() != null && !spec.description().isBlank()) {
            parameter.put(OpenApiKeys.DESCRIPTION, spec.description());
        }
        if (spec.example() != null) {
            parameter.put(OpenApiKeys.EXAMPLE, spec.example());
        }
        Map<String, Object> schema = Schemas.typed(spec.type());
        if (!spec.enumValues().isEmpty()) {
            schema.put(OpenApiKeys.ENUM, spec.enumValues());
        }
        if (spec.defaultValue() != null) {
            schema.put(OpenApiKeys.DEFAULT, spec.defaultValue());
        }
        parameter.put(OpenApiKeys.SCHEMA, schema);
        return parameter;
    }

    private static <T> T firstNonNull(T a, T b) {
        return a != null ? a : b;
    }

    private static Map<String, Object> requestBody(RequestBodySpec spec, SchemaGenerator schemas) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (spec.description() != null) {
            body.put(OpenApiKeys.DESCRIPTION, spec.description());
        }
        body.put(OpenApiKeys.REQUIRED, spec.required());
        body.put(OpenApiKeys.CONTENT, content(spec.contents(), schemas));
        return body;
    }

    private static Map<String, Object> responses(EndpointDocumentation doc, SchemaGenerator schemas) {
        Map<String, Object> responses = new LinkedHashMap<>();

        Map<String, Object> success = new LinkedHashMap<>();
        String description = doc != null && doc.successDescription() != null
            ? doc.successDescription()
            : (doc != null && doc.summary() != null ? doc.summary() : "Successful response");
        success.put(OpenApiKeys.DESCRIPTION, description);
        List<ContentSpec> successContents = doc != null && !doc.successContents().isEmpty()
            ? doc.successContents()
            : List.of(ContentSpec.emptyJson());
        success.put(OpenApiKeys.CONTENT, content(successContents, schemas));
        if (doc != null && doc.shapeable() && doc.arrayResponse()) {
            success.put(OpenApiKeys.HEADERS, paginationHeaders());
        }
        responses.put(OpenApiKeys.STATUS_OK, success);

        if (doc != null) {
            doc.extraResponses().forEach((status, spec) -> responses.put(status, response(spec, schemas)));
        }

        boolean hasDefault = doc != null && doc.extraResponses().containsKey(OpenApiKeys.STATUS_DEFAULT);
        if ((doc == null || doc.autoDefaultResponse()) && !hasDefault) {
            responses.put(OpenApiKeys.STATUS_DEFAULT, response(ResponseSpec.error("Error response"), schemas));
        }
        return responses;
    }

    private static Map<String, Object> response(ResponseSpec spec, SchemaGenerator schemas) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put(OpenApiKeys.DESCRIPTION, spec.description());
        if (!spec.contents().isEmpty()) {
            node.put(OpenApiKeys.CONTENT, content(spec.contents(), schemas));
        }
        return node;
    }

    private static Map<String, Object> getComponents(SchemaGenerator schemas) {
        Map<String, Object> components = new LinkedHashMap<>();

        Map<String, Object> allSchemas = new LinkedHashMap<>(schemas.components());
        allSchemas.put(OpenApiKeys.ERROR_SCHEMA, errorSchema());
        components.put(OpenApiKeys.SCHEMAS, allSchemas);

        Map<String, Object> parameters = new LinkedHashMap<>();
        for (GlobalParameters shaping : GlobalParameters.values()) {
            parameters.put(shaping.component(), shapingParameter(shaping));
        }
        components.put(OpenApiKeys.PARAMETERS, parameters);
        return components;
    }

    private static Map<String, Object> paginationHeaders() {
        Map<String, Object> headers = new LinkedHashMap<>();
        for (GlobalParameters.ResponseHeader header : GlobalParameters.ARRAY_RESPONSE_HEADERS) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put(OpenApiKeys.DESCRIPTION, header.description());
            node.put(OpenApiKeys.SCHEMA, Schemas.typed(header.type()));
            headers.put(header.name(), node);
        }
        return headers;
    }

    private static Map<String, Object> errorSchema() {
        Map<String, Object> schema = Schemas.object();
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("status", Schemas.formatted(OpenApiKeys.TYPE_INTEGER, OpenApiKeys.FORMAT_INT32));
        properties.put("error", Schemas.typed(OpenApiKeys.TYPE_STRING));
        schema.put(OpenApiKeys.PROPERTIES, properties);
        return schema;
    }

    private static Map<String, Object> shapingParameter(GlobalParameters shaping) {
        Map<String, Object> parameter = new LinkedHashMap<>();
        parameter.put(OpenApiKeys.NAME, shaping.getKey());
        parameter.put(OpenApiKeys.IN, OpenApiKeys.IN_QUERY);
        parameter.put(OpenApiKeys.REQUIRED, false);
        parameter.put(OpenApiKeys.DESCRIPTION, shaping.getDescription());
        parameter.put(OpenApiKeys.SCHEMA, Schemas.typed(shaping.getType()));
        return parameter;
    }

    private static Map<String, Object> getModInfo(ApiVersion version, Mod mod) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put(OpenApiKeys.TITLE, mod.getName() + " Web API");
        info.put(OpenApiKeys.VERSION, version.version());
        info.put(OpenApiKeys.DESCRIPTION, mod.getDescription());
        if (!mod.getLicense().isEmpty()) {
            info.put(OpenApiKeys.LICENSE, Map.of(OpenApiKeys.NAME, String.join(", ", mod.getLicense())));
        }
        mod.getHomepage().ifPresent(url -> info.put(OpenApiKeys.CONTACT, Map.of(OpenApiKeys.URL, url)));
        return info;
    }

    private static Map<String, Object> getExternalDocs(Mod mod) {
        if (mod == null) {
            return null;
        }
        String url = mod.getHomepage().or(mod::getSources).or(mod::getIssueTracker).orElse(null);
        return url == null ? null : Map.of(OpenApiKeys.DESCRIPTION, "Project page", OpenApiKeys.URL, url);
    }

    private static List<Object> getServers(ApiVersion version) {
        List<Object> servers = new ArrayList<>();
        for (String namespace : WebServer.NAMESPACES) {
            Map<String, Object> server = new LinkedHashMap<>();
            server.put(OpenApiKeys.URL, "/" + namespace + "/" + WebServer.API_SEGMENT + "/" + version.slug());
            server.put(OpenApiKeys.DESCRIPTION, "Base path via the '" + namespace + "' namespace.");
            servers.add(server);
        }
        return servers;
    }

    private static List<Object> getTags(Set<ApiTagRegistry.ApiTag> used) {
        List<Object> list = new ArrayList<>();
        Set<String> emitted = new LinkedHashSet<>();
        for (ApiTagRegistry.ApiTag tag : ApiTagRegistry.all()) {
            if (!used.contains(tag)) {
                continue;
            }
            Map<String, Object> node = new LinkedHashMap<>();
            node.put(OpenApiKeys.NAME, tag.getName());
            if (tag.getDescription() != null && !tag.getDescription().isBlank()) {
                node.put(OpenApiKeys.DESCRIPTION, tag.getDescription());
            }
            list.add(node);
            emitted.add(tag.getName());
        }
        for (ApiTagRegistry.ApiTag tag : used) {
            if (emitted.add(tag.getName())) {
                list.add(Map.of(OpenApiKeys.NAME, tag.getName()));
            }
        }
        return list;
    }

    private static List<String> segments(String path) {
        List<String> out = new ArrayList<>();
        for (String part : path.split("/")) {
            if (!part.isEmpty()) {
                out.add(part);
            }
        }
        return out;
    }

    private static List<String> pathParameters(String path) {
        List<String> names = new ArrayList<>();
        for (String segment : segments(path)) {
            if (segment.length() >= 2 && segment.startsWith("{") && segment.endsWith("}")) {
                names.add(segment.substring(1, segment.length() - 1));
            }
        }
        return names;
    }

    private static String operationId(String method, String path, Set<String> used) {
        StringBuilder builder = new StringBuilder(method.toLowerCase());
        for (String segment : segments(path)) {
            builder.append('_');
            if (segment.length() >= 2 && segment.startsWith("{") && segment.endsWith("}")) {
                builder.append("by_").append(sanitize(segment.substring(1, segment.length() - 1)));
            } else {
                builder.append(sanitize(segment));
            }
        }
        String base = builder.toString();
        String candidate = base;
        int counter = 2;
        while (!used.add(candidate)) {
            candidate = base + "_" + counter++;
        }
        return candidate;
    }

    private static String sanitize(String segment) {
        return segment.replaceAll("[^a-zA-Z0-9]+", "_");
    }

    private static RecordComponent[] recordComponents(Class<?> model) {
        return model.isRecord() ? model.getRecordComponents() : new RecordComponent[0];
    }

    private static Map<String, Object> content(List<ContentSpec> contents, SchemaGenerator schemas) {
        Map<String, Object> content = new LinkedHashMap<>();
        for (ContentSpec spec : contents) {
            Map<String, Object> media = new LinkedHashMap<>();
            Map<String, Object> schema = spec.schema() == null ? null : spec.schema().resolve(schemas);
            if (schema != null) {
                media.put(OpenApiKeys.SCHEMA, schema);
            }
            if (spec.example() != null) {
                media.put(OpenApiKeys.EXAMPLE, spec.example());
            }
            content.put(spec.mediaType(), media);
        }
        return content;
    }

    private static Map<String, Object> ref(String target) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put(OpenApiKeys.REF, target);
        return node;
    }

    private static EndpointDocumentation safeDoc(de.mrjulsen.crn.web.api.IEndpointHandler handler) {
        try {
            return handler.getDocumentation();
        } catch (Exception e) {
            CreateRailwaysNavigator.LOGGER.warn("Failed to read OpenAPI doc from {}", handler.getClass().getName(), e);
            return null;
        }
    }
}
