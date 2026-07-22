package de.mrjulsen.crn.navigator.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import de.mrjulsen.crn.navigator.NavigationQuery;
import de.mrjulsen.crn.navigator.Waypoint;
import de.mrjulsen.crn.navigator.index.TimetableIndex;
import de.mrjulsen.crn.navigator.route.RouteJourney;
import de.mrjulsen.crn.navigator.route.RouteLeg;
import de.mrjulsen.crn.navigator.route.RouteTransfer;
import de.mrjulsen.crn.navigator.search.RaptorSearch.Ride;

/**
 * Plans a route that has to pass through stations the traveller named, by planning it one stretch at
 * a time and joining the results.
 * <p>
 * A route via somewhere is not the same problem as a route to somewhere, and treating it as one
 * search would mean carrying "have I been to the waypoints yet" through every step. Since the
 * waypoints are visited in a fixed order, the route splits at them into ordinary searches, each
 * starting when the one before it gets in.
 * <p>
 * Simply taking the earliest arrival at each waypoint is not enough: getting there sooner can mean
 * missing a connection that a slightly later arrival would have made. So every stretch keeps not one
 * answer but the whole set worth continuing from - earliest arrival for each number of changes - and
 * the next stretch is planned from all of them. That set stays small, which keeps this from growing
 * out of hand however many waypoints are named.
 *
 * <h2>Limits with waypoints</h2>
 * The query's limit on changes applies to each stretch rather than to the route as a whole, since
 * asking to travel via three places and change at most twice is usually not satisfiable at all.
 */
public final class WaypointPlanner {

    /** How many partial routes are carried from one stretch to the next. */
    private static final int FRONTIER_LIMIT = 6;

    private final NavigationQuery query;
    private final RaptorSearch search;
    private final JourneyBuilder builder;
    private final int[] nodes;
    private final long[] stays;

    /**
     * @param nodes The stations to visit in order: the origin, every waypoint, then the destination.
     * @param stays How long to stay at each waypoint, in the same order as the waypoints.
     */
    public WaypointPlanner(TimetableIndex index, NavigationQuery query, int[] nodes, long[] stays) {
        this.query = query;
        this.search = new RaptorSearch(index, query);
        this.builder = new JourneyBuilder(index, query);
        this.nodes = nodes;
        this.stays = stays;
    }

    /** Builds the planner for a query whose waypoints have already been resolved to nodes. */
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

    /**
     * The routes worth offering that leave at or after the given time, earliest arrival first.
     * <p>
     * Without waypoints this is one plain search. With them it is one per stretch, joined up.
     */
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

    /** How many trips the underlying search has ridden, for diagnostics. */
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

    /** Two stretches travelled one after the other, with the wait at the waypoint between them. */
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

    /**
     * The routes worth carrying forward: for every number of changes, the one that gets in earliest,
     * and only while that is actually earlier than doing it with fewer changes.
     */
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
