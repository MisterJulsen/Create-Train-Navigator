package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.Request;
import de.mrjulsen.crn.web.api.Response;

public class TrainManagerStatsEndpoint implements IEndpointHandler {

    private record Data(boolean active, int trackedTrainsCount) {}

    @Override
    public Response handle(Request request) {
        return Response.json(new Data(
                RailwayBackendApi.isActive(),
                RailwayBackendApi.getTrackedTrainCount()
        ));
    }
}