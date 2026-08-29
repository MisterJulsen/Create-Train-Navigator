package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.Request;
import de.mrjulsen.crn.web.api.Response;

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
}
