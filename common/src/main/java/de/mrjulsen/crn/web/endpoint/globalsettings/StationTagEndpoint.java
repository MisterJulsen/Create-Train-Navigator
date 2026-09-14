package de.mrjulsen.crn.web.endpoint.globalsettings;

import de.mrjulsen.crn.data.settings.GlobalSettings;
import de.mrjulsen.crn.data.settings.StationTag;
import de.mrjulsen.crn.web.api.*;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

import java.net.HttpURLConnection;
import java.util.Optional;
import java.util.UUID;

public class StationTagEndpoint implements IEndpointHandler {

    @Override
    public Response handle(Request request) {
        UUID id = request.pathParameter("id", ParamType.UUID);
        Optional<StationTag> tag = GlobalSettings.getInstance().getStationTag(id);
        return tag
                .map(Response::json)
                .orElseGet(() -> Response.error(HttpURLConnection.HTTP_NOT_FOUND, "No station tag with id " + id));
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag("Global Settings")
            .summary("Get a station tag")
            .description("A station tag by its id.")
            .returns(StationTag.class)
            .shapeable()
            .response(HttpURLConnection.HTTP_NOT_FOUND, "No station tag with that id exists.")
            .build();
    }
}
