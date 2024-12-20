package de.mrjulsen.crn.registry.data;

import java.util.UUID;

import net.minecraft.nbt.CompoundTag;

public record NextConnectionsRequestData(String stationName, UUID selfTrainId) {
    public static final String NBT_STATION_NAME = "StationName";
    public static final String NBT_TRAIN_ID = "TrainId";

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString(NBT_STATION_NAME, stationName());
        nbt.putUUID(NBT_TRAIN_ID, selfTrainId());
        return nbt;
    }

    public static NextConnectionsRequestData fromNbt(CompoundTag nbt) {
        return new NextConnectionsRequestData(
            nbt.getString(NBT_STATION_NAME),
            nbt.getUUID(NBT_TRAIN_ID)
        );
    }
}
