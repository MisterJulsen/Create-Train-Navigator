package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.api.core.query.LineQuery;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.QueryBinder;
import de.mrjulsen.crn.web.api.Request;
import de.mrjulsen.crn.web.api.Response;

public class LinesEndpoint implements IEndpointHandler {

    @Override
    public Response handle(Request request) {
        LineQuery query = QueryBinder.bind(request, LineQuery.class);
        return Response.json(RailwayBackendApi.getAllLines(query));
    }
}
