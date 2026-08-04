package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.api.core.query.TrainQuery;
import de.mrjulsen.crn.web.api.*;

public class TrainsEndpoint implements IEndpointHandler {

    @Override
    public Response handle(Request request) {
        TrainQuery query = QueryBinder.bind(request, TrainQuery.class);
        return Response.json(RailwayBackendApi.getTrains(query));
    }
}
