package de.mrjulsen.crn.web.endpoint.globalsettings;

import com.simibubi.create.content.trains.signal.SignalBoundary;
import de.mrjulsen.crn.api.core.snapshot.CreateSignalSnapshot;
import de.mrjulsen.crn.data.settings.GlobalSettings;
import de.mrjulsen.crn.data.settings.StationTag;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.crn.util.TrainUtils;
import de.mrjulsen.crn.web.annotation.QueryModel;
import de.mrjulsen.crn.web.annotation.QueryParam;
import de.mrjulsen.crn.web.api.*;

import java.net.HttpURLConnection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class StationTagEndpoint implements IEndpointHandler {

    @Override
    public Response handle(Request request) {
        UUID id = request.pathParameter("id", ParamType.UUID);
        Optional<StationTag> tag = GlobalSettings.getInstance().getStationTag(id);
        return tag
                .map(x -> Response.json(x))
                .orElseGet(() -> Response.error(HttpURLConnection.HTTP_NOT_FOUND, "No station tag with id " + id));
    }
}
