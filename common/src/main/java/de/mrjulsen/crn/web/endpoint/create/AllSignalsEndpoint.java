package de.mrjulsen.crn.web.endpoint.create;

import de.mrjulsen.crn.api.core.query.CreateSignalQuery;
import de.mrjulsen.crn.api.core.snapshot.CreateSignalSnapshot;
import de.mrjulsen.crn.util.TrainUtils;
import de.mrjulsen.crn.web.ModWebFeatures;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.QueryBinder;
import org.eclipse.jetty.server.Request;
import de.mrjulsen.crn.web.api.ApiResult;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

public class AllSignalsEndpoint implements IEndpointHandler {

    @Override
    public ApiResult handle(Request request) {
        CreateSignalQuery query = QueryBinder.bind(request, CreateSignalQuery.class);
        return ApiResult.json(TrainUtils.getAllSignals().stream().filter(query::accept).map(CreateSignalSnapshot::of).toList());
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag(ModWebFeatures.TAG_CREATE)
            .summary("List Create signals")
            .description("Raw signal data from Create.")
            .query(CreateSignalQuery.class)
            .returnsList(CreateSignalSnapshot.class)
            .shapeable()
            .build();
    }
}
