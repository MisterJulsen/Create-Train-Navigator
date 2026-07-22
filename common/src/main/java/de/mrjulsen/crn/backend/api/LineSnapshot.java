package de.mrjulsen.crn.backend.api;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import de.mrjulsen.crn.util.NbtHelper;
import net.minecraft.nbt.CompoundTag;

/**
 * An immutable snapshot of a train line: the line itself plus which trains and stations it
 * currently covers.
 *
 * @param line          The train line itself.
 * @param trainIds      The trains currently operating on this line.
 * @param stations      The stations this line currently calls at, in no particular order.
 * @param delayedTrains How many of its trains are currently late.
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

    /** How many trains currently operate on this line. */
    public int trainCount() {
        return trainIds.size();
    }

    /** Whether any train currently operates on this line. */
    public boolean isOperating() {
        return !trainIds.isEmpty();
    }

    /** Whether any train on this line is currently late. */
    public boolean hasDelays() {
        return delayedTrains > 0;
    }

    /** Serializes this line. */
    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.put(NBT_LINE, line.toNbt());
        nbt.put(NBT_TRAIN_IDS, NbtHelper.writeUuids(trainIds));
        nbt.put(NBT_STATIONS, NbtHelper.writeList(stations, StationRef::toNbt));
        nbt.putInt(NBT_DELAYED_TRAINS, delayedTrains);
        return nbt;
    }

    /** Deserializes a line written by {@link #toNbt()}. */
    public static LineSnapshot fromNbt(CompoundTag nbt) {
        return new LineSnapshot(
            LineRef.fromNbt(nbt.getCompound(NBT_LINE)),
            NbtHelper.readUuids(nbt, NBT_TRAIN_IDS),
            NbtHelper.readList(nbt, NBT_STATIONS, StationRef::fromNbt),
            nbt.getInt(NBT_DELAYED_TRAINS)
        );
    }
}
