package de.mrjulsen.crn.core.navigator;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import de.mrjulsen.crn.api.core.ref.TrainCategoryRef;
import de.mrjulsen.crn.api.core.ref.LineRef;
import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.web.annotation.QueryModel;
import de.mrjulsen.crn.web.annotation.QueryParam;

/**
 * Describes a route search: where to travel, when, and within what limits. Passed to
 * {@link Navigator#search(NavigationQuery)}.
 * <p>
 * Build one from {@link #from(String)} and refine it with the chained methods; instances are
 * immutable, so each method returns a new query. Times are in the unit described by
 * {@link RailwayBackendApi#getCurrentTime()}.
 *
 * @param origin             Where the journey starts.
 * @param destination        Where the journey ends.
 * @param waypoints          Stations the journey must pass through, in order.
 * @param departAfter        The earliest departure time, or {@link #NOW} to start from the current
 *                           time.
 * @param minTransferTime    The least time to allow for a transfer, in ticks.
 * @param maxTransfers       The most transfers a journey may have, capped at {@link #TRANSFER_LIMIT}.
 * @param directOnly         Whether only journeys that need no transfer are accepted.
 * @param optimization       How the found journeys are ordered.
 * @param excludedCategories Categories the journey must avoid.
 * @param includedCategories If non-empty, the only categories the journey may use.
 * @param excludedLines      Lines the journey must avoid.
 * @param includedLines      If non-empty, the only lines the journey may use.
 * @param avoidedStations    Stations the journey must not call at.
 * @param transferRiskBuffer Extra time added on top of the transfer time before a connection counts
 *                           as safe, in ticks.
 * @param maxResults         The most journeys to return.
 * @param searchHorizon      How far past the departure time to search, in ticks.
 */
