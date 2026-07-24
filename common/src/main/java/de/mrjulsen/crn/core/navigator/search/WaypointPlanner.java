package de.mrjulsen.crn.core.navigator.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import de.mrjulsen.crn.core.navigator.NavigationQuery;
import de.mrjulsen.crn.core.navigator.Waypoint;
import de.mrjulsen.crn.core.navigator.index.TimetableIndex;
import de.mrjulsen.crn.core.navigator.route.RouteJourney;
import de.mrjulsen.crn.core.navigator.route.RouteLeg;
import de.mrjulsen.crn.core.navigator.route.RouteTransfer;
import de.mrjulsen.crn.core.navigator.search.RaptorSearch.Ride;

public final class WaypointPlanner {

    private static final int FRONTIER_LIMIT = 6;

    private final NavigationQuery query;
    private final RaptorSearch search;
    private final JourneyBuilder builder;
    private final int[] nodes;
    private final long[] stays;

    public WaypointPlanner(TimetableIndex index, NavigationQuery query, int[] nodes, long[] stays) {
        this.query = query;
        this.search = new RaptorSearch(index, query);
        this.builder = new JourneyBuilder(index, query);
        this.nodes = nodes;
        this.stays = stays;
    }

    public static WaypointPlanner of(TimetableIndex index, NavigationQuery query, int origin, int destination, int[] waypointNodes) {
        int[] nodes = new int[waypointNodes.length + 2];
        nodes[0] = origin;
        System.arraycopy(waypointNodes, 0, nodes, 1, waypointNodes.length);
        nodes[nodes.length - 1] = destination;

        long[] stays = new long[waypointNodes.length];
        List<Waypoint> waypoints = query.waypoints();
        for (int i = 0; i < stays.length; i++) {
            stays[i] = waypoints.get(i).minStay();
        }

        return new WaypointPlanner(index, query, nodes, stays);
    }

    public List<RouteJourney> plan(long departAfter) {
        List<RouteJourney> partials = stretch(nodes[0], nodes[1], departAfter);

        for (int i = 1; i < nodes.length - 1; i++) {
            if (partials.isEmpty()) {
                return List.of();
            }
            List<RouteJourney> continued = new ArrayList<>();
            for (RouteJourney partial : partials) {
                long readyAt = partial.arrival() + Math.max(stays[i - 1], query.minTransferTime());
                for (RouteJourney tail : stretch(nodes[i], nodes[i + 1], readyAt)) {
                    continued.add(join(partial, tail));
                }
            }
            partials = frontier(continued);
        }

        return frontier(partials);
    }

    public int getTripsScanned() {
        return search.getTripsScanned();
    }

    private List<RouteJourney> stretch(int from, int to, long departAfter) {
        List<List<Ride>> chains = search.run(from, to, departAfter);
        List<RouteJourney> journeys = new ArrayList<>(chains.size());
        for (List<Ride> chain : chains) {
            RouteJourney journey = builder.build(chain);
            if (journey != null) {
                journeys.add(journey);
            }
        }
        return journeys;
    }

    private RouteJourney join(RouteJourney head, RouteJourney tail) {
        List<RouteLeg> legs = new ArrayList<>(head.legs().size() + tail.legs().size());
        legs.addAll(head.legs());
        legs.addAll(tail.legs());

        List<RouteTransfer> transfers = new ArrayList<>(head.transfers().size() + tail.transfers().size() + 1);
        transfers.addAll(head.transfers());
        transfers.add(builder.connect(false));
        transfers.addAll(tail.transfers());

        return new RouteJourney(legs, transfers);
    }

    private static List<RouteJourney> frontier(List<RouteJourney> journeys) {
        List<RouteJourney> sorted = new ArrayList<>(journeys);
        sorted.sort(Comparator.comparingLong(RouteJourney::arrival).thenComparingInt(RouteJourney::transferCount));

        List<RouteJourney> kept = new ArrayList<>(Math.min(sorted.size(), FRONTIER_LIMIT));
        int fewestTransfers = Integer.MAX_VALUE;
        for (RouteJourney journey : sorted) {
            if (journey.transferCount() >= fewestTransfers) {
                continue;
            }
            fewestTransfers = journey.transferCount();
            kept.add(journey);
            if (kept.size() >= FRONTIER_LIMIT) {
                break;
            }
        }
        return kept;
    }
}
