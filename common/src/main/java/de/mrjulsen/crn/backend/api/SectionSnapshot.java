package de.mrjulsen.crn.backend.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.mrjulsen.crn.backend.core.TrackedTrain;
import de.mrjulsen.crn.backend.schedule.JourneySection;
import de.mrjulsen.crn.backend.schedule.JourneyStop;
import de.mrjulsen.crn.data.TrainCategory;
import de.mrjulsen.crn.data.TrainLine;

/**
 * An immutable snapshot of one section of a train's journey: the stretch of the run that carries a
 * particular line and category, with its own stops and its own timetable.
 * <p>
 * A journey without any section instructions still has exactly one, covering the whole run.
 *
 * @param index          The position of this section in travel order, counting from zero.
 * @param entryIndex     The index of the instruction this section was created from.
 * @param line           The assigned train line, or {@code null} if it carries none.
 * @param category       The assigned train category, or {@code null} if it carries none.
 * @param origin         The station this section starts from, or {@link StationRef#NONE} if it has
 *                       no stops.
 * @param destination    The terminus advertised for this section, or {@link StationRef#NONE}.
 * @param stops          This section's stops in travel order.
 * @param current        Whether the train is operating in this section right now.
 * @param usable         Whether this section may be used for travel.
 * @param defaultSection Whether this is the implicit section of a schedule without section
 *                       instructions.
 * @param includesNextSectionStart Whether the following section's first stop still counts as part
 *                       of this one.
 * @param scheduledDuration How long this section takes from its first arrival to its last
 *                       departure, in ticks, or {@code -1} if its times are not known yet.
 */
public record SectionSnapshot(
    int index,
    int entryIndex,
    TrainLine line,
    TrainCategory category,
    StationRef origin,
    StationRef destination,
    List<StopSnapshot> stops,
    boolean current,
    boolean usable,
    boolean defaultSection,
    boolean includesNextSectionStart,
    long scheduledDuration
) {

    public SectionSnapshot {
        stops = stops == null ? List.of() : List.copyOf(stops);
        origin = origin == null ? StationRef.NONE : origin;
        destination = destination == null ? StationRef.NONE : destination;
    }

    /** Captures the given section of the given train. */
    public static SectionSnapshot of(TrackedTrain train, JourneySection section, JourneyStop currentStop) {
        List<StopSnapshot> stops = new ArrayList<>(section.getStops().size());
        boolean current = false;
        for (JourneyStop stop : section.getStops()) {
            if (!JourneySnapshot.isPublic(stop, currentStop)) {
                continue;
            }
            StopVisitState state = JourneySnapshot.visitStateOf(stop, currentStop);
            current |= state == StopVisitState.CURRENT;
            stops.add(StopSnapshot.of(train, stop, state));
        }

        return new SectionSnapshot(
            section.getSectionIndex(),
            section.entryIndex(),
            section.getTrainLine().orElse(null),
            section.getTrainCategory().orElse(null),
            section.getFirstStop().map(x -> StationRef.of(train.getDisplayStationName(x))).orElse(StationRef.NONE),
            section.getLastStop().map(x -> StationRef.of(train.getSectionDestination(x))).orElse(StationRef.NONE),
            stops,
            current,
            section.isUsable(),
            section.isDefault(),
            section.includesNextSectionStart(),
            computeDuration(stops)
        );
    }

    private static long computeDuration(List<StopSnapshot> stops) {
        if (stops.isEmpty()) {
            return -1;
        }
        StopSnapshot first = stops.get(0);
        StopSnapshot last = stops.get(stops.size() - 1);
        if (!first.scheduled().isKnown() || !last.scheduled().isKnown()) {
            return -1;
        }
        return Math.max(0, last.scheduled().departure() - first.scheduled().arrival());
    }

    /** The train line this section runs on, if it carries one. */
    public Optional<TrainLine> trainLine() {
        return Optional.ofNullable(line);
    }

    /** The train category this section carries, if any. */
    public Optional<TrainCategory> trainCategory() {
        return Optional.ofNullable(category);
    }

    /** Whether this section carries a train line at all. */
    public boolean hasLine() {
        return line != null;
    }

    /** Whether this section carries a train category at all. */
    public boolean hasCategory() {
        return category != null;
    }

    /** The stops the train still has to serve in this section, in travel order. */
    public List<StopSnapshot> pendingStops() {
        return stops.stream().filter(x -> x.visitState().isPending()).toList();
    }

    /** The stops the train has already served in this section, in travel order. */
    public List<StopSnapshot> passedStops() {
        return stops.stream().filter(x -> x.visitState() == StopVisitState.PASSED).toList();
    }

    /** The number of stops in this section. */
    public int stopCount() {
        return stops.size();
    }
}
