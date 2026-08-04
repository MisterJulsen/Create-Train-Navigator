package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.ParamType;
import de.mrjulsen.crn.web.api.Request;
import de.mrjulsen.crn.web.api.Response;

import java.util.UUID;

public class TrainJourneyEndpoint implements IEndpointHandler {

    @Override
    public Response handle(Request request) {
        UUID trainId = request.pathParameter("id", ParamType.UUID);
        return Response.json(request.query("in_cycles", ParamType.INT).map(
                x -> RailwayBackendApi.getJourneyIn(trainId, x).orElseThrow()
                ).orElse(RailwayBackendApi.getJourney(trainId).orElseThrow()));
    }
}
