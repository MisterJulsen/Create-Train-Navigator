package de.mrjulsen.crn.api.core.query;

import de.mrjulsen.crn.api.core.snapshot.StationSnapshot;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.crn.util.TrainUtils;
import de.mrjulsen.crn.web.annotation.QueryModel;
import de.mrjulsen.crn.web.annotation.QueryParam;

import java.util.*;

@QueryModel
public record StationQuery(
        String filter,
        Set<UUID> stationTags,
        Set<UUID> trains,
        Set<UUID> lines,
        Set<UUID> categories,
        boolean hideBlacklisted
) {

    public static StationQuery all() {
        return new StationQuery("", Set.of(), Set.of(), Set.of(), Set.of(), false);
    }

    @QueryParam(value = "filter")
    public StationQuery withFilter(String filter) {
        Objects.requireNonNull(filter);
        return new StationQuery(filter, stationTags, trains, lines, categories, hideBlacklisted);
    }

    @QueryParam(value = "station_tags")
    public StationQuery withTags(Set<UUID> stationTags) {
        Objects.requireNonNull(stationTags);
        return new StationQuery(filter, stationTags, trains, lines, categories, hideBlacklisted);
    }

    @QueryParam(value = "trains")
    public StationQuery withTrains(Set<UUID> trains) {
        Objects.requireNonNull(trains);
        return new StationQuery(filter, stationTags, trains, lines, categories, hideBlacklisted);
    }

    @QueryParam(value = "lines")
    public StationQuery withLines(Set<UUID> lines) {
        Objects.requireNonNull(lines);
        return new StationQuery(filter, stationTags, trains, lines, categories, hideBlacklisted);
    }

    @QueryParam(value = "categories")
    public StationQuery withCategories(Set<UUID> categories) {
        Objects.requireNonNull(categories);
        return new StationQuery(filter, stationTags, trains, lines, categories, hideBlacklisted);
    }

    @QueryParam(value = "hide_blacklisted")
    public StationQuery withHideBlacklisted(boolean hideBlacklisted) {
        return new StationQuery(filter, stationTags, trains, lines, categories, hideBlacklisted);
    }

    public boolean accept(StationSnapshot station) {
        return (filter.isEmpty() || TrainUtils.stationMatches(station.name(), filter)) &&
                ModUtils.listContains(stationTags, station.tags(), (id, v) -> v.id().equals(id)) &&
                ModUtils.listContains(trains, station.trainIds(), (id, v) -> v.equals(id)) &&
                ModUtils.listContains(lines, station.lines(), (id, v) -> v.id().equals(id)) &&
                ModUtils.listContains(categories, station.categories(), (id, v) -> v.id().equals(id)) &&
                (!hideBlacklisted || !station.blacklisted());
    }
}
