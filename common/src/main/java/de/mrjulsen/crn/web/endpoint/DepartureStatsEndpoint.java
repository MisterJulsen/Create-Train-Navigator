package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.core.history.DepartureStats;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.ParamType;
import de.mrjulsen.crn.web.api.Request;
import de.mrjulsen.crn.web.api.Response;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

public class DepartureStatsEndpoint implements IEndpointHandler {

    @Override
    public Response handle(Request request) {
        String stationName = request.pathParameter("station", ParamType.STRING);
        return Response.json(RailwayBackendApi.getDepartureStats(stationName));
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag("Departures")
            .summary("Station departure statistics")
            .description("Summary about the departures recorded at a specific station (e.g. punctually).")
            .returns(DepartureStats.class)
            .shapeable()
            .build();
    }
}
