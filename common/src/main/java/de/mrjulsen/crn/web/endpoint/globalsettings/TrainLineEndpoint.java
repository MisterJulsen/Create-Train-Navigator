package de.mrjulsen.crn.web.endpoint.globalsettings;

import de.mrjulsen.crn.web.api.RequestParams;

import de.mrjulsen.crn.data.settings.GlobalSettings;
import de.mrjulsen.crn.data.settings.TrainLine;
import de.mrjulsen.crn.web.ModWebFeatures;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.ParamType;
import org.eclipse.jetty.server.Request;
import de.mrjulsen.crn.web.api.ApiResult;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

import java.net.HttpURLConnection;
import java.util.Optional;
import java.util.UUID;

public class TrainLineEndpoint implements IEndpointHandler {

    @Override
    public ApiResult handle(Request request) {
        UUID id = RequestParams.path(request, "id", ParamType.UUID);
        Optional<TrainLine> tag = GlobalSettings.getInstance().getTrainLine(id);
        return tag
                .map(x -> ApiResult.json(x))
                .orElseGet(() -> ApiResult.error(HttpURLConnection.HTTP_NOT_FOUND, "No train line with id " + id));
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag(ModWebFeatures.TAG_GLOBAL_SETTINGS)
            .summary("Get a train line")
            .description("A train line by its id.")
            .returns(TrainLine.class)
            .shapeable()
            .response(HttpURLConnection.HTTP_NOT_FOUND, "No train line with that id exists.")
            .build();
    }
}
