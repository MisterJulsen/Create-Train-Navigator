package de.mrjulsen.crn.data.schedule.instruction;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.signal.SignalBoundary;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import com.simibubi.create.content.trains.station.GlobalStation;

import net.createmod.catnip.data.Couple;

/**
 * Where every other train currently is, gathered once so that a whole priority list can be judged
 * without walking the train registry again for each entry.
 */
final class RouteScan {

    /**
     * What stands in the way at one station.
     *
     * @param present a train stopped at the station
     * @param onTrack a train standing on the station's own track without being stopped there
     * @param inbound a train navigating towards the station
     * @param heldRed whether a signal on the approach is forced red by redstone
     */
    record Occupancy(Train present, Train onTrack, Train inbound, boolean heldRed) {}

    private final Map<TrackEdge, Train> trainsByEdge = new IdentityHashMap<>();
    private final Map<GlobalStation, Train> presentByStation = new HashMap<>();
    private final Map<GlobalStation, Train> inboundByStation = new HashMap<>();

    private RouteScan() {}

    /** The asking train is left out, so everything found later is by definition someone else. */
    static RouteScan of(Train self, TrackGraph graph) {
        RouteScan scan = new RouteScan();
        for (Train other : Create.RAILWAYS.trains.values()) {
            if (other == self || other.graph != graph) {
                continue;
            }

            other.getEndpointEdges().forEach(nodes -> {
                if (nodes == null || nodes.getFirst() == null || nodes.getSecond() == null) {
                    return;
                }
                TrackEdge edge = graph.getConnection(nodes);
                if (edge != null) {
                    scan.trainsByEdge.putIfAbsent(edge, other);
                }
            });

            GlobalStation current = other.getCurrentStation();
            if (current != null) {
                scan.presentByStation.putIfAbsent(current, other);
            }
            if (other.navigation != null && other.navigation.destination != null) {
                scan.inboundByStation.putIfAbsent(other.navigation.destination, other);
            }
        }
        return scan;
    }

    /** Resolves the station's track once and answers everything that depends on it in one go. */
    Occupancy at(TrackGraph graph, GlobalStation station) {
        Couple<TrackNode> nodes = nodesOf(graph, station);
        TrackEdge edge = nodes == null ? null : graph.getConnection(nodes);
        return new Occupancy(
            presentByStation.get(station),
            edge == null ? null : trainsByEdge.get(edge),
            inboundByStation.get(station),
            edge != null && heldRed(edge, nodes, station));
    }

    private static boolean heldRed(TrackEdge edge, Couple<TrackNode> nodes, GlobalStation station) {
        if (!edge.getEdgeData().hasPoints()) {
            return false;
        }
        for (TrackNode towards : nodes) {
            if (!station.canApproachFrom(towards)) {
                continue;
            }
            for (TrackEdgePoint point : edge.getEdgeData().getPoints()) {
                if (point instanceof SignalBoundary signal && signal.isForcedRed(towards)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Couple<TrackNode> nodesOf(TrackGraph graph, GlobalStation station) {
        if (station.edgeLocation == null) {
            return null;
        }
        Couple<TrackNode> nodes = station.edgeLocation.map(graph::locateNode);
        return nodes.getFirst() == null || nodes.getSecond() == null ? null : nodes;
    }

}
