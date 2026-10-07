package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.web.api.RequestParams;

import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.api.core.snapshot.JourneySnapshot;
import de.mrjulsen.crn.api.core.snapshot.SectionSnapshot;
import de.mrjulsen.crn.web.ModWebFeatures;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.NotFoundException;
import de.mrjulsen.crn.web.api.ParamType;
import org.eclipse.jetty.server.Request;
import de.mrjulsen.crn.web.api.ApiResult;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

import java.util.UUID;

public class TrainSectionEndpoint implements IEndpointHandler {

    @Override
    public ApiResult handle(Request request) {
        UUID trainId = RequestParams.path(request, "id", ParamType.UUID);
        return ApiResult.json(RailwayBackendApi.getJourney(trainId).flatMap(JourneySnapshot::currentSection).orElseThrow(() -> new NotFoundException("No active section for train " + trainId)));
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag(ModWebFeatures.TAG_TRAINS)
            .summary("Get a train's current section")
            .description("The current schedule section of a train.")
            .returns(SectionSnapshot.class)
            .shapeable()
            .notFound("No train with that id exists, or it has no active section.")
            .build();
    }
}
