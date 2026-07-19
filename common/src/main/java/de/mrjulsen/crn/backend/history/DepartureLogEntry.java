package de.mrjulsen.crn.backend.history;

import java.util.UUID;

import net.minecraft.nbt.CompoundTag;

/**
 * One recorded departure of a train at a station.
 *
 * @param time         The transformed game time of the departure.
 * @param trainId      The id of the train.
 * @param trainName    The name of the train at the time of departure.
 * @param lineId       The id of the train line active at the time of departure, or {@code null}.
 * @param categoryId   The id of the train category active at the time of departure, or {@code null}.
 * @param destination  The destination (final stop of the section) the train was heading to.
 */
public record DepartureLogEntry(long time, UUID trainId, String trainName, UUID lineId, UUID categoryId, String destination) {

    private static final String NBT_TIME = "Time";
    private static final String NBT_TRAIN_ID = "TrainId";
    private static final String NBT_TRAIN_NAME = "TrainName";
    private static final String NBT_LINE = "Line";
    private static final String NBT_CATEGORY = "Category";
    private static final String NBT_DESTINATION = "Destination";

    /** Returns a copy of this entry with its departure time shifted by the given amount of ticks. */
    public DepartureLogEntry shifted(long ticks) {
        return new DepartureLogEntry(time + ticks, trainId, trainName, lineId, categoryId, destination);
    }

    /** Serializes this entry. */
    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putLong(NBT_TIME, time);
        nbt.putUUID(NBT_TRAIN_ID, trainId);
        nbt.putString(NBT_TRAIN_NAME, trainName);
        if (lineId != null) nbt.putUUID(NBT_LINE, lineId);
        if (categoryId != null) nbt.putUUID(NBT_CATEGORY, categoryId);
        nbt.putString(NBT_DESTINATION, destination);
        return nbt;
    }

    /** Deserializes an entry written by {@link #toNbt()}. */
    public static DepartureLogEntry fromNbt(CompoundTag nbt) {
        return new DepartureLogEntry(
            nbt.getLong(NBT_TIME),
            nbt.getUUID(NBT_TRAIN_ID),
            nbt.getString(NBT_TRAIN_NAME),
            nbt.contains(NBT_LINE) ? nbt.getUUID(NBT_LINE) : null,
            nbt.contains(NBT_CATEGORY) ? nbt.getUUID(NBT_CATEGORY) : null,
            nbt.getString(NBT_DESTINATION)
        );
    }
}
