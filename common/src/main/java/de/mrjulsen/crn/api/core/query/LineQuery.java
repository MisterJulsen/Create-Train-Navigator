package de.mrjulsen.crn.api.core.query;

import de.mrjulsen.crn.api.core.snapshot.LineSnapshot;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.crn.util.TrainUtils;
import de.mrjulsen.crn.web.annotation.QueryModel;
import de.mrjulsen.crn.web.annotation.QueryParam;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@QueryModel
public record LineQuery(
        Set<UUID> trains,
        Set<UUID> stationTags,
        String stationFilter
) {

    public static LineQuery all() {
        return new LineQuery(Set.of(), Set.of(), "");
    }

    @QueryParam(value = "trains")
    public LineQuery withTrains(Set<UUID> trains) {
        return new LineQuery(trains, stationTags, stationFilter);
    }

    @QueryParam(value = "station_tags")
    public LineQuery withStationTags(Set<UUID> stationTags) {
        return new LineQuery(trains, stationTags, stationFilter);
    }

    @QueryParam(value = "stations")
    public LineQuery withStationFilter(String stationFilter) {
        Objects.requireNonNull(stationFilter);
        return new LineQuery(trains, stationTags, stationFilter);
    }


    public boolean accept(LineSnapshot line) {
        return (stationFilter.isEmpty() || line.stations().stream().anyMatch(station -> TrainUtils.stationMatches(station.name(), stationFilter))) &&
                ModUtils.listContains(stationTags, line.stations(), (id, v) -> v.tagId().equals(id)) &&
                ModUtils.listContains(trains, line.trainIds(), (id, v) -> v.equals(id));
    }
}
