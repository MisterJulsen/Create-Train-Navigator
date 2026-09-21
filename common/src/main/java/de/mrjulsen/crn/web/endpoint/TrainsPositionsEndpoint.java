package de.mrjulsen.crn.web.endpoint;

import org.eclipse.jetty.server.Request;

import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.api.core.query.TrainPositionQuery;
import de.mrjulsen.crn.api.core.snapshot.TrainPositionSnapshot;
import de.mrjulsen.crn.web.ModWebFeatures;
import de.mrjulsen.crn.web.api.*;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

public class TrainsPositionsEndpoint implements IEndpointHandler {

    @Override
    public ApiResult handle(Request request) {
        TrainPositionQuery query = QueryBinder.bind(request, TrainPositionQuery.class);
        return ApiResult.json(RailwayBackendApi.getAllPositions(query));
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag(ModWebFeatures.TAG_TRAINS)
            .summary("List train positions")
            .description("The location of every train publicly visible and its current speed.")
            .query(TrainPositionQuery.class)
            .returnsList(TrainPositionSnapshot.class)
            .shapeable()
            .build();
    }
}
