package de.mrjulsen.crn.backend.api;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.TrainCategory;
import de.mrjulsen.crn.data.TrainLine;

/**
 * An immutable snapshot of a station: what it is called, how it is grouped and which services call
 * there.
 *
 * @param station     The station itself, carrying its primary tag and platform.
 * @param tags        Every station tag this station belongs to. A station usually has one, but
 *                    nothing stops it from being grouped several ways.
 * @param lines       The train lines currently calling at this station.
 * @param categories  The train categories currently calling at this station.
 * @param trainIds    The trains currently scheduled to call at this station.
 * @param blacklisted Whether this station is hidden from public displays.
 */
public record StationSnapshot(
    StationRef station,
    List<StationTag> tags,
    List<TrainLine> lines,
    List<TrainCategory> categories,
    Set<UUID> trainIds,
    boolean blacklisted
) {

    public StationSnapshot {
        station = station == null ? StationRef.NONE : station;
        tags = tags == null ? List.of() : List.copyOf(tags);
        lines = lines == null ? List.of() : List.copyOf(lines);
        categories = categories == null ? List.of() : List.copyOf(categories);
        trainIds = trainIds == null ? Set.of() : Set.copyOf(trainIds);
    }

    /** The station's name as it exists in the track network. */
    public String name() {
        return station.name();
    }

    /** Whether any train currently calls at this station. */
    public boolean isServed() {
        return !trainIds.isEmpty();
    }

    /** Whether this station belongs to any tag. */
    public boolean isTagged() {
        return !tags.isEmpty();
    }
}
