package de.mrjulsen.crn.backend.schedule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;

import com.simibubi.create.content.trains.schedule.Schedule;

/**
 * An immutable, parsed snapshot of a train's schedule: all stops in travel order, grouped into
 * sections, plus metadata such as timing reset markers. Rebuilt whenever the schedule changes.
 * <p>
 * Everything is built once in the constructor and never mutated, so the collections are handed out
 * directly rather than copied: journeys are read constantly, and defensive copies on those paths
 * would cost far more than they protect.
 */
public final class TrainJourney {

    private final UUID trainId;
    private final transient Schedule schedule;

    /** All stops in travel order. Immutable. */
    private final List<JourneyStop> stops;
    /** All stops by their schedule entry index. Immutable. */
    private final Map<Integer, JourneyStop> stopsByEntryIndex;
    /** All sections in travel order. Immutable. */
    private final List<JourneySection> sections;

    private final Set<Integer> resetTimingEntries;
    private final boolean cyclic;
    private final boolean flexibleDwellTimes;

    /**
     * For every schedule entry index, the position in {@link #stops} of the first stop at or after
     * it, wrapping around to the first stop. Lets {@link #getCurrentStop(int)} answer in constant
     * time instead of scanning.
     */
    private final int[] stopAtOrAfterEntry;

    TrainJourney(UUID trainId, Schedule schedule, List<JourneyStop> stops, List<JourneySection> sections, Set<Integer> resetTimingEntries, boolean cyclic, boolean flexibleDwellTimes) {
        this.trainId = trainId;
        this.schedule = schedule;
        this.resetTimingEntries = Set.copyOf(resetTimingEntries);
        this.cyclic = cyclic;
        this.flexibleDwellTimes = flexibleDwellTimes;

        this.stops = List.copyOf(stops);
        this.sections = List.copyOf(sections);

        Map<Integer, JourneyStop> byEntry = new HashMap<>(this.stops.size());
        for (JourneyStop stop : this.stops) {
            byEntry.put(stop.entryIndex(), stop);
        }
        this.stopsByEntryIndex = Map.copyOf(byEntry);
        this.stopAtOrAfterEntry = buildStopAtOrAfterEntry(schedule, this.stops);
    }

    /**
     * Builds the entry-to-stop table by walking the schedule backwards, so every entry inherits the
     * next stop that follows it. Entries after the last stop wrap around to the first, matching the
     * cyclic execution of a schedule.
     */
    private static int[] buildStopAtOrAfterEntry(Schedule schedule, List<JourneyStop> stops) {
        int entryCount = schedule == null || schedule.entries == null ? 0 : schedule.entries.size();
        if (entryCount <= 0 || stops.isEmpty()) {
            return new int[0];
        }

        int[] stopAt = new int[entryCount];
        java.util.Arrays.fill(stopAt, -1);
        for (int i = 0; i < stops.size(); i++) {
            int entry = stops.get(i).entryIndex();
            if (entry >= 0 && entry < entryCount) {
                stopAt[entry] = i;
            }
        }

        int[] table = new int[entryCount];
        int next = 0;
        for (int entry = entryCount - 1; entry >= 0; entry--) {
            if (stopAt[entry] >= 0) {
                next = stopAt[entry];
            }
            table[entry] = next;
        }
        return table;
    }

    /** A journey without stops, for a train that has no schedule. */
    public static TrainJourney empty(UUID trainId) {
        return new TrainJourney(trainId, null, List.of(), List.of(), Set.of(), false, false);
    }

    /** The id of the train this journey belongs to. */
    public UUID getTrainId() {
        return trainId;
    }

    /** The schedule this journey was parsed from. Used to detect schedule changes. */
    public Schedule getSchedule() {
        return schedule;
    }

    /** Whether this journey has no stops, e.g. because the train has no schedule. */
    public boolean isEmpty() {
        return stops.isEmpty();
    }

