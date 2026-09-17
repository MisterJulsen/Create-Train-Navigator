package de.mrjulsen.crn.web.openapi;

import de.mrjulsen.crn.web.api.ApiTagRegistry;

import java.net.HttpURLConnection;
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
    private final RequestBodySpec requestBody;
    private final boolean arrayResponse;
    private final boolean shapeable;
    private final boolean deprecated;
    private final List<ContentSpec> successContents;
    private final String successDescription;
    private final Map<String, ResponseSpec> extraResponses;
    private final boolean autoDefaultResponse;
    private final List<QueryParameter> queryParams;

    public record QueryParameter(String name, String type, String description, boolean required, List<String> enumValues, String example, String defaultValue) {}

    private EndpointDocumentation(Builder builder) {
        this.summary = builder.summary;
        this.description = builder.description;
        this.tags = List.copyOf(builder.tags);
        this.queryModel = builder.queryModel;
        this.requestBody = builder.requestBody;
        this.arrayResponse = builder.arrayResponse;
        this.shapeable = builder.shapeable;
        this.deprecated = builder.deprecated;
        this.successContents = List.copyOf(builder.successContents);
        this.successDescription = builder.successDescription;
        this.extraResponses = Collections.unmodifiableMap(new LinkedHashMap<>(builder.extraResponses));
        this.autoDefaultResponse = builder.autoDefaultResponse;
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

    public RequestBodySpec requestBody() {
        return requestBody;
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

    public List<ContentSpec> successContents() {
        return successContents;
    }

    public String successDescription() {
        return successDescription;
    }

    public Map<String, ResponseSpec> extraResponses() {
        return extraResponses;
    }

    public boolean autoDefaultResponse() {
        return autoDefaultResponse;
    }

    public List<QueryParameter> queryParams() {
        return queryParams;
    }

    public static final class Builder {

        private String summary;
        private String description;
        private final List<ApiTagRegistry.ApiTag> tags = new ArrayList<>();
        private Class<?> queryModel;
        private RequestBodySpec requestBody;
        private boolean arrayResponse;
        private boolean shapeable;
        private boolean deprecated;
        private List<ContentSpec> successContents = List.of();
        private String successDescription;
        private final Map<String, ResponseSpec> extraResponses = new LinkedHashMap<>();
        private boolean autoDefaultResponse = true;
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

        public Builder returns(Class<?> responseType) {
            this.successContents = List.of(ContentSpec.json(SchemaSpec.of(responseType)));
            this.arrayResponse = false;
            return this;
        }

        public Builder returnsList(Class<?> elementType) {
            this.successContents = List.of(ContentSpec.json(SchemaSpec.list(elementType)));
            this.arrayResponse = true;
            return this;
        }

        public Builder returnsText() {
            this.successContents = List.of(ContentSpec.text());
            this.arrayResponse = false;
            return this;
        }

        public Builder returnsHtml() {
            this.successContents = List.of(ContentSpec.html());
            this.arrayResponse = false;
            return this;
        }

        public Builder returns(ContentSpec... contents) {
            this.successContents = List.of(contents);
            this.arrayResponse = false;
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
            if (status == HttpURLConnection.HTTP_OK) {
                this.successDescription = description;
                return this;
            }
            return response(status, status >= 400 ? ResponseSpec.error(description) : ResponseSpec.description(description));
        }

        public Builder response(int status, ResponseSpec response) {
            if (status == HttpURLConnection.HTTP_OK) {
                this.successContents = response.contents();
                if (response.description() != null) {
                    this.successDescription = response.description();
                }
                return this;
            }
            this.extraResponses.put(Integer.toString(status), response);
            return this;
        }

        public Builder notFound(String description) {
            return response(HttpURLConnection.HTTP_NOT_FOUND, ResponseSpec.error(description));
        }

        public Builder badRequest(String description) {
            return response(HttpURLConnection.HTTP_BAD_REQUEST, ResponseSpec.error(description));
        }

        public Builder defaultResponse(ResponseSpec response) {
            this.extraResponses.put(OpenApiKeys.STATUS_DEFAULT, response);
            this.autoDefaultResponse = false;
            return this;
        }

        public Builder noDefaultResponse() {
            this.autoDefaultResponse = false;
            return this;
        }

        public Builder body(Class<?> requestBody) {
            this.requestBody = RequestBodySpec.json(requestBody);
            return this;
        }

        public Builder body(RequestBodySpec requestBody) {
            this.requestBody = requestBody;
            return this;
        }

        public Builder queryParam(String name, String type, String description) {
            this.queryParams.add(new QueryParameter(name, type, description, false, List.of(), null, null));
            return this;
        }

        public Builder queryParam(String name, String type, String description, List<String> enumValues, String defaultValue) {
            this.queryParams.add(new QueryParameter(name, type, description, false, List.copyOf(enumValues), null, defaultValue));
            return this;
        }

        public EndpointDocumentation build() {
            return new EndpointDocumentation(this);
        }
    }
}
