package de.mrjulsen.crn.web.endpoint.globalsettings;

import de.mrjulsen.crn.data.settings.GlobalSettings;
import de.mrjulsen.crn.web.ModWebFeatures;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import org.eclipse.jetty.server.Request;
import de.mrjulsen.crn.web.api.ApiResult;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

public class TrainBlacklistEndpoint implements IEndpointHandler {

    @Override
    public ApiResult handle(Request request) {
        return ApiResult.json(GlobalSettings.getInstance().getAllBlacklistedTrains());
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
