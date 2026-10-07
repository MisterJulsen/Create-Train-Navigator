package de.mrjulsen.crn.web.endpoint.create;

import com.simibubi.create.content.trains.station.GlobalStation;
import de.mrjulsen.crn.api.core.query.CreateStationQuery;
import de.mrjulsen.crn.api.core.snapshot.CreateStationSnapshot;
import de.mrjulsen.crn.util.TrainUtils;
import de.mrjulsen.crn.web.ModWebFeatures;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.QueryBinder;
import org.eclipse.jetty.server.Request;
import de.mrjulsen.crn.web.api.ApiResult;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

import java.util.ArrayList;
import java.util.List;

public class AllStationsEndpoint implements IEndpointHandler {

    @Override
    public ApiResult handle(Request request) {
        CreateStationQuery query = QueryBinder.bind(request, CreateStationQuery.class);

		List<CreateStationSnapshot> acceptedStations = new ArrayList<>();
		for (GlobalStation station : TrainUtils.getAllStations())
			if (query.accept(station)) acceptedStations.add(CreateStationSnapshot.of(station));

        return ApiResult.json(acceptedStations);
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
