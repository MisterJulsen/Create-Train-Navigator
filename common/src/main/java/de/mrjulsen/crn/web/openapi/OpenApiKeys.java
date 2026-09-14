package de.mrjulsen.crn.web.openapi;

public final class OpenApiKeys {

    private OpenApiKeys() {}

    public static final String OPENAPI = "openapi";
    public static final String INFO = "info";
    public static final String SERVERS = "servers";
    public static final String TAGS = "tags";
    public static final String PATHS = "paths";
    public static final String COMPONENTS = "components";
    public static final String EXTERNAL_DOCS = "externalDocs";

    public static final String TITLE = "title";
    public static final String VERSION = "version";
    public static final String LICENSE = "license";
    public static final String CONTACT = "contact";
    public static final String URL = "url";

    public static final String OPERATION_ID = "operationId";
    public static final String SUMMARY = "summary";
    public static final String DEPRECATED = "deprecated";
    public static final String REQUEST_BODY = "requestBody";
    public static final String RESPONSES = "responses";
    public static final String PARAMETERS = "parameters";
    public static final String CONTENT = "content";
    public static final String HEADERS = "headers";

    public static final String NAME = "name";
    public static final String IN = "in";
    public static final String REQUIRED = "required";
    public static final String SCHEMA = "schema";

    public static final String TYPE = "type";
    public static final String FORMAT = "format";
    public static final String PROPERTIES = "properties";
    public static final String ITEMS = "items";
    public static final String ADDITIONAL_PROPERTIES = "additionalProperties";
    public static final String ENUM = "enum";
    public static final String REF = "$ref";
    public static final String DESCRIPTION = "description";
    public static final String EXAMPLE = "example";
    public static final String DEFAULT = "default";
    public static final String NULLABLE = "nullable";

    public static final String SCHEMAS = "schemas";

    public static final String TYPE_OBJECT = "object";
    public static final String TYPE_ARRAY = "array";
    public static final String TYPE_STRING = "string";
    public static final String TYPE_INTEGER = "integer";
    public static final String TYPE_NUMBER = "number";
    public static final String TYPE_BOOLEAN = "boolean";

    public static final String FORMAT_INT32 = "int32";
    public static final String FORMAT_INT64 = "int64";
    public static final String FORMAT_FLOAT = "float";
    public static final String FORMAT_DOUBLE = "double";
    public static final String FORMAT_UUID = "uuid";

    public static final String IN_QUERY = "query";
    public static final String IN_PATH = "path";

    public static final String MEDIA_JSON = "application/json";
    public static final String SCHEMA_REF_PREFIX = "#/components/schemas/";
    public static final String PARAMETER_REF_PREFIX = "#/components/parameters/";
    public static final String ERROR_SCHEMA = "Error";

    public static final String STATUS_OK = "200";
    public static final String STATUS_DEFAULT = "default";
}
