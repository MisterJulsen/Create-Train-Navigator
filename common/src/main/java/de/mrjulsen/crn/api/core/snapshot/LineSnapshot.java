package de.mrjulsen.crn.api.core.snapshot;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import de.mrjulsen.crn.api.core.ref.LineRef;
import de.mrjulsen.crn.api.core.ref.StationRef;
import de.mrjulsen.crn.util.NbtHelper;
import net.minecraft.nbt.CompoundTag;

/**
 * A line together with the trains currently working it and the stations they serve. Only trains fit
 * to be reported are counted.
 *
 * @param line          The line this describes.
 * @param trainIds      The ids of the trains currently operating on it.
 * @param stations      The stations those trains serve while on this line, in the order they were
 *                      encountered rather than in any timetable order.
 * @param delayedTrains How many of those trains are running late.
 */
public record LineSnapshot(
    LineRef line,
    Set<UUID> trainIds,
    List<StationRef> stations,
    int delayedTrains
) {

    private static final String NBT_LINE = "Line";
    private static final String NBT_TRAIN_IDS = "TrainIds";
    private static final String NBT_STATIONS = "Stations";
    private static final String NBT_DELAYED_TRAINS = "DelayedTrains";

    public LineSnapshot {
        line = line == null ? LineRef.NONE : line;
        trainIds = trainIds == null ? Set.of() : Set.copyOf(trainIds);
        stations = stations == null ? List.of() : List.copyOf(stations);
    }

    /** The line's id. */
    public UUID id() {
        return line.id();
    }

    /** The line's name. */
    public String name() {
        return line.name();
    }

    /** How many trains are currently working this line. */
    public int trainCount() {
        return trainIds.size();
    }

    /** Whether any train is currently working this line. */
    public boolean isOperating() {
        return !trainIds.isEmpty();
    }

    /** Whether any of those trains is running late. */
    public boolean hasDelays() {
        return delayedTrains > 0;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.put(NBT_LINE, line.toNbt());
        nbt.put(NBT_TRAIN_IDS, NbtHelper.writeUuids(trainIds));
        nbt.put(NBT_STATIONS, NbtHelper.writeList(stations, StationRef::toNbt));
        nbt.putInt(NBT_DELAYED_TRAINS, delayedTrains);
        return nbt;
    }

    public static LineSnapshot fromNbt(CompoundTag nbt) {
        return new LineSnapshot(
            LineRef.fromNbt(nbt.getCompound(NBT_LINE)),
            NbtHelper.readUuids(nbt, NBT_TRAIN_IDS),
            NbtHelper.readList(nbt, NBT_STATIONS, StationRef::fromNbt),
            nbt.getInt(NBT_DELAYED_TRAINS)
        );
    }
}
