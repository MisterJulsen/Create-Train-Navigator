package de.mrjulsen.crn.web.endpoint;

import org.eclipse.jetty.server.Request;

import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.api.core.snapshot.TrainSnapshot;
import de.mrjulsen.crn.web.ModWebFeatures;
import de.mrjulsen.crn.web.api.*;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

import java.util.UUID;

public class TrainEndpoint implements IEndpointHandler {

    @Override
    public ApiResult handle(Request request) {
        UUID trainId = RequestParams.path(request, "id", ParamType.UUID);
        return ApiResult.json(RailwayBackendApi.getTrain(trainId).orElseThrow(() -> new NotFoundException("No train with id " + trainId)));
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag(ModWebFeatures.TAG_TRAINS)
            .summary("Get a train")
            .description("A train by its id.")
            .returns(TrainSnapshot.class)
            .shapeable()
            .notFound("No train with that id exists.")
            .build();
    }
}
