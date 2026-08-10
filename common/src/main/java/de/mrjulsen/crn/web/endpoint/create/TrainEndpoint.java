package de.mrjulsen.crn.web.endpoint.create;

import com.simibubi.create.content.trains.entity.Train;
import de.mrjulsen.crn.api.core.snapshot.CreateTrainSnapshot;
import de.mrjulsen.crn.util.TrainUtils;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.ParamType;
import de.mrjulsen.crn.web.api.Request;
import de.mrjulsen.crn.web.api.Response;

import java.net.HttpURLConnection;
import java.util.Optional;
import java.util.UUID;

public class TrainEndpoint implements IEndpointHandler {

    @Override
    public Response handle(Request request) {
        UUID id = request.pathParameter("id", ParamType.UUID);
        Optional<Train> train = TrainUtils.getTrain(id);
        return train
                .map(value -> Response.json(CreateTrainSnapshot.of(value)))
                .orElseGet(() -> Response.error(HttpURLConnection.HTTP_NOT_FOUND, "No train with id " + id));
    }
}
