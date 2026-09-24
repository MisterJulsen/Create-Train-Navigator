package de.mrjulsen.crn.web.endpoint.create;

import de.mrjulsen.crn.web.api.RequestParams;

import com.simibubi.create.content.trains.entity.Train;
import de.mrjulsen.crn.util.TrainUtils;
import de.mrjulsen.crn.web.ModWebFeatures;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.ParamType;
import org.eclipse.jetty.server.Request;
import de.mrjulsen.crn.web.api.ApiResult;
import de.mrjulsen.crn.web.openapi.ContentSpec;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;
import de.mrjulsen.crn.web.openapi.SchemaSpec;

import java.net.HttpURLConnection;
import java.util.Optional;
import java.util.UUID;

public class TrainScheduleEndpoint implements IEndpointHandler {

    @Override
    public ApiResult handle(Request request) {
        UUID id = RequestParams.path(request, "id", ParamType.UUID);
        Optional<Train> train = TrainUtils.getTrain(id);
        return train
                .map(value -> ApiResult.json(value.runtime.schedule))
                .orElseGet(() -> ApiResult.error(HttpURLConnection.HTTP_NOT_FOUND, "No train with id " + id));
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag(ModWebFeatures.TAG_CREATE)
            .summary("Get a Create train's schedule")
            .description("The raw Create schedule assigned to a train.")
            .returns(ContentSpec.json(SchemaSpec.object()))
            .response(HttpURLConnection.HTTP_NOT_FOUND, "No train with that id exists.")
            .build();
    }
}
