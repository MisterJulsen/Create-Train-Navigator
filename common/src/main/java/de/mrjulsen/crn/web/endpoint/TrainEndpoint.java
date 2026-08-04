package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.web.api.*;

import java.util.UUID;

public class TrainEndpoint implements IEndpointHandler {

    @Override
    public Response handle(Request request) {
        UUID trainId = request.pathParameter("id", ParamType.UUID);
        return Response.json(RailwayBackendApi.getTrain(trainId).orElseThrow());
    }
}
