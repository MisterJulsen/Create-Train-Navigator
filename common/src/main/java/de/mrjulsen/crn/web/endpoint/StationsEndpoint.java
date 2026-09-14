package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.api.core.query.StationQuery;
import de.mrjulsen.crn.api.core.snapshot.StationSnapshot;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.QueryBinder;
import de.mrjulsen.crn.web.api.Request;
import de.mrjulsen.crn.web.api.Response;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

public class StationsEndpoint implements IEndpointHandler {

    @Override
    public Response handle(Request request) {
        StationQuery query = QueryBinder.bind(request, StationQuery.class);
        return Response.json(RailwayBackendApi.getAllStations(query));
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag("Stations")
            .summary("List stations")
            .description("Every station the backend knows about, ordered by name.")
            .query(StationQuery.class)
            .returnsList(StationSnapshot.class)
            .shapeable()
            .build();
    }
}
