package de.mrjulsen.crn.core.schedule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;

import com.simibubi.create.content.trains.schedule.Schedule;

public final class TrainJourney {

    private final UUID trainId;
    private final transient Schedule schedule;

    private final List<JourneyStop> stops;
    private final Map<Integer, JourneyStop> stopsByEntryIndex;
    private final List<JourneySection> sections;

    private final Set<Integer> resetTimingEntries;
    private final boolean cyclic;
    private final boolean flexibleDwellTimes;

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

    public static TrainJourney empty(UUID trainId) {
        return new TrainJourney(trainId, null, List.of(), List.of(), Set.of(), false, false);
    }

    public UUID getTrainId() {
        return trainId;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public boolean isEmpty() {
        return stops.isEmpty();
    }

    public boolean isCyclic() {
        return cyclic;
    }

    public boolean hasFlexibleDwellTimes() {
        return flexibleDwellTimes;
    }

    public List<JourneyStop> getStops() {
        return stops;
    }

    public int getStopCount() {
        return stops.size();
    }

    public List<JourneySection> getSections() {
        return sections;
    }

    public Set<Integer> getResetTimingEntries() {
        return resetTimingEntries;
    }

    public Optional<JourneyStop> getStopAtEntry(int entryIndex) {
        return Optional.ofNullable(stopsByEntryIndex.get(entryIndex));
    }

    public Optional<JourneyStop> getCurrentStop(int currentEntryIndex) {
        if (stops.isEmpty()) {
            return Optional.empty();
        }
        if (currentEntryIndex < 0 || currentEntryIndex >= stopAtOrAfterEntry.length) {
            return Optional.of(stops.get(0));
        }
        return Optional.of(stops.get(stopAtOrAfterEntry[currentEntryIndex]));
    }

    public Optional<JourneyStop> getNextStop(JourneyStop stop) {
        int next = stop.getOrderIndex() + 1;
        if (next >= stops.size()) {
            return cyclic ? Optional.of(stops.get(0)) : Optional.empty();
        }
        return Optional.of(stops.get(next));
    }

    public Optional<JourneyStop> getPreviousStop(JourneyStop stop) {
        int prev = stop.getOrderIndex() - 1;
        if (prev < 0) {
            return cyclic ? Optional.of(stops.get(stops.size() - 1)) : Optional.empty();
        }
        return Optional.of(stops.get(prev));
    }

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

    public JourneySection getNextSection(JourneySection section) {
        if (sections.size() <= 1) {
            return section;
        }
        return sections.get((section.getSectionIndex() + 1) % sections.size());
    }

    public JourneySection getPreviousSection(JourneySection section) {
        if (sections.size() <= 1) {
            return section;
        }
        return sections.get((section.getSectionIndex() - 1 + sections.size()) % sections.size());
    }

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

    public boolean carriesPassengersOnward(JourneySection section) {
        return section != null && section.includesNextSectionStart() && nextSectionOf(section).isPresent();
    }

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

    public boolean isOrigin(JourneyStop stop) {
        JourneySection section = stop == null ? null : stop.getSection();
        if (section == null || !section.isUsable() || !section.isFirstStop(stop)) {
            return false;
        }
        return previousSectionOf(section).map(previous -> !previous.isUsable() || !carriesPassengersOnward(previous))
            .orElse(true);
    }

    public Optional<JourneyStop> getOriginOf(JourneyStop stop) {
        JourneySection section = stop == null ? null : stop.getSection();
        if (section == null) {
            return Optional.empty();
        }
        JourneySection origin = section;
        if (section.isFirstStop(stop) || !section.isUsable()) {
            JourneySection previous = previousSectionOf(section).orElse(null);
            if (previous != null && previous.isUsable() && carriesPassengersOnward(previous)) {
                origin = previous;
            }
        }
        return origin.getFirstStop();
    }

    public Optional<JourneySection> getSectionAtEntry(int entryIndex) {
        return getStopAtEntry(entryIndex).map(JourneyStop::getSection)
            .or(() -> getCurrentStop(entryIndex).map(JourneyStop::getSection));
    }
}
