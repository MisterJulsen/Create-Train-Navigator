package de.mrjulsen.crn.web.endpoint.create;

import de.mrjulsen.crn.web.api.RequestParams;

import com.simibubi.create.content.trains.station.GlobalStation;
import de.mrjulsen.crn.api.core.snapshot.CreateStationSnapshot;
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

public class StationEndpoint implements IEndpointHandler {

    @Override
    public ApiResult handle(Request request) {
        UUID id = RequestParams.path(request, "id", ParamType.UUID);
        Optional<GlobalStation> station = TrainUtils.getAllStations().stream().filter(x -> x.id.equals(id)).findFirst();
        return station
                .map(globalStation -> ApiResult.json(CreateStationSnapshot.of(globalStation)))
                .orElseGet(() -> ApiResult.error(HttpURLConnection.HTTP_NOT_FOUND, "No station with id " + id));
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag(ModWebFeatures.TAG_CREATE)
            .summary("Get a Create station")
            .description("Raw Create station data by its id.")
            .returns(CreateStationSnapshot.class)
            .shapeable()
            .response(HttpURLConnection.HTTP_NOT_FOUND, "No station with that id exists.")
            .build();
    }
}