    /** Whether the train repeats this journey instead of stopping at the last entry. */
    public boolean isCyclic() {
        return cyclic;
    }

    /**
     * Whether the schedule contains wait conditions that let the train shorten a stop to catch up.
     * A train without them can never recover from a delay, so its timetable is reset at every
     * section change instead.
     */
    public boolean hasFlexibleDwellTimes() {
        return flexibleDwellTimes;
    }

    /** All stops in travel order. */
    public List<JourneyStop> getStops() {
        return stops;
    }

    /** The number of stops in this journey. */
    public int getStopCount() {
        return stops.size();
    }

    /** All sections in travel order. Contains at least one (default) section if the journey is not empty. */
    public List<JourneySection> getSections() {
        return sections;
    }

    /** The schedule entry indices at which the timings are to be reset. */
    public Set<Integer> getResetTimingEntries() {
        return resetTimingEntries;
    }

    /** The stop at the given schedule entry index, if that entry is a stop. */
    public Optional<JourneyStop> getStopAtEntry(int entryIndex) {
        return Optional.ofNullable(stopsByEntryIndex.get(entryIndex));
    }

    /**
     * The stop the train is currently traveling to or waiting at: the first stop at or after the
     * given schedule entry index, wrapping around for cyclic schedules.
     * <p>
     * Answered from a precomputed table rather than by scanning the stops. This is one of the most
     * frequently asked questions in the whole backend - assembling a single train snapshot asks it
     * several times, and a board does so for every train - so the scan showed up directly in the
     * cost of every query.
     */
    public Optional<JourneyStop> getCurrentStop(int currentEntryIndex) {
        if (stops.isEmpty()) {
            return Optional.empty();
        }
        if (currentEntryIndex < 0 || currentEntryIndex >= stopAtOrAfterEntry.length) {
            return Optional.of(stops.get(0));
        }
        return Optional.of(stops.get(stopAtOrAfterEntry[currentEntryIndex]));
    }

    /** The stop following the given one in travel order, wrapping around for cyclic schedules. */
    public Optional<JourneyStop> getNextStop(JourneyStop stop) {
        int next = stop.getOrderIndex() + 1;
        if (next >= stops.size()) {
            return cyclic ? Optional.of(stops.get(0)) : Optional.empty();
        }
        return Optional.of(stops.get(next));
    }

    /** The stop preceding the given one in travel order, wrapping around for cyclic schedules. */
    public Optional<JourneyStop> getPreviousStop(JourneyStop stop) {
        int prev = stop.getOrderIndex() - 1;
        if (prev < 0) {
            return cyclic ? Optional.of(stops.get(stops.size() - 1)) : Optional.empty();
        }
        return Optional.of(stops.get(prev));
    }

    /**
     * All stops in the order the train will visit them, starting with the given one. For a
     * non-cyclic schedule the list ends at the last stop instead of wrapping around.
     */
    public List<JourneyStop> getStopsInTravelOrder(JourneyStop first) {
        if (first == null || stops.isEmpty()) {
            return List.of();
        }
        int size = stops.size();
        List<JourneyStop> result = new ArrayList<>(size);
        int start = first.getOrderIndex();
        for (int i = 0; i < size; i++) {
            int idx = start + i;
            if (idx >= size) {
                if (!cyclic) break;
                idx %= size;
            }
            result.add(stops.get(idx));
        }
        return result;
    }

    /** The section following the given one in travel order, wrapping around. */
    public JourneySection getNextSection(JourneySection section) {
        if (sections.size() <= 1) {
            return section;
        }
        return sections.get((section.getSectionIndex() + 1) % sections.size());
    }

    /** The section preceding the given one in travel order, wrapping around. */
    public JourneySection getPreviousSection(JourneySection section) {
        if (sections.size() <= 1) {
            return section;
        }
        return sections.get((section.getSectionIndex() - 1 + sections.size()) % sections.size());
    }

