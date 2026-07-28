package de.mrjulsen.crn.core.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.mrjulsen.crn.core.train.TrackedTrain;
import de.mrjulsen.crn.core.schedule.JourneyStop;
import de.mrjulsen.crn.data.settings.StationTag;
import de.mrjulsen.crn.util.TrainUtils;


public final class StationCallIndex {

    public record Call(TrackedTrain train, JourneyStop stop) {}

    private volatile Map<String, List<Call>> byStation = Map.of();
    private volatile Map<String, List<Call>> byScheduledStation = Map.of();

    public void rebuild(Collection<TrackedTrain> trains) {
        Map<String, List<Call>> rebuilt = new HashMap<>();
        Map<String, List<Call>> rebuiltScheduled = new HashMap<>();
        for (TrackedTrain train : trains) {
            for (JourneyStop stop : train.getJourney().getStops()) {
                Call call = new Call(train, stop);
                String station = train.getDisplayStationName(stop);
                String scheduled = train.getScheduledStationName(stop);
                add(rebuilt, station, call);
                if (!scheduled.equals(station)) {
                    add(rebuiltScheduled, scheduled, call);
                }
            }
        }
        this.byStation = rebuilt;
        this.byScheduledStation = rebuiltScheduled;
    }

    private static void add(Map<String, List<Call>> target, String station, Call call) {
        if (station == null || station.isBlank()) {
            return;
        }
        target.computeIfAbsent(station, x -> new ArrayList<>()).add(call);
    }

    public List<Call> callsAt(String stationNameOrFilter, boolean includeDivertedAway) {
        if (stationNameOrFilter == null || stationNameOrFilter.isBlank()) {
            return List.of();
        }

        List<Call> calls = matching(byStation, stationNameOrFilter);
        if (!includeDivertedAway) {
            return calls;
        }
        return merge(calls, matching(byScheduledStation, stationNameOrFilter));
    }

    private static List<Call> matching(Map<String, List<Call>> index, String stationNameOrFilter) {
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

    public List<Call> callsAt(StationTag tag, boolean includeDivertedAway) {
        if (tag == null) {
            return List.of();
        }

        List<Call> calls = tagged(byStation, tag);
        if (!includeDivertedAway) {
            return calls;
        }
        return merge(calls, tagged(byScheduledStation, tag));
    }

    private static List<Call> tagged(Map<String, List<Call>> index, StationTag tag) {
        List<Call> matched = new ArrayList<>();
        for (String station : tag.getAllStationNames()) {
            List<Call> calls = index.get(station);
            if (calls != null) {
                matched.addAll(calls);
            }
        }
        return matched;
    }

    private static List<Call> merge(List<Call> calls, List<Call> diverted) {
        if (diverted.isEmpty()) {
            return calls;
        }
        if (calls.isEmpty()) {
            return diverted;
        }
        Set<Call> merged = new LinkedHashSet<>(calls);
        merged.addAll(diverted);
        return List.copyOf(merged);
    }

    public void clear() {
        this.byStation = Map.of();
        this.byScheduledStation = Map.of();
    }

    public int getStationCount() {
        return byStation.size();
    }
}
