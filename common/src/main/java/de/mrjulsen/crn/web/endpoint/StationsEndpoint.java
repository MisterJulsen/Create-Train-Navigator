package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.api.core.query.StationQuery;
import de.mrjulsen.crn.api.core.snapshot.StationSnapshot;
import de.mrjulsen.crn.web.ModWebFeatures;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.QueryBinder;
import org.eclipse.jetty.server.Request;
import de.mrjulsen.crn.web.api.ApiResult;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

public class StationsEndpoint implements IEndpointHandler {

    @Override
    public ApiResult handle(Request request) {
        StationQuery query = QueryBinder.bind(request, StationQuery.class);
        return ApiResult.json(RailwayBackendApi.getAllStations(query));
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag(ModWebFeatures.TAG_STATIONS)
            .summary("List stations")
            .description("Every station the backend knows about, ordered by name.")
            .query(StationQuery.class)
            .returnsList(StationSnapshot.class)
            .shapeable()
            .build();
    }
}
