package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.web.api.RequestParams;

import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.api.core.snapshot.TrainPositionSnapshot;
import de.mrjulsen.crn.web.ModWebFeatures;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.NotFoundException;
import de.mrjulsen.crn.web.api.ParamType;
import org.eclipse.jetty.server.Request;
import de.mrjulsen.crn.web.api.ApiResult;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

import java.util.UUID;

public class TrainPositionEndpoint implements IEndpointHandler {

    @Override
    public ApiResult handle(Request request) {
        UUID trainId = RequestParams.path(request, "id", ParamType.UUID);
        return ApiResult.json(RailwayBackendApi.getPosition(trainId).orElseThrow(() -> new NotFoundException("No train with id " + trainId)));
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag(ModWebFeatures.TAG_TRAINS)
            .summary("Get a train's position")
            .description("The location of one train with its current speed.")
            .returns(TrainPositionSnapshot.class)
            .shapeable()
            .notFound("No train with that id exists.")
            .build();
    }
}
