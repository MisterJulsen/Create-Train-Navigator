package de.mrjulsen.crn.web.openapi;

public record ContentSpec(String mediaType, SchemaSpec schema, Object example) {

    public static ContentSpec of(String mediaType, SchemaSpec schema) {
        return new ContentSpec(mediaType, schema, null);
    }

    public static ContentSpec of(String mediaType, SchemaSpec schema, Object example) {
        return new ContentSpec(mediaType, schema, example);
    }

    public static ContentSpec json(SchemaSpec schema) {
        return of(OpenApiKeys.MEDIA_JSON, schema);
    }

    public static ContentSpec json(SchemaSpec schema, Object example) {
        return of(OpenApiKeys.MEDIA_JSON, schema, example);
    }

    public static ContentSpec emptyJson() {
        return of(OpenApiKeys.MEDIA_JSON, SchemaSpec.empty());
    }

    public static ContentSpec text() {
        return of(OpenApiKeys.MEDIA_TEXT, SchemaSpec.string());
    }

    public static ContentSpec html() {
        return of(OpenApiKeys.MEDIA_HTML, SchemaSpec.string());
    }
}
