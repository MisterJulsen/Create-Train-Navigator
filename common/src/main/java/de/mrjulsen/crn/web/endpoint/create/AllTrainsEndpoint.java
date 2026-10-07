package de.mrjulsen.crn.web.endpoint.create;

import de.mrjulsen.crn.api.core.query.CreateTrainQuery;
import de.mrjulsen.crn.api.core.snapshot.CreateTrainSnapshot;
import de.mrjulsen.crn.util.TrainUtils;
import de.mrjulsen.crn.web.ModWebFeatures;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.QueryBinder;
import org.eclipse.jetty.server.Request;
import de.mrjulsen.crn.web.api.ApiResult;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

public class AllTrainsEndpoint implements IEndpointHandler {

    @Override
    public ApiResult handle(Request request) {
        CreateTrainQuery query = QueryBinder.bind(request, CreateTrainQuery.class);
        return ApiResult.json(TrainUtils.getAllTrains(false).stream().filter(query::accept).map(CreateTrainSnapshot::of).toList());
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
