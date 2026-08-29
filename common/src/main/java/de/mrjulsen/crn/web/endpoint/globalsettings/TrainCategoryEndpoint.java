package de.mrjulsen.crn.web.endpoint.globalsettings;

import de.mrjulsen.crn.data.settings.GlobalSettings;
import de.mrjulsen.crn.data.settings.TrainCategory;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.ParamType;
import de.mrjulsen.crn.web.api.Request;
import de.mrjulsen.crn.web.api.Response;

import java.net.HttpURLConnection;
import java.util.Optional;
import java.util.UUID;

public class TrainCategoryEndpoint implements IEndpointHandler {

    @Override
    public Response handle(Request request) {
        UUID id = request.pathParameter("id", ParamType.UUID);
        Optional<TrainCategory> tag = GlobalSettings.getInstance().getTrainCategory(id);
        return tag
                .map(x -> Response.json(x))
                .orElseGet(() -> Response.error(HttpURLConnection.HTTP_NOT_FOUND, "No train category with id " + id));
    }
}
