package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.web.ModWebFeatures;
import de.mrjulsen.crn.web.annotation.OpenApiDescription;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.Request;
import de.mrjulsen.crn.web.api.Response;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

public class TrainManagerStatsEndpoint implements IEndpointHandler {

    private record Data(
        @OpenApiDescription("Whether a server is running and the backend is ready.")
        boolean active,
        @OpenApiDescription("How many trains the backend is tracking, including those hidden from public display.")
        int trackedTrainsCount
    ) {}

    @Override
    public Response handle(Request request) {
        return Response.json(new Data(
                RailwayBackendApi.isActive(),
                RailwayBackendApi.getTrackedTrainCount()
        ));
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag(ModWebFeatures.TAG_COMMON)
            .summary("Backend status")
            .description("Whether the train data backend is active and how many trains are being tracked.")
            .returns(Data.class)
            .build();
    }
}