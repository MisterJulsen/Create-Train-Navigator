package de.mrjulsen.crn.backend.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import de.mrjulsen.crn.backend.core.TrackedTrain;
import de.mrjulsen.crn.backend.schedule.JourneySection;
import de.mrjulsen.crn.backend.schedule.JourneyStop;
import de.mrjulsen.crn.backend.schedule.TrainJourney;
import de.mrjulsen.crn.data.storage.GlobalSettings;

/**
 * An immutable snapshot of a train's whole journey: every stop of the run with its timetable and
 * projected times, grouped into sections, and marked with what the train has already done and what
 * is still to come.
 * <p>
 * Taken while no backend update is in progress, so every stop belongs to the same projection pass -
 * unlike values read one by one through the individual queries.
 *
 * @param trainId         The id of the train this journey belongs to.
 * @param trainName       The train's own name.
 * @param displayName     The line name if one is assigned, otherwise the train name.
 * @param cyclic          Whether the train repeats this journey instead of stopping at the end.
 * @param totalDuration   How long one full cycle takes in ticks, or {@code -1} while learning.
 * @param stops           Every stop in travel order, starting from the schedule's first.
 * @param sections        Every section in travel order.
 * @param currentStopIndex The position of the current stop within {@link #stops()}, or {@code -1}.
 * @param currentSectionIndex The position of the current section within {@link #sections()}, or
 *                        {@code -1}.
 */
