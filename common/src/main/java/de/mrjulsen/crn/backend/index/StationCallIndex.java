package de.mrjulsen.crn.backend.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.mrjulsen.crn.backend.core.TrackedTrain;
import de.mrjulsen.crn.backend.schedule.JourneyStop;
import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.train.TrainUtils;

/**
 * Which trains call at which station, indexed by station name.
 * <p>
 * Answering that by scanning every stop of every train is what a departure board used to do, once
 * per query. On a network with a thousand trains that is tens of thousands of comparisons for a
 * single display, several times a second. This index turns the common case - a board for one
 * concrete station - into a map lookup, and the uncommon ones into a scan over station names rather
 * than over trains.
 * <p>
 * Rebuilt once per full update from the tracked trains, so it is at most one update cycle behind.
 * That is deliberate: a train discovered in between has no timings yet and could not be shown
 * anyway, and the boards it feeds are refreshed on the same cycle.
 *
 * <h2>Threading</h2>
 * Rebuilt on the server thread, read from any thread. The map is published as a whole through a
 * volatile field, so a reader always sees one complete generation of it.
 */
public final class StationCallIndex {

    /** One train calling at one station. */
    public record Call(TrackedTrain train, JourneyStop stop) {}

    private volatile Map<String, List<Call>> byStation = Map.of();

    /**
     * Rebuilds the index from the given trains.
     * <p>
     * <b>Server thread only</b>, since it reads each train's journey while it may be re-parsed.
     */
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

    /**
     * Every call at a station.
     *
     * @param stationNameOrFilter The exact station name, or a filter containing {@code *} wildcards.
     */
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

    /** Every call at any station carried by the given tag. */
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

    /** Drops the whole index. */
    public void clear() {
        this.byStation = Map.of();
    }

    /** How many distinct stations are currently indexed. */
    public int getStationCount() {
        return byStation.size();
    }
}
