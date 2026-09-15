package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.api.core.query.LineQuery;
import de.mrjulsen.crn.api.core.snapshot.LineSnapshot;
import de.mrjulsen.crn.web.ModWebFeatures;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.QueryBinder;
import de.mrjulsen.crn.web.api.Request;
import de.mrjulsen.crn.web.api.Response;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

public class LinesEndpoint implements IEndpointHandler {

    @Override
    public Response handle(Request request) {
        LineQuery query = QueryBinder.bind(request, LineQuery.class);
        return Response.json(RailwayBackendApi.getAllLines(query));
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag(ModWebFeatures.TAG_LINES_AND_CATEGORIES)
            .summary("List lines")
            .description("Every line with the trains using it.")
            .query(LineQuery.class)
            .returnsList(LineSnapshot.class)
            .shapeable()
            .build();
    }
}
