package de.mrjulsen.crn.web.endpoint.globalsettings;

import de.mrjulsen.crn.data.settings.GlobalSettings;
import de.mrjulsen.crn.data.settings.StationTag;
import de.mrjulsen.crn.data.settings.TrainLine;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.ParamType;
import de.mrjulsen.crn.web.api.Request;
import de.mrjulsen.crn.web.api.Response;

import java.net.HttpURLConnection;
import java.util.Optional;
import java.util.UUID;

public class TrainLineEndpoint implements IEndpointHandler {

    @Override
    public Response handle(Request request) {
        UUID id = request.pathParameter("id", ParamType.UUID);
        Optional<TrainLine> tag = GlobalSettings.getInstance().getTrainLine(id);
        return tag
                .map(x -> Response.json(x))
                .orElseGet(() -> Response.error(HttpURLConnection.HTTP_NOT_FOUND, "No train line with id " + id));
    }
}
