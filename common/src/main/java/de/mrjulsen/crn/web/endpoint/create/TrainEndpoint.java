package de.mrjulsen.crn.web.endpoint.create;

import de.mrjulsen.crn.web.api.RequestParams;

import com.simibubi.create.content.trains.entity.Train;
import de.mrjulsen.crn.api.core.snapshot.CreateTrainSnapshot;
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

public class TrainEndpoint implements IEndpointHandler {

    @Override
    public ApiResult handle(Request request) {
        UUID id = RequestParams.path(request, "id", ParamType.UUID);
        Optional<Train> train = TrainUtils.getTrain(id);
        return train
                .map(value -> ApiResult.json(CreateTrainSnapshot.of(value)))
                .orElseGet(() -> ApiResult.error(HttpURLConnection.HTTP_NOT_FOUND, "No train with id " + id));
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag(ModWebFeatures.TAG_CREATE)
            .summary("Get a Create train")
            .description("Raw Create train data by its id.")
            .returns(CreateTrainSnapshot.class)
            .shapeable()
            .response(HttpURLConnection.HTTP_NOT_FOUND, "No train with that id exists.")
            .build();
    }
}
