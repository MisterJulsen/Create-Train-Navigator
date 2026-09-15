package de.mrjulsen.crn.web.endpoint.create;

import de.mrjulsen.crn.api.core.query.CreateStationQuery;
import de.mrjulsen.crn.api.core.snapshot.CreateStationSnapshot;
import de.mrjulsen.crn.util.TrainUtils;
import de.mrjulsen.crn.web.ModWebFeatures;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.QueryBinder;
import de.mrjulsen.crn.web.api.Request;
import de.mrjulsen.crn.web.api.Response;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

public class AllStationsEndpoint implements IEndpointHandler {

    @Override
    public Response handle(Request request) {
        CreateStationQuery query = QueryBinder.bind(request, CreateStationQuery.class);
        return Response.json(TrainUtils.getAllStations().stream().filter(query::accept).map(CreateStationSnapshot::of).toList());
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag(ModWebFeatures.TAG_CREATE)
            .summary("List Create stations")
            .description("Raw station data from Create.")
            .query(CreateStationQuery.class)
            .returnsList(CreateStationSnapshot.class)
            .shapeable()
            .build();
    }
}
