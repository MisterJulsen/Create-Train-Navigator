package de.mrjulsen.crn.core.navigator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.core.navigator.index.TimetableIndex;
import de.mrjulsen.crn.core.navigator.route.RouteJourney;
import de.mrjulsen.crn.core.navigator.search.WaypointPlanner;

/**
 * Searches the timetable for journeys between stations. This is the entry point for route finding:
 * build a {@link NavigationQuery} describing what is wanted and pass it here.
 * <p>
 * Times are in the unit described by {@link RailwayBackendApi#getCurrentTime()}. The search reads the
 * backend, so it returns an empty result while no server is running.
 */
public final class Navigator {

    private Navigator() {}

    /**
     * Finds the journeys matching the query, best first according to its
     * {@link NavigationQuery#optimization()}. Where none are found, the result carries the reason
     * instead; see {@link NavigationStatus}.
     */
    public static NavigationResult search(NavigationQuery query) {
        long startedAt = System.currentTimeMillis();
        long now = RailwayBackendApi.getCurrentTime();

        if (!RailwayBackendApi.isActive()) {
            return NavigationResult.failed(NavigationStatus.BACKEND_INACTIVE, now, elapsed(startedAt));
        }
        if (!query.isComplete()) {
            return NavigationResult.failed(NavigationStatus.INCOMPLETE_QUERY, now, elapsed(startedAt));
        }

        long departAfter = query.resolvedDepartAfter();
        long relevanceUntil = query.searchHorizon() >= Long.MAX_VALUE - departAfter
            ? Long.MAX_VALUE
            : departAfter + query.searchHorizon();
        TimetableIndex index = TimetableIndex.obtain(departAfter);

        int origin = index.resolveNode(query.origin());
        int destination = index.resolveNode(query.destination());
        if (origin < 0 || destination < 0) {
            return NavigationResult.failed(NavigationStatus.UNKNOWN_STATION, now, elapsed(startedAt));
        }

        int[] waypoints = new int[query.waypoints().size()];
        for (int i = 0; i < waypoints.length; i++) {
            waypoints[i] = index.resolveNode(query.waypoints().get(i).station());
            if (waypoints[i] < 0) {
                return NavigationResult.failed(NavigationStatus.UNKNOWN_STATION, now, elapsed(startedAt));
            }
        }

        if (repeatsAStation(origin, destination, waypoints)) {
            return NavigationResult.failed(NavigationStatus.SAME_STATION, now, elapsed(startedAt));
        }

        WaypointPlanner planner = WaypointPlanner.of(index, query, origin, destination, waypoints);
        List<RouteJourney> found = collect(planner, query, departAfter, relevanceUntil);

        if (found.isEmpty()) {
            NavigationStatus status = query.directOnly() ? NavigationStatus.NO_DIRECT_ROUTE : NavigationStatus.NO_ROUTE;
            return NavigationResult.failed(status, now, elapsed(startedAt));
        }

        found.sort(query.optimization().comparator());
        return new NavigationResult(NavigationStatus.OK, found, now, elapsed(startedAt),
            index.nodeCount(), planner.getTripsScanned());
    }

    private static List<RouteJourney> collect(WaypointPlanner planner, NavigationQuery query, long departAfter, long until) {
        List<RouteJourney> found = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        long cursor = departAfter;

        int scans = 0;
        int maxScans = Math.max(query.maxResults() * 4, 32);

        while (found.size() < query.maxResults() && cursor <= until && scans++ < maxScans) {
            List<RouteJourney> batch = planner.plan(cursor);
            if (batch.isEmpty()) {
                break;
            }

            long earliestDeparture = Long.MAX_VALUE;
            for (RouteJourney journey : batch) {
                earliestDeparture = Math.min(earliestDeparture, journey.departure());
                if (found.size() < query.maxResults() && withinChangeLimit(journey, query)
                        && seen.add(journey.signature())) {
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

    private static boolean repeatsAStation(int origin, int destination, int[] waypoints) {
        Set<Integer> seen = new HashSet<>();
        seen.add(origin);
        for (int waypoint : waypoints) {
            if (!seen.add(waypoint)) {
                return true;
            }
        }
        return !seen.add(destination);
    }

    private static boolean withinChangeLimit(RouteJourney journey, NavigationQuery query) {
        return query.directOnly() ? journey.isDirect() : journey.transferCount() <= query.maxTransfers();
    }

    private static long elapsed(long startedAt) {
        return System.currentTimeMillis() - startedAt;
    }
}
