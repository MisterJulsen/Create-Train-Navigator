package de.mrjulsen.crn.backend.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.mrjulsen.crn.backend.core.TrackedTrain;
import de.mrjulsen.crn.backend.schedule.JourneySection;
import de.mrjulsen.crn.backend.schedule.JourneyStop;
import de.mrjulsen.crn.util.NbtHelper;
import de.mrjulsen.mcdragonlib.util.DLColor;
import net.minecraft.nbt.CompoundTag;

/**
 * An immutable snapshot of one section of a train's journey: the stretch of the run that carries a
 * particular line and category, with its own stops and its own timetable.
 * <p>
 * A journey without any section instructions still has exactly one, covering the whole run.
 *
 * @param index          The position of this section in travel order, counting from zero.
 * @param entryIndex     The index of the instruction this section was created from.
 * @param line           The assigned train line, or {@link LineRef#NONE} if it carries none.
 * @param category       The assigned train category, or {@link CategoryRef#NONE}.
 * @param origin         The station this section starts from, or {@link StationRef#NONE} if it has
 *                       no stops.
 * @param destination    The terminus advertised for this section, or {@link StationRef#NONE}.
 * @param stops          This section's stops in travel order.
 * @param currentStopIndex The position within {@link #stops()} the train is at, or {@code -1} when
 *                       the train is not operating in this section. {@link #current()} is the
 *                       question usually asked about it.
 * @param usable         Whether this section may be used for travel.
 * @param defaultSection Whether this is the implicit section of a schedule without section
 *                       instructions.
 * @param includesNextSectionStart Whether the following section's first stop still counts as part
 *                       of this one.
 */
