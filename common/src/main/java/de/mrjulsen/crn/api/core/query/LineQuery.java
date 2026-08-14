package de.mrjulsen.crn.api.core.query;

import de.mrjulsen.crn.api.core.snapshot.LineSnapshot;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.crn.util.TrainUtils;
import de.mrjulsen.crn.web.annotation.QueryModel;
import de.mrjulsen.crn.web.annotation.QueryParam;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A filter over the configured lines. Each field that is set narrows the result, and a field left
 * empty imposes no constraint. Build one from {@link #all()} and the {@code with...} methods;
 * instances are immutable, so each method returns a new query.
 *
 * @param trains        If non-empty, keeps only lines worked by one of these trains.
 * @param stationTags   If non-empty, keeps only lines calling at a station in one of these tags.
 * @param stationFilter If non-empty, keeps only lines calling at a station whose name matches this
 *                      filter, which may use the schedule's wildcard syntax.
 */
@QueryModel
public record LineQuery(
        Set<UUID> trains,
        Set<UUID> stationTags,
        String stationFilter
) {

    /** A query that keeps every line, as a starting point for refinement. */
    public static LineQuery all() {
        return new LineQuery(Set.of(), Set.of(), "");
    }

    /** Keeps only lines worked by one of the given trains. */
    @QueryParam(value = "trains")
    public LineQuery withTrains(Set<UUID> trains) {
        return new LineQuery(trains, stationTags, stationFilter);
    }

    /** Keeps only lines calling at a station in one of the given tags. */
    @QueryParam(value = "station_tags")
    public LineQuery withStationTags(Set<UUID> stationTags) {
        return new LineQuery(trains, stationTags, stationFilter);
    }

    /** Keeps only lines calling at a station whose name matches the given filter. */
    @QueryParam(value = "stations")
    public LineQuery withStationFilter(String stationFilter) {
        Objects.requireNonNull(stationFilter);
        return new LineQuery(trains, stationTags, stationFilter);
    }

    /** Whether the given line passes this query. */
    public boolean accept(LineSnapshot line) {
        return (stationFilter.isEmpty() || line.stations().stream().anyMatch(station -> TrainUtils.stationMatches(station.name(), stationFilter))) &&
                ModUtils.listContains(stationTags, line.stations(), (id, v) -> v.tagId().equals(id)) &&
                ModUtils.listContains(trains, line.trainIds(), (id, v) -> v.equals(id));
    }
}
