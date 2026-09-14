package de.mrjulsen.crn.web.endpoint.globalsettings;

import de.mrjulsen.crn.data.settings.GlobalSettings;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.Request;
import de.mrjulsen.crn.web.api.Response;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

public class StationBlacklistEndpoint implements IEndpointHandler {

    @Override
    public Response handle(Request request) {
        return Response.json(GlobalSettings.getInstance().getAllBlacklistedStations());
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag("Global Settings")
            .summary("List blacklisted stations")
            .description("The names of blacklisted stations.")
            .returnsList(String.class)
            .build();
    }
}
