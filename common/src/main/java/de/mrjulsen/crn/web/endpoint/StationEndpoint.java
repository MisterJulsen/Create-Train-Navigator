package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.web.api.RequestParams;

import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.api.core.snapshot.StationSnapshot;
import de.mrjulsen.crn.web.ModWebFeatures;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.NotFoundException;
import de.mrjulsen.crn.web.api.ParamType;
import org.eclipse.jetty.server.Request;
import de.mrjulsen.crn.web.api.ApiResult;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

public class StationEndpoint implements IEndpointHandler {

    @Override
    public ApiResult handle(Request request) {
        String stationName = RequestParams.path(request, "name", ParamType.STRING);
        return ApiResult.json(RailwayBackendApi.getStation(stationName).orElseThrow(() -> new NotFoundException("No station named " + stationName)));
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag(ModWebFeatures.TAG_STATIONS)
            .summary("Get a station")
            .description("Information about a station, like its tags, the lines and categories calling there, and the trains that serve it.")
            .returns(StationSnapshot.class)
            .shapeable()
            .notFound("No station with that name exists.")
            .build();
    }
}
