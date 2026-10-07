package de.mrjulsen.crn.web.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ApiTagRegistry {
    private ApiTagRegistry() {}

    public static class ApiTag {
        private final String name;
        private final String description;

        private ApiTag(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ApiTag o) {
                return name.equals(o.name);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public String toString() {
            return String.format("%s[name=%s, description=%s]", getClass().getSimpleName(), name, description);
        }
    }

    private static final Map<String, ApiTag> TAGS = new LinkedHashMap<>();

    public static ApiTag register(String name, String description) {
        if (TAGS.containsKey(name)) {
            throw new IllegalStateException("A tag with name '" + name + "' has already been registered.");
        }
        ApiTag tag = new ApiTag(name, description);
        TAGS.put(name, tag);
        return tag;
    }

    public static Optional<ApiTag> get(String name) {
        return Optional.ofNullable(TAGS.get(name));
    }

    public static List<ApiTag> all() {
        return List.copyOf(TAGS.values());
    }
}