    /**
     * The section the train reaches after the given one, or empty if there is none.
     * <p>
     * Unlike {@link #getNextSection(JourneySection)} this does not wrap a journey that never comes
     * round again: the last section of a one-way run is followed by nothing, and answering "the first
     * one" there would have a train hand its passengers over to a service it is never going to run.
     */
    public Optional<JourneySection> nextSectionOf(JourneySection section) {
        if (section == null || sections.isEmpty()) {
            return Optional.empty();
        }
        int next = section.getSectionIndex() + 1;
        if (next >= sections.size()) {
            return cyclic ? Optional.of(sections.get(0)) : Optional.empty();
        }
        return Optional.of(sections.get(next));
    }

    /** The section the train ran before the given one, or empty if there is none. */
    public Optional<JourneySection> previousSectionOf(JourneySection section) {
        if (section == null || sections.isEmpty()) {
            return Optional.empty();
        }
        int previous = section.getSectionIndex() - 1;
        if (previous < 0) {
            return cyclic ? Optional.of(sections.get(sections.size() - 1)) : Optional.empty();
        }
        return Optional.of(sections.get(previous));
    }

    /**
     * Whether a passenger riding the given section is carried into the one after it.
     * <p>
     * This is the single question every boundary in a journey comes down to. A section that declares
     * it still covers the following section's first stop hands its passengers over; one that does not
     * ends there, and everybody has to get out - whatever the train does next. A section with nothing
     * after it carries nobody onward by definition.
     */
    public boolean carriesPassengersOnward(JourneySection section) {
        return section != null && section.includesNextSectionStart() && nextSectionOf(section).isPresent();
    }

    /**
     * Whether everybody has to get out at the given stop, i.e. whether the train stops being usable
     * beyond it.
     * <p>
     * That is the case at the end of a section that hands nobody over, and at a stop of a section that
     * is not for public use at all - such a stop is only ever shown because the section before it
     * advertised it, which makes it that service's last call.
     */
    public boolean isTerminus(JourneyStop stop) {
        JourneySection section = stop == null ? null : stop.getSection();
        if (section == null) {
            return false;
        }
        if (!section.isUsable()) {
            return true;
        }
        return section.isLastStop(stop) && !carriesPassengersOnward(section);
    }

    /**
     * Whether the train's service starts at the given stop, i.e. whether nobody can already be aboard.
     * The mirror image of {@link #isTerminus(JourneyStop)}: the same boundary seen from the other side.
     */
    public boolean isOrigin(JourneyStop stop) {
        JourneySection section = stop == null ? null : stop.getSection();
        if (section == null || !section.isUsable() || !section.isFirstStop(stop)) {
            return false;
        }
        return previousSectionOf(section).map(previous -> !previous.isUsable() || !carriesPassengersOnward(previous))
            .orElse(true);
    }

    /**
     * Where the service calling at the given stop started, which is not this journey's first stop
     * whenever a section before it handed its passengers over.
     */
    public Optional<JourneyStop> getOriginOf(JourneyStop stop) {
        JourneySection section = stop == null ? null : stop.getSection();
        if (section == null) {
            return Optional.empty();
        }
        // A stop of a section nobody may travel in belongs to the service that advertised it, not to
        // the section it sits in - so the search for that service's start begins one section earlier.
        JourneySection start = section.isUsable() ? section : previousSectionOf(section).orElse(section);
        for (int i = 0; i < sections.size(); i++) {
            JourneySection previous = previousSectionOf(start).orElse(null);
            if (previous == null || !previous.isUsable() || !carriesPassengersOnward(previous)) {
                break;
            }
            start = previous;
        }
        return start.getFirstStop();
    }

    /** The section the given schedule entry index belongs to. */
    public Optional<JourneySection> getSectionAtEntry(int entryIndex) {
        return getStopAtEntry(entryIndex).map(JourneyStop::getSection)
            .or(() -> getCurrentStop(entryIndex).map(JourneyStop::getSection));
    }
}
