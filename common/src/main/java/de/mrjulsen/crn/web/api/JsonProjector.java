package de.mrjulsen.crn.web.api;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.mrjulsen.crn.api.json.JsonConvert;
import de.mrjulsen.crn.web.annotation.ResponseAlwaysInclude;

public final class JsonProjector {
    private JsonProjector() {}

    public static JsonElement project(JsonElement element, FieldMask mask, Type type) {
        if (mask.selectsWholeSubtree()) {
            return element;
        }
        if (element == null || element.isJsonNull()) {
            return element;
        }
        if (element.isJsonArray()) {
            Type elementType = elementType(type);
            JsonArray out = new JsonArray();
            for (JsonElement item : element.getAsJsonArray()) {
                out.add(project(item, mask, elementType));
            }
            return out;
        }
        if (element.isJsonObject()) {
            return projectObject(element.getAsJsonObject(), mask, type);
        }
        return element;
    }

    private static JsonObject projectObject(JsonObject source, FieldMask mask, Type type) {
        Class<?> raw = rawClass(type);
        Map<String, Component> shape = raw == null ? Map.of() : shapeOf(raw);
        JsonObject out = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
            String key = entry.getKey();
            boolean selected = mask.hasChild(key);
            Component component = shape.get(key);
            boolean forced = component != null && component.alwaysInclude();
            if (!selected && !forced) {
                continue;
            }
            FieldMask childMask = selected ? mask.child(key) : FieldMask.WHOLE;
            Type childType = component != null ? component.type() : null;
            out.add(key, project(entry.getValue(), childMask, childType));
        }
        return out;
    }

    private static Type elementType(Type type) {
        if (type instanceof ParameterizedType parameterized
            && parameterized.getRawType() instanceof Class<?> raw
            && Collection.class.isAssignableFrom(raw)) {
            Type[] arguments = parameterized.getActualTypeArguments();
            if (arguments.length == 1) {
                return arguments[0];
            }
        }
        if (type instanceof Class<?> clazz && clazz.isArray()) {
            return clazz.getComponentType();
        }
        return null;
    }

    private static Class<?> rawClass(Type type) {
        if (type instanceof Class<?> clazz) {
            return clazz;
        }
        if (type instanceof ParameterizedType parameterized && parameterized.getRawType() instanceof Class<?> raw) {
            return raw;
        }
        return null;
    }

    private record Component(Type type, boolean alwaysInclude) {}

    private static final Map<Class<?>, Map<String, Component>> SHAPES = new ConcurrentHashMap<>();

    private static Map<String, Component> shapeOf(Class<?> type) {
        return SHAPES.computeIfAbsent(type, JsonProjector::computeShape);
    }

    private static Map<String, Component> computeShape(Class<?> type) {
        if (!type.isRecord()) {
            return Map.of();
        }
        Map<String, Component> shape = new LinkedHashMap<>();
        for (RecordComponent component : type.getRecordComponents()) {
            String name = JsonConvert.toSnakeCase(component.getName());
            boolean always = component.isAnnotationPresent(ResponseAlwaysInclude.class);
            shape.put(name, new Component(component.getGenericType(), always));
        }
        return shape;
    }
}
