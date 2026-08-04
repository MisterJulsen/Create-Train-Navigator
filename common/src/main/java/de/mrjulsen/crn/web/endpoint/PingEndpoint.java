package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.Request;
import de.mrjulsen.crn.web.api.Response;

public class PingEndpoint implements IEndpointHandler {

    @Override
    public Response handle(Request request) {
        return Response.text("Pong!");
    }
}
