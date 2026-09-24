package de.mrjulsen.crn.web.openapi;

import java.util.LinkedHashMap;
import java.util.Map;

final class Schemas {

    private Schemas() {}

    static Map<String, Object> object() {
        return typed(OpenApiKeys.TYPE_OBJECT);
    }

    static Map<String, Object> typed(String type) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put(OpenApiKeys.TYPE, type);
        return node;
    }

    static Map<String, Object> formatted(String type, String format) {
        Map<String, Object> node = typed(type);
        node.put(OpenApiKeys.FORMAT, format);
        return node;
    }

    static Map<String, Object> array(Map<String, Object> items) {
        Map<String, Object> node = typed(OpenApiKeys.TYPE_ARRAY);
        node.put(OpenApiKeys.ITEMS, items);
        return node;
    }

    static Map<String, Object> schemaRef(String name) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put(OpenApiKeys.REF, OpenApiKeys.SCHEMA_REF_PREFIX + name);
        return node;
    }
}
