package de.mrjulsen.crn.api.core.query;

import de.mrjulsen.crn.api.core.snapshot.StationSnapshot;
import de.mrjulsen.crn.util.TrainUtils;
import de.mrjulsen.crn.web.annotation.RestQueryModel;
import de.mrjulsen.crn.web.annotation.RestQueryParam;

import java.util.*;
import java.util.function.BiPredicate;

@RestQueryModel
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

    @RestQueryParam(value = "filter")
    public StationQuery withFilter(String filter) {
        Objects.requireNonNull(filter);
        return new StationQuery(filter, stationTags, trains, lines, categories, hideBlacklisted);
    }

    @RestQueryParam(value = "station_tags")
    public StationQuery withTags(Set<UUID> stationTags) {
        Objects.requireNonNull(stationTags);
        return new StationQuery(filter, stationTags, trains, lines, categories, hideBlacklisted);
    }

    @RestQueryParam(value = "trains")
    public StationQuery withTrains(Set<UUID> trains) {
        Objects.requireNonNull(trains);
        return new StationQuery(filter, stationTags, trains, lines, categories, hideBlacklisted);
    }

    @RestQueryParam(value = "lines")
    public StationQuery withLines(Set<UUID> lines) {
        Objects.requireNonNull(lines);
        return new StationQuery(filter, stationTags, trains, lines, categories, hideBlacklisted);
    }

    @RestQueryParam(value = "categories")
    public StationQuery withCategories(Set<UUID> categories) {
        Objects.requireNonNull(categories);
        return new StationQuery(filter, stationTags, trains, lines, categories, hideBlacklisted);
    }

    @RestQueryParam(value = "hide_blacklisted")
    public StationQuery withHideBlacklisted(boolean hideBlacklisted) {
        return new StationQuery(filter, stationTags, trains, lines, categories, hideBlacklisted);
    }

    public boolean accept(StationSnapshot station) {
        return (filter.isEmpty() || TrainUtils.stationMatches(station.name(), filter)) &&
                contains(stationTags, station.tags(), (id, v) -> v.id().equals(id)) &&
                contains(trains, station.trainIds(), (id, v) -> v.equals(id)) &&
                contains(lines, station.lines(), (id, v) -> v.id().equals(id)) &&
                contains(categories, station.categories(), (id, v) -> v.id().equals(id)) &&
                (!hideBlacklisted || !station.blacklisted());
    }

    private <T, S> boolean contains(Collection<T> searchFor, Collection<S> searchIn, BiPredicate<T, S> test) {
        if (searchFor.isEmpty()) {
            return true;
        }

        for (S s : searchIn) {
            for (T t : searchFor) {
                if (test.test(t, s)) {
                    return true;
                }
            }
        }
        return false;
    }
}
