package de.mrjulsen.crn.web.openapi;

import de.mrjulsen.crn.web.api.ApiTagRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EndpointDocumentation {

    private final String summary;
    private final String description;
    private final List<ApiTagRegistry.ApiTag> tags;
    private final Class<?> queryModel;
    private final Class<?> requestBody;
    private final Class<?> responseType;
    private final boolean arrayResponse;
    private final boolean shapeable;
    private final boolean deprecated;
    private final Map<Integer, String> responses;
    private final List<QueryParameter> queryParams;

    public record QueryParameter(String name, String type, String description, boolean required, List<String> enumValues, String example, String defaultValue) {}

    private EndpointDocumentation(Builder builder) {
        this.summary = builder.summary;
        this.description = builder.description;
        this.tags = List.copyOf(builder.tags);
        this.queryModel = builder.queryModel;
        this.requestBody = builder.requestBody;
        this.responseType = builder.responseType;
        this.arrayResponse = builder.arrayResponse;
        this.shapeable = builder.shapeable;
        this.deprecated = builder.deprecated;
        this.responses = Collections.unmodifiableMap(new LinkedHashMap<>(builder.responses));
        this.queryParams = List.copyOf(builder.queryParams);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String summary() {
        return summary;
    }

    public String description() {
        return description;
    }

    public List<ApiTagRegistry.ApiTag> tags() {
        return tags;
    }

    public Class<?> queryModel() {
        return queryModel;
    }

    public Class<?> requestBody() {
        return requestBody;
    }

    public Class<?> responseType() {
        return responseType;
    }

    public boolean arrayResponse() {
        return arrayResponse;
    }

    public boolean shapeable() {
        return shapeable;
    }

    public boolean deprecated() {
        return deprecated;
    }

    public Map<Integer, String> responses() {
        return responses;
    }

    public List<QueryParameter> queryParams() {
        return queryParams;
    }

    public static final class Builder {

        private String summary;
        private String description;
        private final List<ApiTagRegistry.ApiTag> tags = new ArrayList<>();
        private Class<?> queryModel;
        private Class<?> requestBody;
        private Class<?> responseType;
        private boolean arrayResponse;
        private boolean shapeable;
        private boolean deprecated;
        private final Map<Integer, String> responses = new LinkedHashMap<>();
        private final List<QueryParameter> queryParams = new ArrayList<>();

        private Builder() {}

        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder tag(ApiTagRegistry.ApiTag... tags) {
            Collections.addAll(this.tags, tags);
            return this;
        }

        public Builder query(Class<?> queryModel) {
            this.queryModel = queryModel;
            return this;
        }

        public Builder body(Class<?> requestBody) {
            this.requestBody = requestBody;
            return this;
        }

        public Builder returns(Class<?> responseType) {
            this.responseType = responseType;
            this.arrayResponse = false;
            return this;
        }

        public Builder returnsList(Class<?> elementType) {
            this.responseType = elementType;
            this.arrayResponse = true;
            return this;
        }

        public Builder shapeable() {
            this.shapeable = true;
            return this;
        }

        public Builder deprecated() {
            this.deprecated = true;
            return this;
        }

        public Builder response(int status, String description) {
            this.responses.put(status, description);
            return this;
        }

        public Builder notFound(String description) {
            return response(java.net.HttpURLConnection.HTTP_NOT_FOUND, description);
        }

        public Builder badRequest(String description) {
            return response(java.net.HttpURLConnection.HTTP_BAD_REQUEST, description);
        }

        /** Documents an optional query parameter the handler reads directly, without a {@code @QueryModel}. */
        public Builder queryParam(String name, String type, String description) {
            this.queryParams.add(new QueryParameter(name, type, description, false, List.of(), null, null));
            return this;
        }

        /** As {@link #queryParam(String, String, String)}, with allowed enum values and a default. */
        public Builder queryParam(String name, String type, String description, List<String> enumValues, String defaultValue) {
            this.queryParams.add(new QueryParameter(name, type, description, false, List.copyOf(enumValues), null, defaultValue));
            return this;
        }

        public EndpointDocumentation build() {
            return new EndpointDocumentation(this);
        }
    }
}
