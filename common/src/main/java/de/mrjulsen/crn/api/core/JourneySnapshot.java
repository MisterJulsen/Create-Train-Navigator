package de.mrjulsen.crn.api.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import de.mrjulsen.crn.core.train.TrackedTrain;
import de.mrjulsen.crn.core.schedule.JourneySection;
import de.mrjulsen.crn.core.schedule.JourneyStop;
import de.mrjulsen.crn.core.schedule.TrainJourney;
import de.mrjulsen.crn.data.settings.GlobalSettings;
import de.mrjulsen.crn.util.NbtHelper;
import net.minecraft.nbt.CompoundTag;

/**
 * A train's whole run, stop by stop, grouped into sections, with the train's progress marked.
 * <p>
 * The snapshot is taken as a whole and is internally consistent, so it should be preferred over
 * assembling a run from separate stop queries. Stops at blacklisted stations are left out, except
 * where the train currently stands at one, so that its position stays describable.
 * <p>
 * Times are in the unit described by {@link RailwayBackendApi#currentTime()}.
 *
 * @param trainId             The train this describes.
 * @param trainName           The train's own name.
 * @param line                The line of the section being worked now, or {@link LineRef#NONE}.
 * @param cyclic              Whether the schedule starts over once it ends.
 * @param totalDuration       How long one full cycle takes, or a negative value if not yet known.
 * @param stops               Every stop of the run in schedule order.
 * @param sections            The sections the run is divided into.
 * @param currentStopIndex    Where the train stands within {@code stops}, or {@code -1} if it
 *                            stands at none.
 * @param currentSectionIndex The section being worked now, or {@code -1}.
 */
