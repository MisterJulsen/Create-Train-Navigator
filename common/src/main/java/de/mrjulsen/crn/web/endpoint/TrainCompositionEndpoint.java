package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.web.api.RequestParams;

import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.api.core.snapshot.TrainCompositionSnapshot;
import de.mrjulsen.crn.web.ModWebFeatures;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.NotFoundException;
import de.mrjulsen.crn.web.api.ParamType;
import org.eclipse.jetty.server.Request;
import de.mrjulsen.crn.web.api.ApiResult;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

import java.util.UUID;

public class TrainCompositionEndpoint implements IEndpointHandler {

    @Override
    public ApiResult handle(Request request) {
        UUID trainId = RequestParams.path(request, "id", ParamType.UUID);
        return ApiResult.json(RailwayBackendApi.getComposition(trainId).orElseThrow(() -> new NotFoundException("No train with id " + trainId)));
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag(ModWebFeatures.TAG_TRAINS)
            .summary("Get a train's composition")
            .description("Information about all carriages of a train.")
            .returns(TrainCompositionSnapshot.class)
            .shapeable()
            .notFound("No train with that id exists.")
            .build();
    }
}
