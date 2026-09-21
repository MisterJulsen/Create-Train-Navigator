package de.mrjulsen.crn.web.api;

import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

import org.eclipse.jetty.util.Fields;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import de.mrjulsen.crn.api.json.JsonConvert;

public final class ResultShaper {

    private ResultShaper() {}

    public static Object shape(Fields query, Object payload, BiConsumer<String, String> headers) {
        if (payload == null) {
            return null;
        }

        FieldMask mask = FieldMask.parse(query.getValue(GlobalParameters.FIELDS.getKey()));
        List<JsonSorter.Key> sortKeys = JsonSorter.parse(query.getValue(GlobalParameters.SORT.getKey()));
        Integer limit = pageValue(query, GlobalParameters.LIMIT.getKey());
        Integer offset = pageValue(query, GlobalParameters.OFFSET.getKey());
        boolean paginate = limit != null || offset != null;

        if (mask.isEmpty() && sortKeys.isEmpty() && !paginate) {
            return payload;
        }

        JsonElement tree = JsonConvert.toJsonTree(payload);

        if (tree.isJsonArray()) {
            JsonArray array = tree.getAsJsonArray();
            if (!sortKeys.isEmpty()) {
                array = JsonSorter.sort(array, sortKeys);
            }
            if (paginate) {
                array = paginate(array, limit, offset, headers);
            }
            return projectArray(array, mask, payload);
        }
        if (!mask.isEmpty()) {
            return JsonProjector.project(tree, mask, payload.getClass());
        }
        return tree;
    }

    private static JsonElement projectArray(JsonArray array, FieldMask mask, Object payload) {
        if (mask.isEmpty()) {
            return array;
        }
        Class<?> elementType = elementClass(payload);
        JsonArray out = new JsonArray(array.size());
        for (JsonElement item : array) {
            out.add(JsonProjector.project(item, mask, elementType));
        }
        return out;
    }

    private static JsonArray paginate(JsonArray array, Integer limit, Integer offset, BiConsumer<String, String> headers) {
        int total = array.size();
        int start = Math.min(offset != null ? offset : 0, total);
        int end = limit != null ? Math.min(start + limit, total) : total;
        JsonArray page = new JsonArray(Math.max(0, end - start));
        for (int i = start; i < end; i++) {
            page.add(array.get(i));
        }
        headers.accept(HttpHeader.X_TOTAL_COUNT, Integer.toString(total));
        headers.accept(HttpHeader.X_OFFSET, Integer.toString(start));
        if (limit != null) {
            headers.accept(HttpHeader.X_LIMIT, Integer.toString(limit));
        }
        return page;
    }

    private static Class<?> elementClass(Object payload) {
        if (payload instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (item != null) {
                    return item.getClass();
                }
            }
            return null;
        }
        if (payload != null && payload.getClass().isArray()) {
            return payload.getClass().getComponentType();
        }
        return null;
    }

    private static Integer pageValue(Fields query, String name) {
        String raw = query.getValue(name);
        if (raw == null) {
            return null;
        }
        int value;
        try {
            value = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("Query parameter '" + name + "' must be an integer, got: " + raw);
        }
        if (value < 0) {
            throw new BadRequestException("Query parameter '" + name + "' must not be negative: " + value);
        }
        return value;
    }
}
