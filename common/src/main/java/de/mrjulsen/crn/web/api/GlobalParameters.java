package de.mrjulsen.crn.web.api;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum GlobalParameters {
    FIELDS("fields", "string", false, "Comma-separated list of fields that should be included in the response. Omitted fields are dropped. A nested field is written as 'parent.child'. When this parameter is set, only the listed fields plus the ones the server always includes are returned, so a field otherwise marked as required in the schema may be missing."),
    SORT("sort", "string", true, "Comma-separated list of fields by which the array response should be sorted. Add the prefix '-' for descending order."),
    LIMIT("limit", "integer", true, "Maximum number of items to return from an array response."),
    OFFSET("offset", "integer", true, "Amount of items to skip from the start of an array response.");

    public record ResponseHeader(String name, String type, String description) {}

    public static final List<ResponseHeader> ARRAY_RESPONSE_HEADERS = List.of(
        new ResponseHeader(HttpHeader.X_TOTAL_COUNT, "integer", "Total number of matching items."),
        new ResponseHeader(HttpHeader.X_OFFSET, "integer", "Index of the first returned item."),
        new ResponseHeader(HttpHeader.X_LIMIT, "integer", "The limit that was applied (only when a limit was given).")
    );

    private final String key;
    private final String type;
    private final boolean arrayOnly;
    private final String description;

    GlobalParameters(String key, String type, boolean arrayOnly, String description) {
        this.key = key;
        this.type = type;
        this.arrayOnly = arrayOnly;
        this.description = description;
    }

    public String getKey() {
        return key;
    }

    public String getType() {
        return type;
    }

    public boolean isForArrayOnly() {
        return arrayOnly;
    }

    public String getDescription() {
        return description;
    }

    public String component() {
        return Character.toUpperCase(key.charAt(0)) + key.substring(1);
    }

    public static Optional<GlobalParameters> fromKey(String key) {
        return Arrays.stream(GlobalParameters.values()).filter(x -> x.key.equals(key)).findFirst();
    }
}
