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
import de.mrjulsen.crn.util.NbtHelper;
import net.minecraft.nbt.CompoundTag;

/**
 * An immutable snapshot of a train's whole journey: every stop of the run with its timetable and
 * projected times, grouped into sections, and marked with how far the train has got.
 * <p>
 * Taken while no backend update is in progress, so every stop belongs to the same projection pass -
 * unlike values read one by one through the individual queries.
 * <p>
 * Where a stop lies relative to the train follows from {@link #currentStopIndex()} and is answered
 * by {@link #visitStateOf(StopSnapshot)} rather than stored on every stop.
 *
 * @param trainId         The id of the train this journey belongs to.
 * @param trainName       The train's own name.
 * @param line            The current section's train line, or {@link LineRef#NONE}. What to show as
 *                        the journey's name is {@link #displayName()}.
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
     * What to show as this journey's name: the name of the line the train currently runs on, or its
     * own name if it carries no line or the line is unnamed.
     */
    public String displayName() {
        return line.hasName() ? line.name() : trainName;
    }

    /**
     * Where the given stop lies relative to the train's position. Stops before the current one on
     * this run count as passed; on a cyclic journey that means "earlier in the current cycle".
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
        return currentStopIndex <= 0 ? List.of() : stops.subList(0, currentStopIndex);
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

    /** The section the given stop belongs to. */
    public Optional<SectionSnapshot> sectionOf(StopSnapshot stop) {
        return stop == null ? Optional.empty() : section(stop.sectionIndex());
    }

    /** The section the train reaches after the given one, or empty if there is none. */
    public Optional<SectionSnapshot> nextSection(SectionSnapshot section) {
        if (section == null || sections.isEmpty()) {
            return Optional.empty();
        }
        int next = section.index() + 1;
        return next >= sections.size() ? (cyclic ? section(0) : Optional.empty()) : section(next);
    }

    /** The section the train ran before the given one, or empty if there is none. */
    public Optional<SectionSnapshot> previousSection(SectionSnapshot section) {
        if (section == null || sections.isEmpty()) {
            return Optional.empty();
        }
        int previous = section.index() - 1;
        return previous < 0 ? (cyclic ? section(sections.size() - 1) : Optional.empty()) : section(previous);
    }

    /**
     * Whether a passenger riding the given section is carried into the one after it.
     * Mirrors {@link de.mrjulsen.crn.backend.schedule.TrainJourney#carriesPassengersOnward} - the
     * rule lives there; this answers it from a snapshot, for a client that has no journey to ask.
     */
    public boolean carriesPassengersOnward(SectionSnapshot section) {
        return section != null && section.includesNextSectionStart() && nextSection(section).isPresent();
    }

    /**
     * Whether everybody has to get out at the given stop, i.e. whether the train stops being usable
     * beyond it. See {@link de.mrjulsen.crn.backend.schedule.TrainJourney#isTerminus}.
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
     * Whether the train's service starts at the given stop, i.e. whether nobody can already be aboard.
     * See {@link de.mrjulsen.crn.backend.schedule.TrainJourney#isOrigin}.
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
     * Whether the service the train arrives with ends at this stop while the train itself carries on
     * as the next one - the point where one line becomes another without anybody having to get out.
     * <p>
     * Neither a terminus nor an origin: passengers ride through it. What changes is only what the
     * train is called, which is why a display approaching such a stop still shows the arriving
     * service and switches over once it is standing there.
     */
    public boolean isHandover(StopSnapshot stop) {
        SectionSnapshot section = sectionOf(stop).orElse(null);
        if (section == null || !section.usable() || !isSectionStart(stop)) {
            return false;
        }
        return previousSection(section).map(previous -> previous.usable() && carriesPassengersOnward(previous)).orElse(false);
    }

    /**
     * The service the train is running at this moment.
     * <p>
     * The complication is the stop a section carries over from the next one. Such a stop belongs, by
     * index, to the section it opens, but the train reaches it still running the previous section that
     * advertised it. Which service to show then depends on what that opened section is:
     * <ul>
     *   <li>If it is usable, the stop is a handover - the train runs the arriving service until it
     *       stands there, then takes on the new one. So it shows the previous section on the way in
     *       and the opened one once arrived.</li>
     *   <li>If it is not usable, there is no new service to take on: the stop is a plain terminus the
     *       previous section runs to, so that section stays the operating one throughout. Reading the
     *       opened, unusable section here is what made the train look out of service at its own
     *       terminus.</li>
     * </ul>
     *
     * @param arrived Whether the train has reached its current stop.
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
     * The stops a passenger riding the given section is carried through: its own, and the first stop
     * of the following one wherever it declares that it still covers it.
     */
    public List<StopSnapshot> servedStops(SectionSnapshot section) {
        if (section == null) {
            return List.of();
        }
        if (!carriesPassengersOnward(section)) {
            return section.stops();
        }
        List<StopSnapshot> served = new ArrayList<>(section.stops().size() + 1);
        served.addAll(section.stops());
        nextSection(section).flatMap(next -> next.stops().stream().findFirst()).ifPresent(served::add);
        return List.copyOf(served);
    }

    /** Whether the given stop is the first of its section. */
    public boolean isSectionStart(StopSnapshot stop) {
        return sectionOf(stop)
            .map(section -> !section.stops().isEmpty()
                && section.stops().get(0).entryIndex() == stop.entryIndex())
            .orElse(false);
    }

    /** Whether the given stop is the last of its section. */
    public boolean isSectionEnd(StopSnapshot stop) {
        return sectionOf(stop)
            .map(section -> !section.stops().isEmpty()
                && section.stops().get(section.stops().size() - 1).entryIndex() == stop.entryIndex())
            .orElse(false);
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
        return new JourneySnapshot(trainId, trainName, line, cyclic, totalDuration,
            advanced, sections, currentStopIndex, currentSectionIndex);
    }

    /** Serializes this journey. */
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

    /** Deserializes a journey written by {@link #toNbt()}. */
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
