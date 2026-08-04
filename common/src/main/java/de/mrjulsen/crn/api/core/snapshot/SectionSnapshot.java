package de.mrjulsen.crn.api.core.snapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.mrjulsen.crn.api.core.*;
import de.mrjulsen.crn.api.core.ref.CategoryRef;
import de.mrjulsen.crn.api.core.ref.LineRef;
import de.mrjulsen.crn.api.core.ref.StationRef;
import de.mrjulsen.crn.core.train.TrackedTrain;
import de.mrjulsen.crn.core.schedule.JourneySection;
import de.mrjulsen.crn.core.schedule.JourneyStop;
import de.mrjulsen.crn.util.NbtHelper;
import de.mrjulsen.mcdragonlib.util.DLColor;
import net.minecraft.nbt.CompoundTag;

/**
 * One section of a train's run: a stretch of consecutive stops worked under the same line and
 * category. A run is divided into sections by the schedule; where it is not divided at all, the
 * whole run forms a single default section.
 * <p>
 * Stops hidden from public display are left out, so the stops here need not cover the section
 * completely.
 *
 * @param index                    The section's position within the run.
 * @param entryIndex               The schedule entry the section begins at.
 * @param line                     The line worked in this section, or {@link LineRef#NONE}.
 * @param category                 The category worked in this section, or {@link CategoryRef#NONE}.
 * @param origin                   The section's first stop.
 * @param destination              The stop this section runs to, as it should be shown to travellers:
 *                                 its own last stop, or the following section's first one where it
 *                                 carries them onward. Where the train goes after that is not part
 *                                 of this section's service.
 * @param stops                    The section's stops in order.
 * @param currentStopIndex         Where the train stands within {@code stops}, or {@code -1} if it
 *                                 is not currently in this section.
 * @param usable                   Whether travellers may use this section. An unusable section is
 *                                 kept out of boards and route searches.
 * @param defaultSection           Whether this stands in for a run that defines no sections of its
 *                                 own.
 * @param includesNextSectionStart Whether the section carries travellers into the first stop of the
 *                                 following section.
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
            train.getSectionTerminus(section).map(StationRef::of).orElse(StationRef.NONE),
            stops,
            currentStopIndex,
            section.isUsable(),
            section.isDefault(),
            section.includesNextSectionStart()
        );
    }

    /** Whether the train is working this section at the moment. */
    public boolean current() {
        return currentStopIndex >= 0;
    }

    /** The stop the train stands at within this section, if it is in this section at all. */
    public Optional<StopSnapshot> currentStop() {
        return currentStopIndex < 0 || currentStopIndex >= stops.size()
            ? Optional.empty()
            : Optional.of(stops.get(currentStopIndex));
    }

    /**
     * How long the section takes end to end according to the timetable, in ticks, or {@code -1}
     * where it has no stops or their times are not known.
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

    /**
     * Whether the train has passed the given stop, stands at it, or has still to reach it. A stop
     * not belonging to this section counts as upcoming.
     */
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

    public boolean hasLine() {
        return line.isKnown();
    }

    public boolean hasCategory() {
        return category.isKnown();
    }

    /** The colour this section should be shown in, taken from its line or category. */
    public DLColor displayColor() {
        return ServiceColor.of(line, category);
    }

    /**
     * The stops the train has still to depart from, including the one it stands at. Yields every
     * stop where the train is not in this section.
     */
    public List<StopSnapshot> pendingStops() {
        return currentStopIndex < 0 ? List.copyOf(stops) : stops.subList(currentStopIndex, stops.size());
    }

    /** The stops the train has already left behind in this section. */
    public List<StopSnapshot> passedStops() {
        return currentStopIndex <= 0 ? List.of() : stops.subList(0, currentStopIndex);
    }

    public int stopCount() {
        return stops.size();
    }

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
