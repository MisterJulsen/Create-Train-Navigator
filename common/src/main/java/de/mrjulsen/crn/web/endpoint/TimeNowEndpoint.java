package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.crn.web.ModWebFeatures;
import de.mrjulsen.crn.web.annotation.OpenApiDescription;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import org.eclipse.jetty.server.Request;
import de.mrjulsen.crn.web.api.ApiResult;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

public class TimeNowEndpoint implements IEndpointHandler {

    private record Data(
        @OpenApiDescription("The current game time in ticks.")
        long time
    ) {}

    @Override
    public ApiResult handle(Request request) {
        return ApiResult.json(new Data(ModUtils.getTransformedWorldTime()));
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag(ModWebFeatures.TAG_COMMON)
            .summary("Current game time")
            .description("The current time on the server. Use this to Compare snapshot timestamps.")
            .returns(Data.class)
            .build();
    }
}
