package de.mrjulsen.crn.web.endpoint;

import de.mrjulsen.crn.web.api.RequestParams;

import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.api.core.snapshot.LineSnapshot;
import de.mrjulsen.crn.web.ModWebFeatures;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.NotFoundException;
import de.mrjulsen.crn.web.api.ParamType;
import org.eclipse.jetty.server.Request;
import de.mrjulsen.crn.web.api.ApiResult;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

import java.util.UUID;

public class LineEndpoint implements IEndpointHandler {

    @Override
    public ApiResult handle(Request request) {
        UUID id = RequestParams.path(request, "id", ParamType.UUID);
        return ApiResult.json(RailwayBackendApi.getLine(id).orElseThrow(() -> new NotFoundException("No line with id " + id)));
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag(ModWebFeatures.TAG_LINES_AND_CATEGORIES)
            .summary("Get a line")
            .description("One line with the trains using it.")
            .returns(LineSnapshot.class)
            .shapeable()
            .notFound("No line with that id exists.")
            .build();
    }
}
