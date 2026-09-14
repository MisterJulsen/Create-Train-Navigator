package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.api.core.query.TrainQuery;
import de.mrjulsen.crn.api.core.snapshot.TrainSnapshot;
import de.mrjulsen.crn.web.api.*;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

public class TrainsEndpoint implements IEndpointHandler {

    @Override
    public Response handle(Request request) {
        TrainQuery query = QueryBinder.bind(request, TrainQuery.class);
        return Response.json(RailwayBackendApi.getTrains(query));
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag("Trains")
            .summary("List trains")
            .description("Every train that is shown publicly.")
            .query(TrainQuery.class)
            .returnsList(TrainSnapshot.class)
            .shapeable()
            .build();
    }
}
