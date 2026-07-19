package de.mrjulsen.crn.navigator;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import de.mrjulsen.crn.backend.api.RailwayBackendApi;
import de.mrjulsen.crn.data.TrainCategory;
import de.mrjulsen.crn.data.TrainLine;

/**
 * Everything a route search needs to know: where the traveller is going, when, and what they are
 * willing to accept on the way.
 * <p>
 * Immutable and composable - every {@code with...} returns a new query, so one can be kept as a
 * default and varied per search. Build one with {@link #from(String)} and chain from there:
 *
 * <pre>{@code
 * NavigationQuery.from("Berlin").to("Munich")
 *     .via("Leipzig", 6000)
 *     .departingAfter(time)
 *     .withMinTransferTime(400)
 *     .preferring(RouteOptimization.FEWEST_TRANSFERS);
 * }</pre>
 *
 * @param origin            The station or station tag to start from.
 * @param destination       The station or station tag to reach.
 * @param waypoints         Stations to travel via, in the order they are to be visited.
 * @param departAfter       The earliest departure, in transformed game ticks. {@code -1} means now.
 * @param minTransferTime   How long a change of trains takes at minimum, in ticks.
 * @param maxTransfers      The most changes a route may have.
 * @param directOnly        Whether only routes without a single change are acceptable.
 * @param optimization      Which of the found routes to present first.
 * @param excludedCategories Train categories the traveller refuses to use.
 * @param includedCategories The only train categories to use. Empty means no restriction.
 * @param excludedLines     Train lines the traveller refuses to use.
 * @param includedLines     The only train lines to use. Empty means no restriction.
 * @param avoidedStations   Stations that may not be used to get on, off or change.
 * @param transferRiskBuffer How much slack a change needs beyond the feeding train's current delay
 *                          before it counts as comfortable, in ticks.
 * @param maxResults        How many routes to return at most.
 * @param searchHorizon     How far into the future to look for departures, in ticks.
 */
