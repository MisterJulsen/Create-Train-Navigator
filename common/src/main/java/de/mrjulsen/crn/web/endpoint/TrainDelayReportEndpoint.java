package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.ParamType;
import de.mrjulsen.crn.web.api.Request;
import de.mrjulsen.crn.web.api.Response;

import java.util.UUID;

public class TrainDelayReportEndpoint implements IEndpointHandler {

    @Override
    public Response handle(Request request) {
        UUID trainId = request.pathParameter("id", ParamType.UUID);
        return Response.json(RailwayBackendApi.getDelayReport(trainId).orElseThrow());
    }
}
