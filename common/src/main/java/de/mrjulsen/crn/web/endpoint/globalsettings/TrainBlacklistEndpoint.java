package de.mrjulsen.crn.web.endpoint.globalsettings;

import de.mrjulsen.crn.data.settings.GlobalSettings;
import de.mrjulsen.crn.web.ModWebFeatures;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.Request;
import de.mrjulsen.crn.web.api.Response;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

public class TrainBlacklistEndpoint implements IEndpointHandler {

    @Override
    public Response handle(Request request) {
        return Response.json(GlobalSettings.getInstance().getAllBlacklistedTrains());
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag(ModWebFeatures.TAG_GLOBAL_SETTINGS)
            .summary("List blacklisted trains")
            .description("The names of all blacklisted trains.")
            .returnsList(String.class)
            .build();
    }
}
