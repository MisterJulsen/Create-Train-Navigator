package de.mrjulsen.crn.registry.data;

import java.util.UUID;

import net.minecraft.nbt.CompoundTag;

public record NextConnectionsRequestData(String stationName, UUID selfTrainId, boolean allowDuplicates) {
    public static final String NBT_STATION_NAME = "StationName";
    public static final String NBT_TRAIN_ID = "TrainId";
    public static final String NBT_ALLOW_DUPLICATES = "AllowDuplicates";

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString(NBT_STATION_NAME, stationName());
        nbt.putUUID(NBT_TRAIN_ID, selfTrainId());
        nbt.putBoolean(NBT_ALLOW_DUPLICATES, allowDuplicates);
        return nbt;
    }

    public static NextConnectionsRequestData fromNbt(CompoundTag nbt) {
        return new NextConnectionsRequestData(
            nbt.getString(NBT_STATION_NAME),
            nbt.getUUID(NBT_TRAIN_ID),
            nbt.getBoolean(NBT_ALLOW_DUPLICATES)
        );
    }
}