public record SectionSnapshot(
    int index,
    int entryIndex,
    LineRef line,
    CategoryRef category,
    StationRef origin,
    StationRef destination,
    List<StopSnapshot> stops,
    int currentStopIndex,
    boolean usable,
    boolean defaultSection,
    boolean includesNextSectionStart
) {

    private static final String NBT_INDEX = "Index";
    private static final String NBT_ENTRY_INDEX = "EntryIndex";
    private static final String NBT_LINE = "Line";
    private static final String NBT_CATEGORY = "Category";
    private static final String NBT_ORIGIN = "Origin";
    private static final String NBT_DESTINATION = "Destination";
    private static final String NBT_STOPS = "Stops";
    private static final String NBT_CURRENT_STOP_INDEX = "CurrentStopIndex";
    private static final String NBT_USABLE = "Usable";
    private static final String NBT_DEFAULT = "Default";
    private static final String NBT_INCLUDES_NEXT_START = "IncludesNextSectionStart";

    public SectionSnapshot {
        stops = stops == null ? List.of() : List.copyOf(stops);
        line = line == null ? LineRef.NONE : line;
        category = category == null ? CategoryRef.NONE : category;
        origin = origin == null ? StationRef.NONE : origin;
        destination = destination == null ? StationRef.NONE : destination;
    }

    /** Captures the given section of the given train. */
    public static SectionSnapshot of(TrackedTrain train, JourneySection section, JourneyStop currentStop) {
        List<StopSnapshot> stops = new ArrayList<>(section.getStops().size());
        int currentStopIndex = -1;
        for (JourneyStop stop : section.getStops()) {
            if (!JourneySnapshot.isPublic(stop, currentStop)) {
                continue;
            }
            if (currentStop != null && stop.getOrderIndex() == currentStop.getOrderIndex()) {
                currentStopIndex = stops.size();
            }
            stops.add(StopSnapshot.of(train, stop));
        }

        return new SectionSnapshot(
            section.getSectionIndex(),
            section.entryIndex(),
            LineRef.of(section.getTrainLine().orElse(null)),
            CategoryRef.of(section.getTrainCategory().orElse(null)),
            section.getFirstStop().map(x -> StationRef.of(train.getDisplayStationName(x))).orElse(StationRef.NONE),
            section.getLastStop().map(x -> StationRef.of(train.getSectionDestination(x))).orElse(StationRef.NONE),
            stops,
            currentStopIndex,
            section.isUsable(),
            section.isDefault(),
            section.includesNextSectionStart()
        );
    }

    /** Whether the train is operating in this section right now. */
    public boolean current() {
        return currentStopIndex >= 0;
    }

    /** The stop the train is at within this section, if it is in this section at all. */
    public Optional<StopSnapshot> currentStop() {
        return currentStopIndex < 0 || currentStopIndex >= stops.size()
            ? Optional.empty()
            : Optional.of(stops.get(currentStopIndex));
    }

    /**
     * How long this section takes from its first arrival to its last departure, in ticks, or
     * {@code -1} if its times are not known yet.
     */
    public long scheduledDuration() {
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

    /** Where the given stop of this section lies relative to the train. */
    public StopVisitState visitStateOf(StopSnapshot stop) {
        int position = stops.indexOf(stop);
        if (currentStopIndex < 0 || position < 0) {
            return StopVisitState.UPCOMING;
        }
        if (position == currentStopIndex) {
            return StopVisitState.CURRENT;
        }
        return position < currentStopIndex ? StopVisitState.PASSED : StopVisitState.UPCOMING;
    }

    /** Whether this section carries a train line at all. */
    public boolean hasLine() {
        return line.isKnown();
    }

    /** Whether this section carries a train category at all. */
    public boolean hasCategory() {
        return category.isKnown();
    }

    /**
     * What to paint this section in: the colour of its line, the colour of its category if the line
     * carries none, and a neutral default if neither does.
     */
    public DLColor displayColor() {
        return ServiceColor.of(line, category);
    }

    /** The stops the train still has to serve in this section, in travel order. */
    public List<StopSnapshot> pendingStops() {
        return currentStopIndex < 0 ? List.copyOf(stops) : stops.subList(currentStopIndex, stops.size());
    }

    /** The stops the train has already served in this section, in travel order. */
    public List<StopSnapshot> passedStops() {
        return currentStopIndex <= 0 ? List.of() : stops.subList(0, currentStopIndex);
    }

    /** The number of stops in this section. */
    public int stopCount() {
        return stops.size();
    }

    /** Serializes this section. */
    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt(NBT_INDEX, index);
        nbt.putInt(NBT_ENTRY_INDEX, entryIndex);
        nbt.put(NBT_LINE, line.toNbt());
        nbt.put(NBT_CATEGORY, category.toNbt());
        nbt.put(NBT_ORIGIN, origin.toNbt());
        nbt.put(NBT_DESTINATION, destination.toNbt());
        nbt.put(NBT_STOPS, NbtHelper.writeList(stops, StopSnapshot::toNbt));
        nbt.putInt(NBT_CURRENT_STOP_INDEX, currentStopIndex);
        nbt.putBoolean(NBT_USABLE, usable);
        nbt.putBoolean(NBT_DEFAULT, defaultSection);
        nbt.putBoolean(NBT_INCLUDES_NEXT_START, includesNextSectionStart);
        return nbt;
    }

    /** Deserializes a section written by {@link #toNbt()}. */
    public static SectionSnapshot fromNbt(CompoundTag nbt) {
        return new SectionSnapshot(
            nbt.getInt(NBT_INDEX),
            nbt.getInt(NBT_ENTRY_INDEX),
            LineRef.fromNbt(nbt.getCompound(NBT_LINE)),
            CategoryRef.fromNbt(nbt.getCompound(NBT_CATEGORY)),
            StationRef.fromNbt(nbt.getCompound(NBT_ORIGIN)),
            StationRef.fromNbt(nbt.getCompound(NBT_DESTINATION)),
            NbtHelper.readList(nbt, NBT_STOPS, StopSnapshot::fromNbt),
            nbt.getInt(NBT_CURRENT_STOP_INDEX),
            nbt.getBoolean(NBT_USABLE),
            nbt.getBoolean(NBT_DEFAULT),
            nbt.getBoolean(NBT_INCLUDES_NEXT_START)
        );
    }
}
