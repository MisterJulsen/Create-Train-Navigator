package de.mrjulsen.crn.web.endpoint.create;

import com.simibubi.create.content.trains.graph.TrackGraph;
import de.mrjulsen.crn.api.core.query.CreateTrackQuery;
import de.mrjulsen.crn.api.core.snapshot.CreateTrackGraphSnapshot;
import de.mrjulsen.crn.util.TrainUtils;
import de.mrjulsen.crn.web.api.IEndpointHandler;
import de.mrjulsen.crn.web.api.QueryBinder;
import de.mrjulsen.crn.web.api.Request;
import de.mrjulsen.crn.web.api.Response;

import java.util.ArrayList;
import java.util.List;

public class AllTracksEndpoint implements IEndpointHandler {

    @Override
    public Response handle(Request request) {
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
        return Response.json(graphs);
    }
}
