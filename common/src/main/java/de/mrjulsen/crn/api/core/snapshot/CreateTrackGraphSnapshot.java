package de.mrjulsen.crn.api.core.snapshot;

import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import de.mrjulsen.crn.web.annotation.ResponseAlwaysInclude;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record CreateTrackGraphSnapshot(
        @ResponseAlwaysInclude java.util.UUID id,
        List<CreateTrackNodeSnapshot> nodes,
        List<CreateTrackEdgeSnapshot> edges
) {

    public static CreateTrackGraphSnapshot of(TrackGraph graph, Set<ResourceLocation> dimensions) {
        List<CreateTrackNodeSnapshot> nodes = new ArrayList<>();
        Set<Integer> keptNetIds = new HashSet<>();
        for (TrackNodeLocation location : graph.getNodes()) {
            if (!dimensions.isEmpty() && !dimensions.contains(location.getDimension().location())) {
                continue;
            }
            TrackNode node = graph.locateNode(location);
            if (node == null) {
                continue;
            }
            nodes.add(CreateTrackNodeSnapshot.of(node));
            keptNetIds.add(node.getNetId());
        }

        List<CreateTrackEdgeSnapshot> edges = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (TrackNodeLocation location : graph.getNodes()) {
            TrackNode node = graph.locateNode(location);
            if (node == null || !keptNetIds.contains(node.getNetId())) {
                continue;
            }
            for (TrackEdge edge : graph.getConnectionsFrom(node).values()) {
                int a = edge.node1.getNetId();
                int b = edge.node2.getNetId();
                if (!keptNetIds.contains(a) || !keptNetIds.contains(b)) {
                    continue;
                }
                long key = (long) Math.min(a, b) << 32 | (Math.max(a, b) & 0xFFFFFFFFL);
                if (seen.add(key)) {
                    edges.add(CreateTrackEdgeSnapshot.of(edge));
                }
            }
        }

        return new CreateTrackGraphSnapshot(graph.id, nodes, edges);
    }
}
