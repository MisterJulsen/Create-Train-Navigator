/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.data.schedule.instruction;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.station.GlobalStation;

import net.createmod.catnip.data.Couple;

final class RouteScan {

    record Occupancy(Train present, Train onTrack, Train inBlock, Train inbound) {}

    private final Map<TrackEdge, Train> trainsByEdge = new IdentityHashMap<>();
    private final Map<UUID, Train> trainsBySignalBlock = new HashMap<>();
    private final Map<GlobalStation, Train> presentByStation = new HashMap<>();
    private final Map<GlobalStation, Train> inboundByStation = new HashMap<>();

    private RouteScan() {}

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

            for (UUID group : other.occupiedSignalBlocks.keySet()) {
                scan.trainsBySignalBlock.putIfAbsent(group, other);
            }

            GlobalStation current = other.getCurrentStation();
            if (current != null) {
                scan.presentByStation.putIfAbsent(current, other);
            }
            if (other.navigation != null && other.navigation.destination != null) {
                scan.inboundByStation.merge(other.navigation.destination, other,
                    (a, b) -> hasBetterClaim(b, a) ? b : a);
            }
        }
        return scan;
    }

    static boolean hasBetterClaim(Train a, Train b) {
        double da = a.navigation.distanceToDestination;
        double db = b.navigation.distanceToDestination;
        if (da != db) {
            return da < db;
        }
        return a.id.compareTo(b.id) < 0;
    }

    Occupancy at(TrackGraph graph, GlobalStation station) {
        Couple<TrackNode> nodes = nodesOf(graph, station);
        TrackEdge edge = nodes == null ? null : graph.getConnection(nodes);
        UUID group = edge == null ? null : blockOf(graph, station, edge);
        return new Occupancy(
            presentByStation.get(station),
            edge == null ? null : trainsByEdge.get(edge),
            group == null ? null : trainsBySignalBlock.get(group),
            inboundByStation.get(station));
    }

    private UUID blockOf(TrackGraph graph, GlobalStation station, TrackEdge edge) {
        UUID group = edge.getEdgeData().getGroupAtPosition(graph, station.getLocationOn(edge));
        return group == null || group.equals(graph.id) ? null : group;
    }

    private static Couple<TrackNode> nodesOf(TrackGraph graph, GlobalStation station) {
        if (station.edgeLocation == null) {
            return null;
        }
        Couple<TrackNode> nodes = station.edgeLocation.map(graph::locateNode);
        return nodes.getFirst() == null || nodes.getSecond() == null ? null : nodes;
    }

}
