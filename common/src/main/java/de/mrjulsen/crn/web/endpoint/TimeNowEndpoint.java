package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.crn.web.annotation.OpenApiDescription;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.Request;
import de.mrjulsen.crn.web.api.Response;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

public class TimeNowEndpoint implements IEndpointHandler {

    private record Data(
        @OpenApiDescription("The current game time in ticks.")
        long time
    ) {}

    @Override
    public Response handle(Request request) {
        return Response.json(new Data(ModUtils.getTransformedWorldTime()));
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag("Meta")
            .summary("Current game time")
            .description("The current time on the server. Use this to Compare snapshot timestamps.")
            .returns(Data.class)
            .build();
    }
}
