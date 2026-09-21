package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.web.api.RequestParams;

import de.mrjulsen.crn.web.ModWebFeatures;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import org.eclipse.jetty.server.Request;
import de.mrjulsen.crn.web.api.ApiResult;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

import java.net.HttpURLConnection;

public class PingEndpoint implements IEndpointHandler {

    @Override
    public ApiResult handle(Request request) {
        if (RequestParams.fullPath(request).endsWith("ping")) {
            return ApiResult.text("Pong!");
        } else if (RequestParams.fullPath(request).endsWith("hello")) {
            return ApiResult.text("World!");
        }
        return ApiResult.text("Hello World!");
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag(ModWebFeatures.TAG_COMMON)
            .summary("Health check")
            .description("Returns a small plain-text response to check if the API is reachable.")
            .returnsText()
            .response(HttpURLConnection.HTTP_OK, "A short plain-text reply.")
            .build();
    }
}
