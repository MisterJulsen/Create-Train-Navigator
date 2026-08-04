package de.mrjulsen.crn.core.navigator;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import de.mrjulsen.crn.api.core.ref.CategoryRef;
import de.mrjulsen.crn.api.core.ref.LineRef;
import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.web.annotation.RestQueryModel;
import de.mrjulsen.crn.web.annotation.RestQueryParam;

@RestQueryModel
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

    public static final long DEFAULT_TRANSFER_TIME = 1000;

    public static final int DEFAULT_MAX_TRANSFERS = 4;

    public static final int TRANSFER_LIMIT = 8;

    public static final long DEFAULT_TRANSFER_RISK_BUFFER = 200;

    public static final int DEFAULT_MAX_RESULTS = 6;

    public static final long DEFAULT_SEARCH_HORIZON = 24000;

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

    @RestQueryParam(value = "from", required = true)
    public static NavigationQuery from(String origin) {
        return new NavigationQuery(origin, "", List.of(), NOW, DEFAULT_TRANSFER_TIME,
            DEFAULT_MAX_TRANSFERS, false, RouteOptimization.FASTEST,
            Set.of(), Set.of(), Set.of(), Set.of(), Set.of(),
            DEFAULT_TRANSFER_RISK_BUFFER, DEFAULT_MAX_RESULTS, DEFAULT_SEARCH_HORIZON);
    }

    @RestQueryParam(value = "to", required = true)
    public NavigationQuery to(String destination) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, minTransferTime,
            maxTransfers, directOnly, optimization, excludedCategories, includedCategories,
            excludedLines, includedLines, avoidedStations, transferRiskBuffer, maxResults, searchHorizon);
    }

    public NavigationQuery via(String station) {
        return via(Waypoint.of(station));
    }

    public NavigationQuery via(String station, long minStay) {
        return via(Waypoint.of(station, minStay));
    }

    public NavigationQuery via(Waypoint waypoint) {
        List<Waypoint> combined = new ArrayList<>(waypoints);
        combined.add(waypoint);
        return withWaypoints(combined);
    }

    @RestQueryParam(value = "via")
    public NavigationQuery withWaypoints(List<Waypoint> waypoints) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, minTransferTime,
            maxTransfers, directOnly, optimization, excludedCategories, includedCategories,
            excludedLines, includedLines, avoidedStations, transferRiskBuffer, maxResults, searchHorizon);
    }

    public NavigationQuery departingAfter(long time) {
        return new NavigationQuery(origin, destination, waypoints, time, minTransferTime,
            maxTransfers, directOnly, optimization, excludedCategories, includedCategories,
            excludedLines, includedLines, avoidedStations, transferRiskBuffer, maxResults, searchHorizon);
    }

    @RestQueryParam(value = "departure_in")
    public NavigationQuery departingIn(long ticksFromNow) {
        return departingAfter(RailwayBackendApi.getCurrentTime() + Math.max(0, ticksFromNow));
    }

    @RestQueryParam(value = "transfer_time")
    public NavigationQuery withMinTransferTime(long ticks) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, ticks,
            maxTransfers, directOnly, optimization, excludedCategories, includedCategories,
            excludedLines, includedLines, avoidedStations, transferRiskBuffer, maxResults, searchHorizon);
    }

    public NavigationQuery withMaxTransfers(int maxTransfers) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, minTransferTime,
            maxTransfers, directOnly, optimization, excludedCategories, includedCategories,
            excludedLines, includedLines, avoidedStations, transferRiskBuffer, maxResults, searchHorizon);
    }

    public NavigationQuery onlyDirect() {
        return withDirectOnly(true);
    }

    @RestQueryParam(value = "direct")
    public NavigationQuery withDirectOnly(boolean directOnly) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, minTransferTime,
            maxTransfers, directOnly, optimization, excludedCategories, includedCategories,
            excludedLines, includedLines, avoidedStations, transferRiskBuffer, maxResults, searchHorizon);
    }

    @RestQueryParam(value = "optimization")
    public NavigationQuery preferring(RouteOptimization optimization) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, minTransferTime,
            maxTransfers, directOnly, optimization, excludedCategories, includedCategories,
            excludedLines, includedLines, avoidedStations, transferRiskBuffer, maxResults, searchHorizon);
    }

    @RestQueryParam(value = "excluding_categories")
    public NavigationQuery excludingCategories(Set<UUID> categoryIds) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, minTransferTime,
            maxTransfers, directOnly, optimization, categoryIds, includedCategories,
            excludedLines, includedLines, avoidedStations, transferRiskBuffer, maxResults, searchHorizon);
    }

    @RestQueryParam(value = "only_categories")
    public NavigationQuery onlyCategories(Set<UUID> categoryIds) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, minTransferTime,
            maxTransfers, directOnly, optimization, excludedCategories, categoryIds,
            excludedLines, includedLines, avoidedStations, transferRiskBuffer, maxResults, searchHorizon);
    }

    @RestQueryParam(value = "excluding_lines")
    public NavigationQuery excludingLines(Set<UUID> lineIds) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, minTransferTime,
            maxTransfers, directOnly, optimization, excludedCategories, includedCategories,
            lineIds, includedLines, avoidedStations, transferRiskBuffer, maxResults, searchHorizon);
    }

    @RestQueryParam(value = "only_lines")
    public NavigationQuery onlyLines(Set<UUID> lineIds) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, minTransferTime,
            maxTransfers, directOnly, optimization, excludedCategories, includedCategories,
            excludedLines, lineIds, avoidedStations, transferRiskBuffer, maxResults, searchHorizon);
    }

    @RestQueryParam(value = "avoid_stations")
    public NavigationQuery avoidingStations(Set<String> stationNames) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, minTransferTime,
            maxTransfers, directOnly, optimization, excludedCategories, includedCategories,
            excludedLines, includedLines, stationNames, transferRiskBuffer, maxResults, searchHorizon);
    }

    public NavigationQuery withTransferRiskBuffer(long ticks) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, minTransferTime,
            maxTransfers, directOnly, optimization, excludedCategories, includedCategories,
            excludedLines, includedLines, avoidedStations, ticks, maxResults, searchHorizon);
    }

    @RestQueryParam(value = "max_results")
    public NavigationQuery withMaxResults(int maxResults) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, minTransferTime,
            maxTransfers, directOnly, optimization, excludedCategories, includedCategories,
            excludedLines, includedLines, avoidedStations, transferRiskBuffer, maxResults, searchHorizon);
    }

    public NavigationQuery withSearchHorizon(long ticks) {
        return new NavigationQuery(origin, destination, waypoints, departAfter, minTransferTime,
            maxTransfers, directOnly, optimization, excludedCategories, includedCategories,
            excludedLines, includedLines, avoidedStations, transferRiskBuffer, maxResults, ticks);
    }

    public long resolvedDepartAfter() {
        return departAfter < 0 ? RailwayBackendApi.getCurrentTime() : departAfter;
    }

    public int maxLegs() {
        return directOnly ? 1 : maxTransfers + 1;
    }

    public boolean hasWaypoints() {
        return !waypoints.isEmpty();
    }

    public boolean isComplete() {
        return !origin.isBlank() && !destination.isBlank();
    }

    public boolean accepts(LineRef line, CategoryRef category) {
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
