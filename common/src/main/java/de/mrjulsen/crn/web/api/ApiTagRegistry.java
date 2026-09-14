package de.mrjulsen.crn.web.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ApiTagRegistry {
    private ApiTagRegistry() {}

    public record Tag(String name, String description) {}

    private static final Map<String, Tag> TAGS = new LinkedHashMap<>();

    public static void register(String name, String description) {
        TAGS.put(name, new Tag(name, description));
    }

    public static Optional<Tag> get(String name) {
        return Optional.ofNullable(TAGS.get(name));
    }

    public static List<Tag> all() {
        return List.copyOf(TAGS.values());
    }
}
