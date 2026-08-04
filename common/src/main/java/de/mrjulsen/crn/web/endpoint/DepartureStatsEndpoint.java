package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.ParamType;
import de.mrjulsen.crn.web.api.Request;
import de.mrjulsen.crn.web.api.Response;

public class DepartureStatsEndpoint implements IEndpointHandler {

    @Override
    public Response handle(Request request) {
        String stationName = request.pathParameter("station", ParamType.STRING);
        return Response.json(RailwayBackendApi.getDepartureStats(stationName));
    }
}
