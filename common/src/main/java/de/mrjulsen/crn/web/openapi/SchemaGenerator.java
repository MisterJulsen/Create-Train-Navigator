package de.mrjulsen.crn.web.openapi;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.mrjulsen.crn.api.json.JsonConvert;
import de.mrjulsen.crn.web.annotation.OpenApiDescription;

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
        String typeDescription = description(type.getAnnotation(OpenApiDescription.class));
        if (typeDescription != null) {
            schema.put(OpenApiKeys.DESCRIPTION, typeDescription);
        }
        Map<String, Object> properties = new LinkedHashMap<>();
        schema.put(OpenApiKeys.PROPERTIES, properties);
        components.put(name, schema);
        populate(type, properties);
        return name;
    }

    private void populate(Class<?> type, Map<String, Object> properties) {
        if (type.isRecord()) {
            for (RecordComponent component : type.getRecordComponents()) {
                Map<String, Object> property = schema(component.getGenericType());
                applyDescription(property, component.getAnnotation(OpenApiDescription.class));
                properties.put(JsonConvert.toSnakeCase(component.getName()), property);
            }
            return;
        }
        for (Field field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers()) || field.isSynthetic()) {
                continue;
            }
            Map<String, Object> property = schema(field.getGenericType());
            applyDescription(property, field.getAnnotation(OpenApiDescription.class));
            properties.put(JsonConvert.toSnakeCase(field.getName()), property);
        }
    }

    private String uniqueName(Class<?> type) {
        String base = type.getSimpleName();
        if (type.getEnclosingClass() != null) {
            base = type.getEnclosingClass().getSimpleName() + base;
        }
        String candidate = base;
        int counter = 2;
        while (components.containsKey(candidate)) {
            candidate = base + counter++;
        }
        return candidate;
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
