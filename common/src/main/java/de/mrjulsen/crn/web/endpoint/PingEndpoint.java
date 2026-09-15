package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.web.ModWebFeatures;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.Request;
import de.mrjulsen.crn.web.api.Response;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

import java.net.HttpURLConnection;

public class PingEndpoint implements IEndpointHandler {

    @Override
    public Response handle(Request request) {
        if (request.path().endsWith("ping")) {
            return Response.text("Pong!");
        } else if (request.path().endsWith("hello")) {
            return Response.text("World!");
        }
        return Response.text("Hello World!");
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag(ModWebFeatures.TAG_COMMON)
            .summary("Health check")
            .description("Returns a small plain-text response to check if the API is reachable.")
            .response(HttpURLConnection.HTTP_OK, "A short plain-text reply.")
            .build();
    }
}
