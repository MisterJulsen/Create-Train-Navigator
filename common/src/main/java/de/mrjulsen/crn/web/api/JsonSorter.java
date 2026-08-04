package de.mrjulsen.crn.web.api;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class JsonSorter {
    private JsonSorter() {}

    public record Key(String[] path, boolean descending) {}

    public static List<Key> parse(String spec) {
        List<Key> keys = new ArrayList<>();
        if (spec == null) {
            return keys;
        }
        for (String raw : spec.split(",")) {
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            boolean descending = trimmed.charAt(0) == '-';
            if (descending || trimmed.charAt(0) == '+') {
                trimmed = trimmed.substring(1).trim();
            }
            if (!trimmed.isEmpty()) {
                keys.add(new Key(trimmed.split("\\."), descending));
            }
        }
        return keys;
    }

    public static JsonArray sort(JsonArray array, List<Key> keys) {
        List<JsonElement> items = new ArrayList<>(array.size());
        array.forEach(items::add);
        items.sort((a, b) -> compare(a, b, keys));
        JsonArray out = new JsonArray(items.size());
        items.forEach(out::add);
        return out;
    }

    private static int compare(JsonElement a, JsonElement b, List<Key> keys) {
        for (Key key : keys) {
            JsonElement va = resolve(a, key.path());
            JsonElement vb = resolve(b, key.path());
            boolean aMissing = !sortable(va);
            boolean bMissing = !sortable(vb);
            if (aMissing && bMissing) {
                continue;
            }
            if (aMissing) {
                return 1;
            }
            if (bMissing) {
                return -1;
            }
            int result = compareValues(va.getAsJsonPrimitive(), vb.getAsJsonPrimitive());
            if (result != 0) {
                return key.descending() ? -result : result;
            }
        }
        return 0;
    }

    private static JsonElement resolve(JsonElement element, String[] path) {
        JsonElement current = element;
        for (String segment : path) {
            if (current == null || !current.isJsonObject()) {
                return null;
            }
            current = current.getAsJsonObject().get(segment);
        }
        return current;
    }

    private static boolean sortable(JsonElement element) {
        return element != null && element.isJsonPrimitive();
    }

    private static int compareValues(JsonPrimitive a, JsonPrimitive b) {
        if (a.isNumber() && b.isNumber()) {
            return Double.compare(a.getAsDouble(), b.getAsDouble());
        }
        if (a.isBoolean() && b.isBoolean()) {
            return Boolean.compare(a.getAsBoolean(), b.getAsBoolean());
        }
        return a.getAsString().compareToIgnoreCase(b.getAsString());
    }
}
