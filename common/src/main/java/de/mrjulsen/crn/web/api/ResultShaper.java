package de.mrjulsen.crn.web.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import de.mrjulsen.crn.api.json.JsonConvert;

public final class ResultShaper {

    public static final String FIELDS = "fields";
    public static final String SORT = "sort";
    public static final String LIMIT = "limit";
    public static final String OFFSET = "offset";

    private ResultShaper() {}

    public static void apply(Request request, Response response) {
        if (!response.isJson() || response.status() < 200 || response.status() >= 300) {
            return;
        }

        FieldMask mask = FieldMask.parse(request.query(FIELDS).orElse(null));
        List<JsonSorter.Key> sortKeys = JsonSorter.parse(request.query(SORT).orElse(null));
        Integer limit = pageValue(request, LIMIT);
        Integer offset = pageValue(request, OFFSET);
        boolean paginate = limit != null || offset != null;

        if (mask.isEmpty() && sortKeys.isEmpty() && !paginate) {
            return;
        }

        Object payload = response.jsonPayload();
        JsonElement tree = JsonConvert.toJsonTree(payload);

        if (tree.isJsonArray()) {
            JsonArray array = tree.getAsJsonArray();
            if (!sortKeys.isEmpty()) {
                array = JsonSorter.sort(array, sortKeys);
            }
            if (paginate) {
                array = paginate(array, limit, offset, response);
            }
            tree = projectArray(array, mask, payload);
        } else if (!mask.isEmpty()) {
            tree = JsonProjector.project(tree, mask, payload.getClass());
        }

        response.jsonPayload(tree);
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

    private static JsonArray paginate(JsonArray array, Integer limit, Integer offset, Response response) {
        int total = array.size();
        int start = Math.min(offset != null ? offset : 0, total);
        int end = limit != null ? Math.min(start + limit, total) : total;
        JsonArray page = new JsonArray(Math.max(0, end - start));
        for (int i = start; i < end; i++) {
            page.add(array.get(i));
        }
        response.header(HttpHeader.X_TOTAL_COUNT, Integer.toString(total));
        response.header(HttpHeader.X_OFFSET, Integer.toString(start));
        if (limit != null) {
            response.header(HttpHeader.X_LIMIT, Integer.toString(limit));
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

    private static Integer pageValue(Request request, String name) {
        Optional<Integer> value = request.query(name, ParamType.INT);
        if (value.isPresent() && value.get() < 0) {
            throw new BadRequestException("Query parameter '" + name + "' must not be negative, got: " + value.get());
        }
        return value.orElse(null);
    }
}