public record NavigationQuery(
    String origin,
    String destination,
    List<Waypoint> waypoints,
    long departAfter,
    long minTransferTime,
    int maxTransfers,
    boolean directOnly,
    RouteOptimization optimization,
    Set<UUID> excludedCategories,
    Set<UUID> includedCategories,
    Set<UUID> excludedLines,
    Set<UUID> includedLines,
    Set<String> avoidedStations,
    long transferRiskBuffer,
    int maxResults,
    long searchHorizon
) {

    /** The default time a change of trains is assumed to take, in ticks. */
    public static final long DEFAULT_TRANSFER_TIME = 1000;

    /** The default upper limit on changes, which also bounds how long a search may run. */
    public static final int DEFAULT_MAX_TRANSFERS = 4;

    /** The highest number of changes a search will ever consider, whatever a query asks for. */
    public static final int TRANSFER_LIMIT = 8;

    /** The default slack a change needs beyond the feeding train's delay to count as comfortable. */
    public static final long DEFAULT_TRANSFER_RISK_BUFFER = 200;

    /** How many routes are returned by default. */
    public static final int DEFAULT_MAX_RESULTS = 6;

    /** How far ahead departures are looked for by default, in ticks - one full day. */
    public static final long DEFAULT_SEARCH_HORIZON = 24000;

    /** Means "as soon as possible" wherever a departure time is expected. */
    public static final long NOW = -1;

    public NavigationQuery {
        origin = origin == null ? "" : origin.trim();
        destination = destination == null ? "" : destination.trim();
        waypoints = waypoints == null ? List.of() : List.copyOf(waypoints);
        minTransferTime = Math.max(0, minTransferTime);
        maxTransfers = Math.max(0, Math.min(TRANSFER_LIMIT, maxTransfers));
        optimization = optimization == null ? RouteOptimization.FASTEST : optimization;
        excludedCategories = copyOf(excludedCategories);
        includedCategories = copyOf(includedCategories);
        excludedLines = copyOf(excludedLines);
        includedLines = copyOf(includedLines);
        avoidedStations = copyOf(avoidedStations);
        transferRiskBuffer = Math.max(0, transferRiskBuffer);
        maxResults = Math.max(1, maxResults);
        searchHorizon = Math.max(1, searchHorizon);
    }

    /** Starts a query from the given station or station tag. */
    public static NavigationQuery from(String origin) {
        return new NavigationQuery(origin, "", List.of(), NOW, DEFAULT_TRANSFER_TIME,
            DEFAULT_MAX_TRANSFERS, false, RouteOptimization.FASTEST,
            Set.of(), Set.of(), Set.of(), Set.of(), Set.of(),
            DEFAULT_TRANSFER_RISK_BUFFER, DEFAULT_MAX_RESULTS, DEFAULT_SEARCH_HORIZON);
    }

    /** The station or station tag to travel to. */
    public NavigationQuery to(String destination) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, minTransferTime,
            maxTransfers, directOnly, optimization, excludedCategories, includedCategories,
            excludedLines, includedLines, avoidedStations, transferRiskBuffer, maxResults, searchHorizon);
    }

    /** Adds a station to travel via, after any waypoints already added. */
    public NavigationQuery via(String station) {
        return via(Waypoint.of(station));
    }

    /** Adds a station to travel via, staying there for at least the given number of ticks. */
    public NavigationQuery via(String station, long minStay) {
        return via(Waypoint.of(station, minStay));
    }

    /** Adds a waypoint, after any already added. */
    public NavigationQuery via(Waypoint waypoint) {
        List<Waypoint> combined = new ArrayList<>(waypoints);
        combined.add(waypoint);
        return withWaypoints(combined);
    }

    /** Replaces the waypoints with the given ones, in travel order. */
    public NavigationQuery withWaypoints(List<Waypoint> waypoints) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, minTransferTime,
            maxTransfers, directOnly, optimization, excludedCategories, includedCategories,
            excludedLines, includedLines, avoidedStations, transferRiskBuffer, maxResults, searchHorizon);
    }

    /** Looks for routes leaving at or after the given time, in transformed game ticks. */
    public NavigationQuery departingAfter(long time) {
        return new NavigationQuery(origin, destination, waypoints, time, minTransferTime,
            maxTransfers, directOnly, optimization, excludedCategories, includedCategories,
            excludedLines, includedLines, avoidedStations, transferRiskBuffer, maxResults, searchHorizon);
    }

    /** Looks for routes leaving at least the given number of ticks from now. */
    public NavigationQuery departingIn(long ticksFromNow) {
        return departingAfter(RailwayBackendApi.currentTime() + Math.max(0, ticksFromNow));
    }

    /** How long the traveller needs to change trains, in ticks. */
    public NavigationQuery withMinTransferTime(long ticks) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, ticks,
            maxTransfers, directOnly, optimization, excludedCategories, includedCategories,
            excludedLines, includedLines, avoidedStations, transferRiskBuffer, maxResults, searchHorizon);
    }

    /** The most changes a route may have. */
    public NavigationQuery withMaxTransfers(int maxTransfers) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, minTransferTime,
            maxTransfers, directOnly, optimization, excludedCategories, includedCategories,
            excludedLines, includedLines, avoidedStations, transferRiskBuffer, maxResults, searchHorizon);
    }

    /**
     * Accepts only routes served by a single train from start to finish. Such a route may not exist
     * at all, in which case the search returns nothing rather than falling back to one with changes.
     */
    public NavigationQuery onlyDirect() {
        return withDirectOnly(true);
    }

    /** Whether only routes without a single change are acceptable. */
    public NavigationQuery withDirectOnly(boolean directOnly) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, minTransferTime,
            maxTransfers, directOnly, optimization, excludedCategories, includedCategories,
            excludedLines, includedLines, avoidedStations, transferRiskBuffer, maxResults, searchHorizon);
    }

    /** Which of the found routes to present first. */
    public NavigationQuery preferring(RouteOptimization optimization) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, minTransferTime,
            maxTransfers, directOnly, optimization, excludedCategories, includedCategories,
            excludedLines, includedLines, avoidedStations, transferRiskBuffer, maxResults, searchHorizon);
    }

    /** Refuses the given train categories. */
    public NavigationQuery excludingCategories(Set<UUID> categoryIds) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, minTransferTime,
            maxTransfers, directOnly, optimization, categoryIds, includedCategories,
            excludedLines, includedLines, avoidedStations, transferRiskBuffer, maxResults, searchHorizon);
    }

    /** Accepts only the given train categories. An empty set imposes no restriction. */
    public NavigationQuery onlyCategories(Set<UUID> categoryIds) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, minTransferTime,
            maxTransfers, directOnly, optimization, excludedCategories, categoryIds,
            excludedLines, includedLines, avoidedStations, transferRiskBuffer, maxResults, searchHorizon);
    }

    /** Refuses the given train lines. */
    public NavigationQuery excludingLines(Set<UUID> lineIds) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, minTransferTime,
            maxTransfers, directOnly, optimization, excludedCategories, includedCategories,
            lineIds, includedLines, avoidedStations, transferRiskBuffer, maxResults, searchHorizon);
    }

    /** Accepts only the given train lines. An empty set imposes no restriction. */
    public NavigationQuery onlyLines(Set<UUID> lineIds) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, minTransferTime,
            maxTransfers, directOnly, optimization, excludedCategories, includedCategories,
            excludedLines, lineIds, avoidedStations, transferRiskBuffer, maxResults, searchHorizon);
    }

    /**
     * Keeps the route away from the given stations. They may still be passed through without
     * stopping - what is refused is getting on, off or changing there.
     */
    public NavigationQuery avoidingStations(Set<String> stationNames) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, minTransferTime,
            maxTransfers, directOnly, optimization, excludedCategories, includedCategories,
            excludedLines, includedLines, stationNames, transferRiskBuffer, maxResults, searchHorizon);
    }

    /** How much slack a change needs beyond the feeding train's delay to count as comfortable. */
    public NavigationQuery withTransferRiskBuffer(long ticks) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, minTransferTime,
            maxTransfers, directOnly, optimization, excludedCategories, includedCategories,
            excludedLines, includedLines, avoidedStations, ticks, maxResults, searchHorizon);
    }

    /** How many routes to return at most. */
    public NavigationQuery withMaxResults(int maxResults) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, minTransferTime,
            maxTransfers, directOnly, optimization, excludedCategories, includedCategories,
            excludedLines, includedLines, avoidedStations, transferRiskBuffer, maxResults, searchHorizon);
    }

    /** How far into the future to look for departures, in ticks. */
    public NavigationQuery withSearchHorizon(long ticks) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, minTransferTime,
            maxTransfers, directOnly, optimization, excludedCategories, includedCategories,
            excludedLines, includedLines, avoidedStations, transferRiskBuffer, maxResults, ticks);
    }

    /** The earliest departure this query asks for, resolving {@link #NOW} to the current time. */
    public long resolvedDepartAfter() {
        return departAfter < 0 ? RailwayBackendApi.currentTime() : departAfter;
    }

    /** The most legs a route may consist of, which is one more than the number of changes. */
    public int maxLegs() {
        return directOnly ? 1 : maxTransfers + 1;
    }

    /** Whether this query asks for stations to be travelled via. */
    public boolean hasWaypoints() {
        return !waypoints.isEmpty();
    }

    /** Whether this query names both an origin and a destination. */
    public boolean isComplete() {
        return !origin.isBlank() && !destination.isBlank();
    }

    /** Whether a section carrying the given line and category may be used. */
    public boolean accepts(TrainLine line, TrainCategory category) {
        return acceptsId(line == null ? null : line.getId(), includedLines, excludedLines)
            && acceptsId(category == null ? null : category.getId(), includedCategories, excludedCategories);
    }

    /** Whether the traveller may get on, off or change at the given station. */
    public boolean acceptsStation(String stationName) {
        return avoidedStations.isEmpty() || !avoidedStations.contains(stationName);
    }

    private static boolean acceptsId(UUID id, Set<UUID> included, Set<UUID> excluded) {
        if (id == null) {
            return included.isEmpty();
        }
        return !excluded.contains(id) && (included.isEmpty() || included.contains(id));
    }

    private static <T> Set<T> copyOf(Set<T> set) {
        return set == null || set.isEmpty() ? Set.of() : Set.copyOf(new LinkedHashSet<>(set));
    }
}
