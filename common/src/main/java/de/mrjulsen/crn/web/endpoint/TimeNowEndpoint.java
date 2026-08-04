package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.Request;
import de.mrjulsen.crn.web.api.Response;

public class TimeNowEndpoint implements IEndpointHandler {

    private record Data(long time) {}

    @Override
    public Response handle(Request request) {
        return Response.json(new Data(ModUtils.getTransformedWorldTime()));
    }
}
