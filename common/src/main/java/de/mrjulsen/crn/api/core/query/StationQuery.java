package de.mrjulsen.crn.api.core.query;

import de.mrjulsen.crn.api.core.snapshot.StationSnapshot;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.crn.util.TrainUtils;
import de.mrjulsen.crn.web.annotation.OpenApiDescription;
import de.mrjulsen.crn.web.annotation.QueryModel;
import de.mrjulsen.crn.web.annotation.QueryParam;

import java.util.*;

/**
 * A filter over the known stations. Each field that is set narrows the result, and a field left
 * empty imposes no constraint. Build one from {@link #all()} and the {@code with...} methods;
 * instances are immutable, so each method returns a new query.
 *
 * @param filter          If non-empty, keeps only stations whose name matches this filter, which may
 *                        use the schedule's wildcard syntax.
 * @param stationTags     If non-empty, keeps only stations belonging to one of these tags.
 * @param trains          If non-empty, keeps only stations served by one of these trains.
 * @param lines           If non-empty, keeps only stations served by one of these lines.
 * @param categories      If non-empty, keeps only stations served by one of these categories.
 * @param hideBlacklisted Whether to drop stations hidden from public display.
 */
@QueryModel
public record StationQuery(
        String filter,
        Set<UUID> stationTags,
        Set<UUID> trains,
        Set<UUID> lines,
        Set<UUID> categories,
        boolean hideBlacklisted
) {

    /** A query that keeps every station, as a starting point for refinement. */
    public static StationQuery all() {
        return new StationQuery("", Set.of(), Set.of(), Set.of(), Set.of(), false);
    }

    /** Keeps only stations whose name matches the given filter. */
    @QueryParam(value = "filter")
    @OpenApiDescription("Keep only stations whose name matches this filter, which may use the schedule's wildcard syntax.")
    public StationQuery withFilter(String filter) {
        Objects.requireNonNull(filter);
        return new StationQuery(filter, stationTags, trains, lines, categories, hideBlacklisted);
    }

    /** Keeps only stations belonging to one of the given tags. */
    @QueryParam(value = "station_tags")
    @OpenApiDescription("Keep only stations belonging to one of these tag ids.")
    public StationQuery withTags(Set<UUID> stationTags) {
        Objects.requireNonNull(stationTags);
        return new StationQuery(filter, stationTags, trains, lines, categories, hideBlacklisted);
    }

    /** Keeps only stations served by one of the given trains. */
    @QueryParam(value = "trains")
    @OpenApiDescription("Keep only stations served by one of these train ids.")
    public StationQuery withTrains(Set<UUID> trains) {
        Objects.requireNonNull(trains);
        return new StationQuery(filter, stationTags, trains, lines, categories, hideBlacklisted);
    }

    /** Keeps only stations served by one of the given lines. */
    @QueryParam(value = "lines")
    @OpenApiDescription("Keep only stations served by one of these line ids.")
    public StationQuery withLines(Set<UUID> lines) {
        Objects.requireNonNull(lines);
        return new StationQuery(filter, stationTags, trains, lines, categories, hideBlacklisted);
    }

    /** Keeps only stations served by one of the given categories. */
    @QueryParam(value = "categories")
    @OpenApiDescription("Keep only stations served by one of these category ids.")
    public StationQuery withCategories(Set<UUID> categories) {
        Objects.requireNonNull(categories);
        return new StationQuery(filter, stationTags, trains, lines, categories, hideBlacklisted);
    }

    /** Drops stations hidden from public display when set to {@code true}. */
    @QueryParam(value = "hide_blacklisted")
    @OpenApiDescription("Drop stations hidden from public display when set to true.")
    public StationQuery withHideBlacklisted(boolean hideBlacklisted) {
        return new StationQuery(filter, stationTags, trains, lines, categories, hideBlacklisted);
    }

    /** Whether the given station passes this query. */
    public boolean accept(StationSnapshot station) {
        return (filter.isEmpty() || TrainUtils.stationMatches(station.name(), filter)) &&
                ModUtils.listContainsAny(stationTags, station.tags(), (id, v) -> v.id().equals(id)) &&
                ModUtils.listContainsAny(trains, station.trainIds(), (id, v) -> v.equals(id)) &&
                ModUtils.listContainsAny(lines, station.lines(), (id, v) -> v.id().equals(id)) &&
                ModUtils.listContainsAny(categories, station.categories(), (id, v) -> v.id().equals(id)) &&
                (!hideBlacklisted || !station.blacklisted());
    }
}
