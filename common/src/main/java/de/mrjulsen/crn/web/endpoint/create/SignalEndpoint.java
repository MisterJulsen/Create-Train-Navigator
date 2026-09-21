package de.mrjulsen.crn.web.endpoint.create;

import de.mrjulsen.crn.web.api.RequestParams;

import com.simibubi.create.content.trains.signal.SignalBoundary;
import de.mrjulsen.crn.api.core.snapshot.CreateSignalSnapshot;
import de.mrjulsen.crn.util.TrainUtils;
import de.mrjulsen.crn.web.ModWebFeatures;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.ParamType;
import org.eclipse.jetty.server.Request;
import de.mrjulsen.crn.web.api.ApiResult;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

import java.net.HttpURLConnection;
import java.util.Optional;
import java.util.UUID;

public class SignalEndpoint implements IEndpointHandler {

    @Override
    public ApiResult handle(Request request) {
        UUID id = RequestParams.path(request, "id", ParamType.UUID);
        Optional<SignalBoundary> signal = TrainUtils.getSignal(id);
        return signal
                .map(signalBoundary -> ApiResult.json(CreateSignalSnapshot.of(signalBoundary)))
                .orElseGet(() -> ApiResult.error(HttpURLConnection.HTTP_NOT_FOUND, "No signal with id " + id));
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag(ModWebFeatures.TAG_CREATE)
            .summary("Get a Create signal")
            .description("Raw Create signal data by its id.")
            .returns(CreateSignalSnapshot.class)
            .shapeable()
            .response(HttpURLConnection.HTTP_NOT_FOUND, "No signal with that id exists.")
            .build();
    }
}
