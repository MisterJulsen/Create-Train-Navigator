package de.mrjulsen.crn.backend.api;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import de.mrjulsen.crn.data.TrainLine;

/**
 * An immutable snapshot of a train line: the configured line plus which trains and stations it
 * currently covers.
 *
 * @param line          The train line itself.
 * @param trainIds      The trains currently operating on this line.
 * @param stations      The stations this line currently calls at, in no particular order.
 * @param delayedTrains How many of its trains are currently late.
 */
public record LineSnapshot(
    TrainLine line,
    Set<UUID> trainIds,
    List<StationRef> stations,
    int delayedTrains
) {

    public LineSnapshot {
        trainIds = trainIds == null ? Set.of() : Set.copyOf(trainIds);
        stations = stations == null ? List.of() : List.copyOf(stations);
    }

    /** The line's id. */
    public UUID id() {
        return line.getId();
    }

    /** The line's name. */
    public String name() {
        return line.getLineName();
    }

    /** How many trains currently operate on this line. */
    public int trainCount() {
        return trainIds.size();
    }

    /** Whether any train currently operates on this line. */
    public boolean isOperating() {
        return !trainIds.isEmpty();
    }

    /** Whether any train on this line is currently late. */
    public boolean hasDelays() {
        return delayedTrains > 0;
    }
}
