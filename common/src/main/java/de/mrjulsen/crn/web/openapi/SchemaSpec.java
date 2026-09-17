package de.mrjulsen.crn.web.openapi;

import java.util.LinkedHashMap;
import java.util.Map;

public final class SchemaSpec {

    private enum Kind {
        OBJECT,
        ARRAY,
        REF,
        PRIMITIVE,
        RAW,
        EMPTY
    }

    private final Kind kind;
    private final Class<?> type;
    private final String refName;
    private final String primitiveType;
    private final String primitiveFormat;
    private final Map<String, Object> raw;

    private SchemaSpec(Kind kind, Class<?> type, String refName, String primitiveType, String primitiveFormat, Map<String, Object> raw) {
        this.kind = kind;
        this.type = type;
        this.refName = refName;
        this.primitiveType = primitiveType;
        this.primitiveFormat = primitiveFormat;
        this.raw = raw;
    }

    public static SchemaSpec of(Class<?> type) {
        return new SchemaSpec(Kind.OBJECT, type, null, null, null, null);
    }

    public static SchemaSpec list(Class<?> elementType) {
        return new SchemaSpec(Kind.ARRAY, elementType, null, null, null, null);
    }

    public static SchemaSpec ref(String componentName) {
        return new SchemaSpec(Kind.REF, null, componentName, null, null, null);
    }

    public static SchemaSpec primitive(String type, String format) {
        return new SchemaSpec(Kind.PRIMITIVE, null, null, type, format, null);
    }

    public static SchemaSpec string() {
        return primitive(OpenApiKeys.TYPE_STRING, null);
    }

    public static SchemaSpec object() {
        return raw(Schemas.object());
    }

    public static SchemaSpec raw(Map<String, Object> schema) {
        return new SchemaSpec(Kind.RAW, null, null, null, null, schema);
    }

    public static SchemaSpec empty() {
        return new SchemaSpec(Kind.EMPTY, null, null, null, null, null);
    }

    Map<String, Object> resolve(SchemaGenerator schemas) {
        return switch (kind) {
            case OBJECT -> schemas.schema(type);
            case ARRAY -> Schemas.array(schemas.schema(type));
            case REF -> Schemas.schemaRef(refName);
            case PRIMITIVE -> primitiveFormat == null ? Schemas.typed(primitiveType) : Schemas.formatted(primitiveType, primitiveFormat);
            case RAW -> new LinkedHashMap<>(raw);
            case EMPTY -> null;
        };
    }
}
