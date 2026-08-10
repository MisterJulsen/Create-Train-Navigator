package de.mrjulsen.crn.web.endpoint.create;

import com.simibubi.create.content.trains.signal.SignalBoundary;
import de.mrjulsen.crn.api.core.snapshot.CreateSignalSnapshot;
import de.mrjulsen.crn.util.TrainUtils;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.ParamType;
import de.mrjulsen.crn.web.api.Request;
import de.mrjulsen.crn.web.api.Response;

import java.net.HttpURLConnection;
import java.util.Optional;
import java.util.UUID;

public class SignalEndpoint implements IEndpointHandler {

    @Override
    public Response handle(Request request) {
        UUID id = request.pathParameter("id", ParamType.UUID);
        Optional<SignalBoundary> signal = TrainUtils.getSignal(id);
        return signal
                .map(signalBoundary -> Response.json(CreateSignalSnapshot.of(signalBoundary)))
                .orElseGet(() -> Response.error(HttpURLConnection.HTTP_NOT_FOUND, "No signal with id " + id));
    }
}
