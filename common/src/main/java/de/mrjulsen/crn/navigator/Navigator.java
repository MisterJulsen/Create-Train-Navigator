package de.mrjulsen.crn.navigator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.mrjulsen.crn.backend.api.RailwayBackendApi;
import de.mrjulsen.crn.navigator.index.TimetableIndex;
import de.mrjulsen.crn.navigator.route.RouteJourney;
import de.mrjulsen.crn.navigator.search.WaypointPlanner;

/**
 * The route search: how to get from one station to another, and when.
 * <p>
 * This is the only entry point. Describe the journey with a {@link NavigationQuery} and hand it to
 * {@link #search(NavigationQuery)}; everything behind it - the timetable index, the search itself,
 * the planning around waypoints - is its own business.
 *
 * <pre>{@code
 * NavigationResult result = Navigator.search(
 *     NavigationQuery.from("Berlin").to("Munich").withMaxTransfers(2));
 * result.best().ifPresent(journey -> ...);
 * }</pre>
 *
 * <h2>What comes back</h2>
 * Not one route but several, and deliberately so. A traveller choosing between leaving now with two
 * changes and leaving in twenty minutes without any is making a judgement no search can make for
 * them, so both are offered. What the query's {@link RouteOptimization} decides is only which of
 * them is put first.
 * <p>
 * Every route returned is worth its place: it either leaves later, arrives earlier, or needs fewer
 * changes than every other one. Anything beaten on all three is left out.
 *
 * <h2>Cost</h2>
 * The expensive part is turning the backend's per-train data into departures per station, and that
 * result is shared between searches until the backend has moved on. The search proper is bounded by
 * the number of changes allowed rather than by the size of the network.
 *
 * <h2>Threading</h2>
 * <b>Server thread only</b>, because building the timetable index reads the live train objects. The
 * search proper does not, so once an index is built the rest could run anywhere - see
 * {@link TimetableIndex} for what stands in the way of that.
 */
public final class Navigator {

    private Navigator() {}

    /** Searches for routes as described by the given query. */
    public static NavigationResult search(NavigationQuery query) {
        long startedAt = System.currentTimeMillis();
        long now = RailwayBackendApi.currentTime();

        if (!RailwayBackendApi.isActive()) {
            return NavigationResult.failed(NavigationStatus.BACKEND_INACTIVE, now, elapsed(startedAt));
        }
        if (!query.isComplete()) {
            return NavigationResult.failed(NavigationStatus.INCOMPLETE_QUERY, now, elapsed(startedAt));
        }

        long departAfter = query.resolvedDepartAfter();
        long until = departAfter + query.searchHorizon();
        TimetableIndex index = TimetableIndex.obtain(departAfter, until);

        int origin = index.resolveNode(query.origin());
        int destination = index.resolveNode(query.destination());
        if (origin < 0 || destination < 0) {
            return NavigationResult.failed(NavigationStatus.UNKNOWN_STATION, now, elapsed(startedAt));
        }
        if (origin == destination) {
            return NavigationResult.failed(NavigationStatus.SAME_STATION, now, elapsed(startedAt));
        }

        int[] waypoints = new int[query.waypoints().size()];
        for (int i = 0; i < waypoints.length; i++) {
            waypoints[i] = index.resolveNode(query.waypoints().get(i).station());
            if (waypoints[i] < 0) {
                return NavigationResult.failed(NavigationStatus.UNKNOWN_STATION, now, elapsed(startedAt));
            }
        }

        WaypointPlanner planner = WaypointPlanner.of(index, query, origin, destination, waypoints);
        List<RouteJourney> found = collect(planner, query, departAfter, until);

        if (found.isEmpty()) {
            NavigationStatus status = query.directOnly() ? NavigationStatus.NO_DIRECT_ROUTE : NavigationStatus.NO_ROUTE;
            return NavigationResult.failed(status, now, elapsed(startedAt));
        }

        found.sort(query.optimization().comparator());
        return new NavigationResult(NavigationStatus.OK, found, now, elapsed(startedAt),
            index.nodeCount(), planner.getTripsScanned());
    }

    /**
     * Searches repeatedly, each time from just after the earliest departure the last search found,
     * until there are enough routes or nothing is left within the horizon.
     * <p>
     * One search answers "what is the best way to leave from now on", which yields the routes worth
     * taking at that moment but says nothing about a train an hour later that gets in just as soon
     * with half the changes. Asking again from after each departure is what turns a single best
     * answer into the list a traveller expects, and is why a later departure can appear alongside an
     * earlier one rather than being hidden behind it.
     */
    private static List<RouteJourney> collect(WaypointPlanner planner, NavigationQuery query, long departAfter, long until) {
        List<RouteJourney> found = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        long cursor = departAfter;

        while (found.size() < query.maxResults() && cursor <= until) {
            List<RouteJourney> batch = planner.plan(cursor);
            if (batch.isEmpty()) {
                break;
            }

            long earliestDeparture = Long.MAX_VALUE;
            for (RouteJourney journey : batch) {
                earliestDeparture = Math.min(earliestDeparture, journey.departure());
                if (found.size() < query.maxResults() && seen.add(journey.signature())) {
                    found.add(journey);
                }
            }

            if (earliestDeparture == Long.MAX_VALUE) {
                break;
            }
            cursor = earliestDeparture + 1;
        }

        return found;
    }

    private static long elapsed(long startedAt) {
        return System.currentTimeMillis() - startedAt;
    }
}
