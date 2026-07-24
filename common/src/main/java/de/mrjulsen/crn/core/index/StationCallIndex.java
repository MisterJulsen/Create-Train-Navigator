package de.mrjulsen.crn.core.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.mrjulsen.crn.core.train.TrackedTrain;
import de.mrjulsen.crn.core.schedule.JourneyStop;
import de.mrjulsen.crn.data.settings.StationTag;
import de.mrjulsen.crn.util.TrainUtils;

public final class StationCallIndex {

    public record Call(TrackedTrain train, JourneyStop stop) {}

    private volatile Map<String, List<Call>> byStation = Map.of();

    public void rebuild(Collection<TrackedTrain> trains) {
        Map<String, List<Call>> rebuilt = new HashMap<>();
        for (TrackedTrain train : trains) {
            for (JourneyStop stop : train.getJourney().getStops()) {
                String station = stop.getStationName();
                if (station == null || station.isBlank()) {
                    continue;
                }
                rebuilt.computeIfAbsent(station, x -> new ArrayList<>()).add(new Call(train, stop));
            }
        }
        this.byStation = rebuilt;
    }

    public List<Call> callsAt(String stationNameOrFilter) {
        if (stationNameOrFilter == null || stationNameOrFilter.isBlank()) {
            return List.of();
        }

        Map<String, List<Call>> index = byStation;

        List<Call> exact = index.get(stationNameOrFilter);
        if (exact != null) {
            return exact;
        }

        List<Call> matched = null;
        for (Map.Entry<String, List<Call>> entry : index.entrySet()) {
            if (!TrainUtils.stationMatches(entry.getKey(), stationNameOrFilter)) {
                continue;
            }
            if (matched == null) {
                matched = new ArrayList<>(entry.getValue());
            } else {
                matched.addAll(entry.getValue());
            }
        }
        return matched == null ? List.of() : matched;
    }

    public List<Call> callsAt(StationTag tag) {
        if (tag == null) {
            return List.of();
        }

        Map<String, List<Call>> index = byStation;
        List<Call> matched = new ArrayList<>();
        for (String station : tag.getAllStationNames()) {
            List<Call> calls = index.get(station);
            if (calls != null) {
                matched.addAll(calls);
            }
        }
        return matched;
    }

    public void clear() {
        this.byStation = Map.of();
    }

    public int getStationCount() {
        return byStation.size();
    }
}
