package de.mrjulsen.crn.web.openapi;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.mrjulsen.crn.api.json.JsonConvert;
import de.mrjulsen.crn.web.annotation.OpenApiDescription;
import de.mrjulsen.crn.web.annotation.ResponseAlwaysInclude;

final class SchemaGenerator {

    private static final String MOD_PACKAGE = "de.mrjulsen";

    private final Map<String, Map<String, Object>> components = new LinkedHashMap<>();
    private final Map<Class<?>, String> assignedNames = new LinkedHashMap<>();

    Map<String, Map<String, Object>> components() {
        return components;
    }

    Map<String, Object> schema(Type type) {
        if (type instanceof ParameterizedType parameterized) {
            return parameterized(parameterized);
        }
        if (type instanceof Class<?> clazz) {
            return forClass(clazz);
        }
        return Schemas.object();
    }

    Map<String, Object> querySchema(Type type) {
        if (type instanceof ParameterizedType parameterized) {
            Class<?> raw = (Class<?>) parameterized.getRawType();
            if (Collection.class.isAssignableFrom(raw)) {
                return Schemas.array(querySchema(parameterized.getActualTypeArguments()[0]));
            }
            if (Optional.class.isAssignableFrom(raw)) {
                Map<String, Object> node = querySchema(parameterized.getActualTypeArguments()[0]);
                node.put(OpenApiKeys.NULLABLE, true);
                return node;
            }
        }
        if (type instanceof Class<?> clazz) {
            if (clazz.isArray()) {
                return Schemas.array(querySchema(clazz.getComponentType()));
            }
            if (isStringParsed(clazz)) {
                return Schemas.typed(OpenApiKeys.TYPE_STRING);
            }
        }
        return schema(type);
    }

    private static boolean isStringParsed(Class<?> type) {
        return type.getName().startsWith(MOD_PACKAGE) && !type.isEnum() && SchemaTypeRegistry.lookup(type).isEmpty();
    }

    private Map<String, Object> parameterized(ParameterizedType type) {
        Class<?> raw = (Class<?>) type.getRawType();
        Type[] args = type.getActualTypeArguments();
        if (Collection.class.isAssignableFrom(raw)) {
            return Schemas.array(schema(args[0]));
        }
        if (Map.class.isAssignableFrom(raw)) {
            Map<String, Object> node = Schemas.object();
            node.put(OpenApiKeys.ADDITIONAL_PROPERTIES, schema(args.length > 1 ? args[1] : Object.class));
            return node;
        }
        if (Optional.class.isAssignableFrom(raw)) {
            Map<String, Object> node = schema(args[0]);
            node.put(OpenApiKeys.NULLABLE, true);
            return node;
        }
        return forClass(raw);
    }

    private Map<String, Object> forClass(Class<?> type) {
        Optional<Map<String, Object>> mapped = SchemaTypeRegistry.lookup(type);
        if (mapped.isPresent()) {
            return mapped.get();
        }
        if (Number.class.isAssignableFrom(type)) {
            return Schemas.typed(OpenApiKeys.TYPE_NUMBER);
        }
        if (type.isEnum()) {
            return enumeration(type);
        }
        if (type.isArray()) {
            return Schemas.array(schema(type.getComponentType()));
        }
        if (Collection.class.isAssignableFrom(type)) {
            return Schemas.array(Schemas.object());
        }
        if (Map.class.isAssignableFrom(type)) {
            return Schemas.object();
        }
        if (type.getName().startsWith(MOD_PACKAGE)) {
            return Schemas.schemaRef(register(type));
        }
        return Schemas.object();
    }

    private String register(Class<?> type) {
        String existing = assignedNames.get(type);
        if (existing != null) {
            return existing;
        }
        String name = uniqueName(type);
        assignedNames.put(type, name);
        Map<String, Object> schema = Schemas.object();
        schema.put(OpenApiKeys.TITLE, generateDisplayName(name));
        String typeDescription = description(type.getAnnotation(OpenApiDescription.class));
        if (typeDescription != null) {
            schema.put(OpenApiKeys.DESCRIPTION, typeDescription);
        }
        Map<String, Object> properties = new LinkedHashMap<>();
        schema.put(OpenApiKeys.PROPERTIES, properties);
        components.put(name, schema);
        List<String> required = populate(type, properties);
        if (!required.isEmpty()) {
            schema.put(OpenApiKeys.REQUIRED, required);
        }
        return name;
    }

    private List<String> populate(Class<?> type, Map<String, Object> properties) {
        List<String> required = new ArrayList<>();
        if (type.isRecord()) {
            for (RecordComponent component : type.getRecordComponents()) {
                Map<String, Object> property = schema(component.getGenericType());
                applyDescription(property, component.getAnnotation(OpenApiDescription.class));
                String name = JsonConvert.toSnakeCase(component.getName());
                properties.put(name, property);
                if (component.isAnnotationPresent(ResponseAlwaysInclude.class)) {
                    required.add(name);
                }
            }
            return required;
        }
        for (Field field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers()) || field.isSynthetic()) {
                continue;
            }
            Map<String, Object> property = schema(field.getGenericType());
            applyDescription(property, field.getAnnotation(OpenApiDescription.class));
            String name = JsonConvert.toSnakeCase(field.getName());
            properties.put(name, property);
            if (field.isAnnotationPresent(ResponseAlwaysInclude.class)) {
                required.add(name);
            }
        }
        return required;
    }

    private String uniqueName(Class<?> type) {
        Deque<String> parts = new ArrayDeque<>();
        for (Class<?> current = type; current != null; current = current.getEnclosingClass()) {
            parts.addFirst(current.getSimpleName());
        }
        String base = String.join("", parts);
        String candidate = base;
        int counter = 2;
        while (components.containsKey(candidate)) {
            candidate = base + counter++;
        }
        return candidate;
    }

    private static String generateDisplayName(String name) {
        String spaced = name
            .replaceAll("([a-z0-9])([A-Z])", "$1 $2")
            .replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2");
        return spaced.trim().replaceAll("\\s+", " ");
    }

    private Map<String, Object> enumeration(Class<?> type) {
        Map<String, Object> node = Schemas.typed(OpenApiKeys.TYPE_STRING);
        List<String> values = new ArrayList<>();
        for (Object constant : type.getEnumConstants()) {
            values.add(((Enum<?>) constant).name());
        }
        node.put(OpenApiKeys.ENUM, values);
        return node;
    }

    private static void applyDescription(Map<String, Object> node, OpenApiDescription annotation) {
        if (annotation == null || node.containsKey(OpenApiKeys.REF)) {
            return;
        }
        String text = description(annotation);
        if (text != null) {
            node.put(OpenApiKeys.DESCRIPTION, text);
        }
        if (!annotation.example().isBlank()) {
            node.put(OpenApiKeys.EXAMPLE, annotation.example());
        }
    }

    private static String description(OpenApiDescription annotation) {
        return annotation == null || annotation.value().isBlank() ? null : annotation.value();
    }
}
