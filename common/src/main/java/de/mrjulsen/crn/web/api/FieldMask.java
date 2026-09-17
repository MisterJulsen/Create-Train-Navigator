package de.mrjulsen.crn.web.api;

import java.util.LinkedHashMap;
import java.util.Map;

public final class FieldMask {

    static final FieldMask WHOLE = whole();

    private final Map<String, FieldMask> children = new LinkedHashMap<>();
    private boolean selectsWholeSubtree;

    private FieldMask() {}

    private static FieldMask whole() {
        FieldMask node = new FieldMask();
        node.selectsWholeSubtree = true;
        return node;
    }

    public static FieldMask parse(String spec) {
        FieldMask root = new FieldMask();
        if (spec == null) {
            return root;
        }
        for (String path : spec.split(",")) {
            String trimmed = path.trim();
            if (!trimmed.isEmpty()) {
                root.add(trimmed.split("\\."), 0);
            }
        }
        return root;
    }

    private void add(String[] segments, int index) {
        String key = segments[index].trim();
        if (key.isEmpty()) {
            return;
        }
        FieldMask child = children.computeIfAbsent(key, k -> new FieldMask());
        if (index == segments.length - 1) {
            child.selectsWholeSubtree = true;
        } else {
            child.add(segments, index + 1);
        }
    }

    public boolean isEmpty() {
        return !selectsWholeSubtree && children.isEmpty();
    }

    boolean selectsWholeSubtree() {
        return selectsWholeSubtree;
    }

    boolean hasChild(String key) {
        return children.containsKey(key);
    }

    FieldMask child(String key) {
        return children.get(key);
    }
}
