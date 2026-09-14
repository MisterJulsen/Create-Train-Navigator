package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.api.core.snapshot.StationSnapshot;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.ParamType;
import de.mrjulsen.crn.web.api.Request;
import de.mrjulsen.crn.web.api.Response;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

public class StationEndpoint implements IEndpointHandler {

    @Override
    public Response handle(Request request) {
        String stationName = request.pathParameter("name", ParamType.STRING);
        return Response.json(RailwayBackendApi.getStation(stationName).orElseThrow());
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag("Stations")
            .summary("Get a station")
            .description("Information about a station, like its tags, the lines and categories calling there, and the trains that serve it.")
            .returns(StationSnapshot.class)
            .shapeable()
            .build();
    }
}
