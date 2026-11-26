package de.mrjulsen.crn.web.endpoint.create;

import com.simibubi.create.content.trains.entity.Train;
import de.mrjulsen.crn.api.core.query.CreateTrainQuery;
import de.mrjulsen.crn.api.core.snapshot.CreateTrainSnapshot;
import de.mrjulsen.crn.util.TrainUtils;
import de.mrjulsen.crn.web.ModWebFeatures;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.QueryBinder;
import org.eclipse.jetty.server.Request;
import de.mrjulsen.crn.web.api.ApiResult;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

import java.util.ArrayList;
import java.util.List;

public class AllTrainsEndpoint implements IEndpointHandler {

    @Override
    public ApiResult handle(Request request) {
        CreateTrainQuery query = QueryBinder.bind(request, CreateTrainQuery.class);

	    List<CreateTrainSnapshot> acceptedTrains = new ArrayList<>();
		for (Train train : TrainUtils.getAllTrains(false))
			if (query.accept(train)) acceptedTrains.add(CreateTrainSnapshot.of(train));

        return ApiResult.json(acceptedTrains);
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag(ModWebFeatures.TAG_CREATE)
            .summary("List Create trains")
            .description("Raw train data from Create, not processed or modified by CRN's backend.")
            .query(CreateTrainQuery.class)
            .returnsList(CreateTrainSnapshot.class)
            .shapeable()
            .build();
    }
}
