package de.mrjulsen.crn.api.core.snapshot;

import com.simibubi.create.content.trains.graph.TrackEdge;

/**
 * A plain data view of one edge between two nodes in Create's track graph.
 *
 * @param node1            The network id of the edge's first node.
 * @param node2            The network id of the edge's second node.
 * @param length           The edge's length, in blocks.
 * @param turn             Whether the edge curves rather than running straight.
 * @param interDimensional Whether the edge crosses between two dimensions.
 */
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