@QueryModel
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

    /** The default minimum transfer time, in ticks. */
    public static final long DEFAULT_TRANSFER_TIME = 1000;
    /** The default limit on transfers. */
    public static final int DEFAULT_MAX_TRANSFERS = 4;
    /** The hard cap on how many transfers a query may ask for. */
    public static final int TRANSFER_LIMIT = 8;
    /** The default transfer risk buffer, in ticks. */
    public static final long DEFAULT_TRANSFER_RISK_BUFFER = 200;
    /** The default number of journeys to return. */
    public static final int DEFAULT_MAX_RESULTS = 6;
    /** The default search horizon, in ticks. */
    public static final long DEFAULT_SEARCH_HORIZON = Long.MAX_VALUE / 4;
    /** Used as the departure time to search from the current time. */
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

    /** A query starting a journey at the given station, with default limits. */
    @QueryParam(value = "from", required = true)
    public static NavigationQuery from(String origin) {
        return new NavigationQuery(origin, "", List.of(), NOW, DEFAULT_TRANSFER_TIME,
            DEFAULT_MAX_TRANSFERS, false, RouteOptimization.FASTEST,
            Set.of(), Set.of(), Set.of(), Set.of(), Set.of(),
            DEFAULT_TRANSFER_RISK_BUFFER, DEFAULT_MAX_RESULTS, DEFAULT_SEARCH_HORIZON);
    }

    /** Sets the destination. */
    @QueryParam(value = "to", required = true)
    public NavigationQuery to(String destination) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, minTransferTime,
            maxTransfers, directOnly, optimization, excludedCategories, includedCategories,
            excludedLines, includedLines, avoidedStations, transferRiskBuffer, maxResults, searchHorizon);
    }

    /** Adds a waypoint the journey must pass through. */
    public NavigationQuery via(String station) {
        return via(Waypoint.of(station));
    }

    /** Adds a waypoint the journey must pass through, staying at least the given time, in ticks. */
    public NavigationQuery via(String station, long minStay) {
        return via(Waypoint.of(station, minStay));
    }

    /** Adds the given waypoint. */
    public NavigationQuery via(Waypoint waypoint) {
        List<Waypoint> combined = new ArrayList<>(waypoints);
        combined.add(waypoint);
        return withWaypoints(combined);
    }

    /** Replaces the waypoints. */
    @QueryParam(value = "via")
    public NavigationQuery withWaypoints(List<Waypoint> waypoints) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, minTransferTime,
            maxTransfers, directOnly, optimization, excludedCategories, includedCategories,
            excludedLines, includedLines, avoidedStations, transferRiskBuffer, maxResults, searchHorizon);
    }

    /** Sets the earliest departure time. */
    public NavigationQuery departingAfter(long time) {
        return new NavigationQuery(origin, destination, waypoints, time, minTransferTime,
            maxTransfers, directOnly, optimization, excludedCategories, includedCategories,
            excludedLines, includedLines, avoidedStations, transferRiskBuffer, maxResults, searchHorizon);
    }

    /** Sets the earliest departure to the given number of ticks from now. */
    @QueryParam(value = "departure_in")
    public NavigationQuery departingIn(long ticksFromNow) {
        return departingAfter(RailwayBackendApi.getCurrentTime() + Math.max(0, ticksFromNow));
    }

    /** Sets the least time to allow for a transfer, in ticks. */
    @QueryParam(value = "transfer_time")
    public NavigationQuery withMinTransferTime(long ticks) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, ticks,
            maxTransfers, directOnly, optimization, excludedCategories, includedCategories,
            excludedLines, includedLines, avoidedStations, transferRiskBuffer, maxResults, searchHorizon);
    }

    /** Sets the most transfers a journey may have. */
    public NavigationQuery withMaxTransfers(int maxTransfers) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, minTransferTime,
            maxTransfers, directOnly, optimization, excludedCategories, includedCategories,
            excludedLines, includedLines, avoidedStations, transferRiskBuffer, maxResults, searchHorizon);
    }

    /** Restricts the search to journeys that need no transfer. */
    public NavigationQuery onlyDirect() {
        return withDirectOnly(true);
    }

    /** Sets whether only direct journeys are accepted. */
    @QueryParam(value = "direct")
    public NavigationQuery withDirectOnly(boolean directOnly) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, minTransferTime,
            maxTransfers, directOnly, optimization, excludedCategories, includedCategories,
            excludedLines, includedLines, avoidedStations, transferRiskBuffer, maxResults, searchHorizon);
    }

    /** Sets how the found journeys are ordered. */
    @QueryParam(value = "optimization")
    public NavigationQuery preferring(RouteOptimization optimization) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, minTransferTime,
            maxTransfers, directOnly, optimization, excludedCategories, includedCategories,
            excludedLines, includedLines, avoidedStations, transferRiskBuffer, maxResults, searchHorizon);
    }

    /** Excludes journeys using any of the given categories. */
    @QueryParam(value = "excluding_categories")
    public NavigationQuery excludingCategories(Set<UUID> categoryIds) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, minTransferTime,
            maxTransfers, directOnly, optimization, categoryIds, includedCategories,
            excludedLines, includedLines, avoidedStations, transferRiskBuffer, maxResults, searchHorizon);
    }

    /** Restricts journeys to the given categories. */
    @QueryParam(value = "only_categories")
    public NavigationQuery onlyCategories(Set<UUID> categoryIds) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, minTransferTime,
            maxTransfers, directOnly, optimization, excludedCategories, categoryIds,
            excludedLines, includedLines, avoidedStations, transferRiskBuffer, maxResults, searchHorizon);
    }

    /** Excludes journeys using any of the given lines. */
    @QueryParam(value = "excluding_lines")
    public NavigationQuery excludingLines(Set<UUID> lineIds) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, minTransferTime,
            maxTransfers, directOnly, optimization, excludedCategories, includedCategories,
            lineIds, includedLines, avoidedStations, transferRiskBuffer, maxResults, searchHorizon);
    }

    /** Restricts journeys to the given lines. */
    @QueryParam(value = "only_lines")
    public NavigationQuery onlyLines(Set<UUID> lineIds) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, minTransferTime,
            maxTransfers, directOnly, optimization, excludedCategories, includedCategories,
            excludedLines, lineIds, avoidedStations, transferRiskBuffer, maxResults, searchHorizon);
    }

    /** Excludes journeys calling at any of the given stations. */
    @QueryParam(value = "avoid_stations")
    public NavigationQuery avoidingStations(Set<String> stationNames) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, minTransferTime,
            maxTransfers, directOnly, optimization, excludedCategories, includedCategories,
            excludedLines, includedLines, stationNames, transferRiskBuffer, maxResults, searchHorizon);
    }

    /** Sets the extra time before a connection counts as safe, in ticks. */
    public NavigationQuery withTransferRiskBuffer(long ticks) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, minTransferTime,
            maxTransfers, directOnly, optimization, excludedCategories, includedCategories,
            excludedLines, includedLines, avoidedStations, ticks, maxResults, searchHorizon);
    }

    /** Sets the most journeys to return. */
    @QueryParam(value = "max_results")
    public NavigationQuery withMaxResults(int maxResults) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, minTransferTime,
            maxTransfers, directOnly, optimization, excludedCategories, includedCategories,
            excludedLines, includedLines, avoidedStations, transferRiskBuffer, maxResults, searchHorizon);
    }

    /** Sets how far past the departure time to search, in ticks. */
    public NavigationQuery withSearchHorizon(long ticks) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, minTransferTime,
            maxTransfers, directOnly, optimization, excludedCategories, includedCategories,
            excludedLines, includedLines, avoidedStations, transferRiskBuffer, maxResults, ticks);
    }

    /** The departure time to search from, resolving {@link #NOW} to the current time. */
    public long resolvedDepartAfter() {
        return departAfter < 0 ? RailwayBackendApi.getCurrentTime() : departAfter;
    }

    /** The most legs a journey may have, one more than its transfers, or one when direct only. */
    public int maxLegs() {
        return directOnly ? 1 : maxTransfers + 1;
    }

    /** Whether any waypoint is set. */
    public boolean hasWaypoints() {
        return !waypoints.isEmpty();
    }

    /** Whether both an origin and a destination are set. */
    public boolean isComplete() {
        return !origin.isBlank() && !destination.isBlank();
    }

    /** Whether a service on the given line and category is allowed by the include and exclude filters. */
    public boolean accepts(LineRef line, TrainCategoryRef category) {
        return acceptsId(line == null ? null : line.id(), includedLines, excludedLines)
            && acceptsId(category == null ? null : category.id(), includedCategories, excludedCategories);
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
