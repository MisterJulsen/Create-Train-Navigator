package de.mrjulsen.crn.web.endpoint.create;

import com.simibubi.create.content.trains.graph.TrackGraph;
import de.mrjulsen.crn.api.core.query.CreateTrackQuery;
import de.mrjulsen.crn.api.core.snapshot.CreateTrackGraphSnapshot;
import de.mrjulsen.crn.util.TrainUtils;
import de.mrjulsen.crn.web.ModWebFeatures;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.QueryBinder;
import org.eclipse.jetty.server.Request;
import de.mrjulsen.crn.web.api.ApiResult;
import de.mrjulsen.crn.web.openapi.EndpointDocumentation;

import java.util.ArrayList;
import java.util.List;

public class AllTracksEndpoint implements IEndpointHandler {

    @Override
    public ApiResult handle(Request request) {
        CreateTrackQuery query = QueryBinder.bind(request, CreateTrackQuery.class);
        List<CreateTrackGraphSnapshot> graphs = new ArrayList<>();
        for (TrackGraph graph : TrainUtils.getRailwayManager().trackNetworks.values()) {
            if (!query.accept(graph)) {
                continue;
            }
            CreateTrackGraphSnapshot snapshot = CreateTrackGraphSnapshot.of(graph, query.dimension());
            if (snapshot.nodes().isEmpty()) {
                continue;
            }
            graphs.add(snapshot);
        }
        return ApiResult.json(graphs);
    }

    @Override
    public EndpointDocumentation getDocumentation() {
        return EndpointDocumentation.builder()
            .tag(ModWebFeatures.TAG_CREATE)
            .summary("List Create track graphs")
            .description("The raw track network from Create, grouped by track graphs.")
            .query(CreateTrackQuery.class)
            .returnsList(CreateTrackGraphSnapshot.class)
            .shapeable()
            .build();
    }
}
