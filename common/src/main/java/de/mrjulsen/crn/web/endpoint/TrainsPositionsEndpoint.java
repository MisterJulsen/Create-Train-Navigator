package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.api.core.query.TrainPositionQuery;
import de.mrjulsen.crn.web.api.*;

public class TrainsPositionsEndpoint implements IEndpointHandler {

    @Override
    public Response handle(Request request) {
        TrainPositionQuery query = QueryBinder.bind(request, TrainPositionQuery.class);
        return Response.json(RailwayBackendApi.getAllPositions(query));
    }
}
