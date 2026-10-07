package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.web.ModWebFeatures;
import de.mrjulsen.crn.web.api.ApiVersion;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import org.eclipse.jetty.server.Request;
import de.mrjulsen.crn.web.api.ApiResult;
import de.mrjulsen.crn.web.openapi.ContentSpec;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;
import de.mrjulsen.crn.web.openapi.OpenApiGenerator;
import de.mrjulsen.crn.web.openapi.SchemaSpec;

public class OpenApiEndpoint implements IEndpointHandler {

    @Override
    public ApiResult handle(Request request) {
        return ApiResult.json(OpenApiGenerator.generate(ApiVersion.latest()));
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag(ModWebFeatures.TAG_COMMON)
            .summary("OpenAPI 3.1 specs")
            .description("The OpenAPI document describing every endpoint of this API.")
            .returns(ContentSpec.json(SchemaSpec.object()))
            .build();
    }
}