public record JourneySnapshot(
    UUID trainId,
    String trainName,
    LineRef line,
    boolean cyclic,
    long totalDuration,
    List<StopSnapshot> stops,
    List<SectionSnapshot> sections,
    int currentStopIndex,
    int currentSectionIndex
) {

    private static final String NBT_TRAIN_ID = "TrainId";
    private static final String NBT_TRAIN_NAME = "TrainName";
    private static final String NBT_LINE = "Line";
    private static final String NBT_CYCLIC = "Cyclic";
    private static final String NBT_TOTAL_DURATION = "TotalDuration";
    private static final String NBT_STOPS = "Stops";
    private static final String NBT_SECTIONS = "Sections";
    private static final String NBT_CURRENT_STOP_INDEX = "CurrentStopIndex";
    private static final String NBT_CURRENT_SECTION_INDEX = "CurrentSectionIndex";

    public JourneySnapshot {
        stops = stops == null ? List.of() : List.copyOf(stops);
        sections = sections == null ? List.of() : List.copyOf(sections);
        trainName = trainName == null ? "" : trainName;
        line = line == null ? LineRef.NONE : line;
    }

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
                stops.add(StopSnapshot.of(train, stop));
            }

            List<SectionSnapshot> sections = new ArrayList<>(journey.getSections().size());
            for (JourneySection section : journey.getSections()) {
                sections.add(SectionSnapshot.of(train, section, currentStop));
            }

            return new JourneySnapshot(
                train.getTrainId(),
                train.getTrainName(),
                LineRef.of(currentSection == null ? null : currentSection.getTrainLine().orElse(null)),
                journey.isCyclic(),
                train.getTotalDuration(),
                stops,
                sections,
                currentStopIndex,
                currentSection == null ? -1 : currentSection.getSectionIndex()
            );
        });
    }

    static boolean isPublic(JourneyStop stop, JourneyStop currentStop) {
        return stop == currentStop || !GlobalSettings.getInstance().isStationBlacklisted(stop.getStationName());
    }

    /** The name to show for this train: its line name where it has one, otherwise its own name. */
    public String displayName() {
        return line.hasName() ? line.name() : trainName;
    }

    /**
     * Whether the train has passed the given stop, stands at it, or has still to reach it. A stop
     * not belonging to this run counts as upcoming.
     */
    public StopVisitState visitStateOf(StopSnapshot stop) {
        int position = stop == null ? -1 : stops.indexOf(stop);
        if (currentStopIndex < 0 || position < 0) {
            return StopVisitState.UPCOMING;
        }
        if (position == currentStopIndex) {
            return StopVisitState.CURRENT;
        }
        return position < currentStopIndex ? StopVisitState.PASSED : StopVisitState.UPCOMING;
    }

    /** The stop the train stands at, or empty while it is running between stops. */
    public Optional<StopSnapshot> currentStop() {
        return currentStopIndex < 0 || currentStopIndex >= stops.size()
            ? Optional.empty()
            : Optional.of(stops.get(currentStopIndex));
    }

    /** The section the train is working now. */
    public Optional<SectionSnapshot> currentSection() {
        return sections.stream().filter(SectionSnapshot::current).findFirst();
    }

    /**
     * The stops the train has still to depart from, beginning with the one it stands at. On a
     * cyclic run this wraps around and covers one whole cycle. Empty while the train stands at no
     * stop.
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

    /** The stops the train has already left behind on this run. */
    public List<StopSnapshot> passedStops() {
        return currentStopIndex <= 0 ? List.of() : stops.subList(0, currentStopIndex);
    }

    /** The stop the train will reach after the one it stands at. */
    public Optional<StopSnapshot> nextStop() {
        List<StopSnapshot> upcoming = upcomingStops();
        return upcoming.size() < 2 ? Optional.empty() : Optional.of(upcoming.get(1));
    }

    /** The final stop in schedule order, which on a cyclic run is not an end of the journey. */
    public Optional<StopSnapshot> lastStop() {
        return stops.isEmpty() ? Optional.empty() : Optional.of(stops.get(stops.size() - 1));
    }

    /** The section at the given position, or empty if there is none. */
    public Optional<SectionSnapshot> section(int index) {
        return index < 0 || index >= sections.size() ? Optional.empty() : Optional.of(sections.get(index));
    }

    /** The section a stop belongs to. */
    public Optional<SectionSnapshot> sectionOf(StopSnapshot stop) {
        return stop == null ? Optional.empty() : section(stop.sectionIndex());
    }

    /** The section following the given one, wrapping around on a cyclic run. */
    public Optional<SectionSnapshot> nextSection(SectionSnapshot section) {
        if (section == null || sections.isEmpty()) {
            return Optional.empty();
        }
        int next = section.index() + 1;
        return next >= sections.size() ? (cyclic ? section(0) : Optional.empty()) : section(next);
    }

    /** The section preceding the given one, wrapping around on a cyclic run. */
    public Optional<SectionSnapshot> previousSection(SectionSnapshot section) {
        if (section == null || sections.isEmpty()) {
            return Optional.empty();
        }
        int previous = section.index() - 1;
        return previous < 0 ? (cyclic ? section(sections.size() - 1) : Optional.empty()) : section(previous);
    }

    /**
     * Whether travellers aboard in the given section stay aboard into the following one, rather
     * than the section ending their journey.
     */
    public boolean carriesPassengersOnward(SectionSnapshot section) {
        return section != null && section.includesNextSectionStart() && nextSection(section).isPresent();
    }

    /**
     * Whether the stop ends a journey for travellers aboard, either because its section ends there
     * without carrying them onward, or because the section cannot be used at all.
     */
    public boolean isTerminus(StopSnapshot stop) {
        SectionSnapshot section = sectionOf(stop).orElse(null);
        if (section == null) {
            return false;
        }
        if (!section.usable()) {
            return true;
        }
        return isSectionEnd(stop) && !carriesPassengersOnward(section);
    }

    /**
     * Whether travellers can first board at this stop, meaning its section begins there and nothing
     * carried them in from the section before.
     */
    public boolean isOrigin(StopSnapshot stop) {
        SectionSnapshot section = sectionOf(stop).orElse(null);
        if (section == null || !section.usable() || !isSectionStart(stop)) {
            return false;
        }
        return previousSection(section)
            .map(previous -> !previous.usable() || !carriesPassengersOnward(previous))
            .orElse(true);
    }

    /**
     * Whether the train changes from one section to the next at this stop while carrying travellers
     * through, so the service continues under a different line or category.
     */
    public boolean isHandover(StopSnapshot stop) {
        SectionSnapshot section = sectionOf(stop).orElse(null);
        if (section == null || !section.usable() || !isSectionStart(stop)) {
            return false;
        }
        return previousSection(section).map(previous -> previous.usable() && carriesPassengersOnward(previous)).orElse(false);
    }

    /**
     * The section the train should be described as working right now. At a stop where one section
     * hands over to the next, the train still counts as working the earlier one until it has
     * arrived, so that a service is not renamed while travellers are still boarding.
     *
     * @param arrived Whether the train has come to a stand at its current stop.
     */
    public Optional<SectionSnapshot> operatingSection(boolean arrived) {
        StopSnapshot current = currentStop().orElse(null);
        SectionSnapshot section = current == null ? null : sectionOf(current).orElse(null);
        if (section == null) {
            return currentSection();
        }
        if (isSectionStart(current)) {
            Optional<SectionSnapshot> previous = previousSection(section);
            boolean carriedOver = previous.map(p -> p.usable() && carriesPassengersOnward(p)).orElse(false);
            if (carriedOver && (!section.usable() || !arrived)) {
                return previous;
            }
        }
        return Optional.of(section);
    }

    /**
     * The stops travellers can reach aboard the given section, which includes the first stop of the
     * following section where the train carries them onward. On a cyclic run whose only section
     * carries travellers back into itself, that closing stop is the section's own first one again,
     * listed a second time with the times of the coming cycle.
     *
     * @param arrived Whether the train has come to a stand at its current stop.
     */
    public List<StopSnapshot> servedStops(SectionSnapshot section, boolean arrived) {
        if (section == null) {
            return List.of();
        }
        if (!carriesPassengersOnward(section)) {
            return section.stops();
        }
        List<StopSnapshot> own = section.stops();
        List<StopSnapshot> served = new ArrayList<>(own.size() + 1);
        served.addAll(own);
        nextSection(section).flatMap(next -> next.stops().stream().findFirst())
            .map(onward -> closesItsOwnRun(own, onward, arrived) ? onward.advancedBy(1, totalDuration) : onward)
            .ifPresent(served::add);
        return List.copyOf(served);
    }

    /**
     * Whether the stop closing the service is the very one it opened with and the train still stands
     * there, about to drive the whole run. A stop holds the times of its next call, so in that one
     * case the closing call is a whole cycle later than what the stop reports; at every other point
     * of the run the reported call is already the closing one.
     */
    private boolean closesItsOwnRun(List<StopSnapshot> own, StopSnapshot onward, boolean arrived) {
        if (!arrived || own.stream().noneMatch(x -> x.entryIndex() == onward.entryIndex())) {
            return false;
        }
        return currentStop().map(x -> x.entryIndex() == onward.entryIndex()).orElse(false);
    }

    /**
     * Where the train stands within {@link #servedStops(SectionSnapshot, boolean)}, or {@code -1} where it
     * stands at none of them. A run that wraps around into itself lists its first stop twice, once
     * as the start of the service and once as its end; which of the two the train is at depends on
     * whether it has already come to a stand there.
     *
     * @param arrived Whether the train has come to a stand at its current stop.
     */
    public int servedStopIndex(SectionSnapshot section, boolean arrived) {
        StopSnapshot current = currentStop().orElse(null);
        if (current == null) {
            return -1;
        }
        List<StopSnapshot> served = servedStops(section, arrived);
        int found = -1;
        for (int i = 0; i < served.size(); i++) {
            if (served.get(i).entryIndex() != current.entryIndex()) {
                continue;
            }
            found = i;
            if (arrived) {
                break;
            }
        }
        return found;
    }

    /** Whether the stop is the first of its section. */
    public boolean isSectionStart(StopSnapshot stop) {
        return sectionOf(stop)
            .map(section -> !section.stops().isEmpty()
                && section.stops().get(0).entryIndex() == stop.entryIndex())
            .orElse(false);
    }

    /** Whether the stop is the last of its section. */
    public boolean isSectionEnd(StopSnapshot stop) {
        return sectionOf(stop)
            .map(section -> !section.stops().isEmpty()
                && section.stops().get(section.stops().size() - 1).entryIndex() == stop.entryIndex())
            .orElse(false);
    }

    /** How far through its run the train is, from zero to one. */
    public double progress() {
        if (stops.size() < 2 || currentStopIndex < 0) {
            return 0;
        }
        return (double) currentStopIndex / (stops.size() - 1);
    }

    /** Whether the run has no stops, which is the case for a train without a usable schedule. */
    public boolean isEmpty() {
        return stops.isEmpty();
    }

    /**
     * Whether the run can be projected into future cycles, which needs it to be cyclic and its
     * cycle duration to be known.
     */
    public boolean repeats() {
        return cyclic && totalDuration > 0;
    }

    /**
     * The train's next call at the given station at or after the given time. On a repeating run
     * later cycles are considered, so a call can be found beyond the end of the current one.
     *
     * @param stationNameOrFilter A station name, or a filter using the schedule's wildcard syntax.
     */
    public Optional<StopSnapshot> nextCallAt(String stationNameOrFilter, long notBefore) {
        StopSnapshot best = null;
        for (StopSnapshot stop : stops) {
            if (!stop.realtimeStation().matches(stationNameOrFilter) || !stop.hasTimes()) {
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
     * The same run as it falls the given number of cycles later. Returns this run unchanged where
     * it does not repeat or no projection is asked for.
     */
    public JourneySnapshot advancedBy(int cycles) {
        if (cycles <= 0 || !repeats()) {
            return this;
        }
        List<StopSnapshot> advanced = new ArrayList<>(stops.size());
        for (StopSnapshot stop : stops) {
            advanced.add(stop.advancedBy(cycles, totalDuration));
        }
        return new JourneySnapshot(trainId, trainName, line, cyclic, totalDuration,
            advanced, sections, currentStopIndex, currentSectionIndex);
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        NbtHelper.putNullableUUID(nbt, NBT_TRAIN_ID, trainId);
        nbt.putString(NBT_TRAIN_NAME, trainName);
        nbt.put(NBT_LINE, line.toNbt());
        nbt.putBoolean(NBT_CYCLIC, cyclic);
        nbt.putLong(NBT_TOTAL_DURATION, totalDuration);
        nbt.put(NBT_STOPS, NbtHelper.writeList(stops, StopSnapshot::toNbt));
        nbt.put(NBT_SECTIONS, NbtHelper.writeList(sections, SectionSnapshot::toNbt));
        nbt.putInt(NBT_CURRENT_STOP_INDEX, currentStopIndex);
        nbt.putInt(NBT_CURRENT_SECTION_INDEX, currentSectionIndex);
        return nbt;
    }

    public static JourneySnapshot fromNbt(CompoundTag nbt) {
        return new JourneySnapshot(
            NbtHelper.readNullableUUID(nbt, NBT_TRAIN_ID),
            nbt.getString(NBT_TRAIN_NAME),
            LineRef.fromNbt(nbt.getCompound(NBT_LINE)),
            nbt.getBoolean(NBT_CYCLIC),
            nbt.getLong(NBT_TOTAL_DURATION),
            NbtHelper.readList(nbt, NBT_STOPS, StopSnapshot::fromNbt),
            NbtHelper.readList(nbt, NBT_SECTIONS, SectionSnapshot::fromNbt),
            nbt.getInt(NBT_CURRENT_STOP_INDEX),
            nbt.getInt(NBT_CURRENT_SECTION_INDEX)
        );
    }
}