public record JourneySnapshot(
    UUID trainId,
    String trainName,
    String displayName,
    boolean cyclic,
    long totalDuration,
    List<StopSnapshot> stops,
    List<SectionSnapshot> sections,
    int currentStopIndex,
    int currentSectionIndex
) {

    public JourneySnapshot {
        stops = stops == null ? List.of() : List.copyOf(stops);
        sections = sections == null ? List.of() : List.copyOf(sections);
    }

    /** Captures the current journey of the given train. */
    public static JourneySnapshot of(TrackedTrain train) {
        return train.readCoherently(() -> {
            TrainJourney journey = train.getJourney();
            JourneyStop currentStop = train.getCurrentStop().orElse(null);
            JourneySection currentSection = train.getCurrentSection().orElse(null);

            List<StopSnapshot> stops = new ArrayList<>(journey.getStopCount());
            int currentStopIndex = -1;
            for (JourneyStop stop : journey.getStops()) {
                if (!isPublic(stop, currentStop)) {
                    continue;
                }
                if (stop == currentStop) {
                    currentStopIndex = stops.size();
                }
                stops.add(StopSnapshot.of(train, stop, visitStateOf(stop, currentStop)));
            }

            List<SectionSnapshot> sections = new ArrayList<>(journey.getSections().size());
            for (JourneySection section : journey.getSections()) {
                sections.add(SectionSnapshot.of(train, section, currentStop));
            }

            return new JourneySnapshot(
                train.getTrainId(),
                train.getTrainName(),
                train.getDisplayName(),
                journey.isCyclic(),
                train.getTotalDuration(),
                stops,
                sections,
                currentStopIndex,
                currentSection == null ? -1 : currentSection.getSectionIndex()
            );
        });
    }

    /**
     * Whether a stop may be shown at all. A stop at a blacklisted station is left out entirely, the
     * same way a board omits it - the point of the blacklist is that the station does not appear.
     * <p>
     * The train's current stop is the one exception: it is where the train actually is, so hiding it
     * would leave the journey unable to say where the train has got to. It stays, and only the
     * stations around it disappear.
     */
    static boolean isPublic(JourneyStop stop, JourneyStop currentStop) {
        return stop == currentStop || !GlobalSettings.getInstance().isStationBlacklisted(stop.getStationName());
    }

    /**
     * Where a stop lies relative to the train's position. Stops before the current one on this run
     * count as passed; on a cyclic journey that means "earlier in the current cycle".
     */
    static StopVisitState visitStateOf(JourneyStop stop, JourneyStop currentStop) {
        if (currentStop == null) {
            return StopVisitState.UPCOMING;
        }
        if (stop.getOrderIndex() == currentStop.getOrderIndex()) {
            return StopVisitState.CURRENT;
        }
        return stop.getOrderIndex() < currentStop.getOrderIndex() ? StopVisitState.PASSED : StopVisitState.UPCOMING;
    }

    /** The stop the train is at or traveling towards. */
    public Optional<StopSnapshot> currentStop() {
        return currentStopIndex < 0 || currentStopIndex >= stops.size()
            ? Optional.empty()
            : Optional.of(stops.get(currentStopIndex));
    }

    /** The section the train is operating in. */
    public Optional<SectionSnapshot> currentSection() {
        return sections.stream().filter(SectionSnapshot::current).findFirst();
    }

    /**
     * Every stop the train still has to serve, in the order it will reach them. On a cyclic journey
     * the list wraps around, so it always covers a full cycle from the current stop.
     */
    public List<StopSnapshot> upcomingStops() {
        if (stops.isEmpty() || currentStopIndex < 0) {
            return List.of();
        }
        List<StopSnapshot> upcoming = new ArrayList<>(stops.size());
        for (int i = 0; i < stops.size(); i++) {
            int index = currentStopIndex + i;
            if (index >= stops.size()) {
                if (!cyclic) {
                    break;
                }
                index %= stops.size();
            }
            upcoming.add(stops.get(index));
        }
        return upcoming;
    }

    /** Every stop the train has already served on this run, in travel order. */
    public List<StopSnapshot> passedStops() {
        return stops.stream().filter(x -> x.visitState() == StopVisitState.PASSED).toList();
    }

    /** The stop after the current one, i.e. the train's next call. */
    public Optional<StopSnapshot> nextStop() {
        List<StopSnapshot> upcoming = upcomingStops();
        return upcoming.size() < 2 ? Optional.empty() : Optional.of(upcoming.get(1));
    }

    /** The final stop of the run, which is the current cycle's terminus. */
    public Optional<StopSnapshot> lastStop() {
        return stops.isEmpty() ? Optional.empty() : Optional.of(stops.get(stops.size() - 1));
    }

    /** The section at the given position in travel order. */
    public Optional<SectionSnapshot> section(int index) {
        return index < 0 || index >= sections.size() ? Optional.empty() : Optional.of(sections.get(index));
    }

    /**
     * How far through the run the train is, from {@code 0} at the first stop to {@code 1} at the
     * last. Measured in stops rather than distance, and {@code 0} while the position is unknown.
     */
    public double progress() {
        if (stops.size() < 2 || currentStopIndex < 0) {
            return 0;
        }
        return (double) currentStopIndex / (stops.size() - 1);
    }

    /** Whether this journey has any stops at all. */
    public boolean isEmpty() {
        return stops.isEmpty();
    }

    /** Whether this journey repeats, and therefore has occurrences beyond the current run. */
    public boolean repeats() {
        return cyclic && totalDuration > 0;
    }

    /**
     * When this train next calls at a station at or after the given time, projecting into later
     * journey cycles if the current run has already passed it.
     * <p>
     * This is what a route search asks: not "when does this train reach the station on the run it is
     * on" but "when could a traveller standing there at this time board it". On a non-cyclic journey
     * only the current run exists, so a call already passed yields nothing.
     *
     * @param stationNameOrFilter The exact station name, or a filter containing {@code *} wildcards.
     * @param notBefore           The earliest acceptable arrival, in transformed game ticks.
     */
    public Optional<StopSnapshot> nextCallAt(String stationNameOrFilter, long notBefore) {
        StopSnapshot best = null;
        for (StopSnapshot stop : stops) {
            if (!stop.station().matches(stationNameOrFilter) || !stop.hasTimes()) {
                continue;
            }
            StopSnapshot candidate = repeats() ? stop.atOrAfter(notBefore, totalDuration) : stop;
            if (candidate.realtime().arrival() < notBefore) {
                continue;
            }
            if (best == null || candidate.realtime().arrival() < best.realtime().arrival()) {
                best = candidate;
            }
        }
        return Optional.ofNullable(best);
    }

    /**
     * This journey as the train will run it {@code cycles} cycles from now, with every stop's times
     * moved along. Returns this journey unchanged if it does not repeat.
     *
     * @see de.mrjulsen.crn.backend.timing.CycleProjector
     */
    public JourneySnapshot advancedBy(int cycles) {
        if (cycles <= 0 || !repeats()) {
            return this;
        }
        List<StopSnapshot> advanced = new ArrayList<>(stops.size());
        for (StopSnapshot stop : stops) {
            advanced.add(stop.advancedBy(cycles, totalDuration));
        }
        return new JourneySnapshot(trainId, trainName, displayName, cyclic, totalDuration,
            advanced, sections, currentStopIndex, currentSectionIndex);
    }
}
