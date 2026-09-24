package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.web.api.RequestParams;

import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.api.core.snapshot.JourneySnapshot;
import de.mrjulsen.crn.web.ModWebFeatures;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.NotFoundException;
import de.mrjulsen.crn.web.api.ParamType;
import org.eclipse.jetty.server.Request;
import de.mrjulsen.crn.web.api.ApiResult;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

import java.util.UUID;

public class TrainJourneyEndpoint implements IEndpointHandler {

    @Override
    public ApiResult handle(Request request) {
        UUID trainId = RequestParams.path(request, "id", ParamType.UUID);
        return ApiResult.json(RequestParams.query(request, "in_cycles", ParamType.INT).map(
                x -> RailwayBackendApi.getJourneyIn(trainId, x).orElseThrow(() -> new NotFoundException("No train with id " + trainId))
                ).orElseGet(() -> RailwayBackendApi.getJourney(trainId).orElseThrow(() -> new NotFoundException("No train with id " + trainId))));
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag(ModWebFeatures.TAG_TRAINS)
            .summary("Get a train's journey")
            .description("The full run of one train.")
            .queryParam("in_cycles", "integer", "The number of cycles for which the train should be simulated to the future.")
            .returns(JourneySnapshot.class)
            .shapeable()
            .notFound("No train with that id exists.")
            .build();
    }
}
