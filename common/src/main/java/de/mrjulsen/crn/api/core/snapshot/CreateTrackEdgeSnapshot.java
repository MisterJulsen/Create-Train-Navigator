package de.mrjulsen.crn.api.core.snapshot;

import com.simibubi.create.content.trains.graph.TrackEdge;

public record CreateTrackEdgeSnapshot(
        int node1,
        int node2,
        double length,
        boolean turn,
        boolean interDimensional
) {

    public static CreateTrackEdgeSnapshot of(TrackEdge edge) {
        return new CreateTrackEdgeSnapshot(
                edge.node1.getNetId(),
                edge.node2.getNetId(),
                edge.getLength(),
                edge.isTurn(),
                edge.isInterDimensional()
        );
    }
}
