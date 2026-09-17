package de.mrjulsen.crn.web.openapi;

import java.util.List;

public record ResponseSpec(String description, List<ContentSpec> contents) {

    public ResponseSpec {
        contents = List.copyOf(contents);
    }

    public static ResponseSpec of(String description, ContentSpec... contents) {
        return new ResponseSpec(description, List.of(contents));
    }

    public static ResponseSpec description(String description) {
        return new ResponseSpec(description, List.of());
    }

    public static ResponseSpec error(String description) {
        return of(description, ContentSpec.json(SchemaSpec.ref(OpenApiKeys.ERROR_SCHEMA)));
    }
}
