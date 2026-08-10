package de.mrjulsen.crn.web.endpoint.create;

import com.simibubi.create.content.trains.station.GlobalStation;
import de.mrjulsen.crn.api.core.snapshot.CreateStationSnapshot;
import de.mrjulsen.crn.util.TrainUtils;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.ParamType;
import de.mrjulsen.crn.web.api.Request;
import de.mrjulsen.crn.web.api.Response;

import java.net.HttpURLConnection;
import java.util.Optional;
import java.util.UUID;

public class StationEndpoint implements IEndpointHandler {

    @Override
    public Response handle(Request request) {
        UUID id = request.pathParameter("id", ParamType.UUID);
        Optional<GlobalStation> station = TrainUtils.getAllStations().stream().filter(x -> x.id.equals(id)).findFirst();
        return station
                .map(globalStation -> Response.json(CreateStationSnapshot.of(globalStation)))
                .orElseGet(() -> Response.error(HttpURLConnection.HTTP_NOT_FOUND, "No station with id " + id));
    }
}
