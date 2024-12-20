package de.mrjulsen.crn.data;

import java.util.UUID;

import de.mrjulsen.crn.data.StationTag.StationInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public record TrainConnection(String trainName, UUID trainId, ResourceLocation trainIconId, int ticks, String scheduleTitle, StationInfo stationDetails) {

    private static final String NBT_TRAIN_NAME = "TrainName";
    private static final String NBT_TRAIN_ID = "Id";
    private static final String NBT_TRAIN_ICON = "Icon";
    private static final String NBT_TICKS = "Ticks";
    private static final String NBT_SCHEDULE_TITLE = "Title";

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();

        nbt.putString(NBT_TRAIN_NAME, trainName);
        nbt.putUUID(NBT_TRAIN_ID, trainId);
        nbt.putString(NBT_TRAIN_ICON, trainIconId.getPath());
        nbt.putInt(NBT_TICKS, ticks);
        nbt.putString(NBT_SCHEDULE_TITLE, scheduleTitle);
        stationDetails().writeNbt(nbt);

        return nbt;
    }

    public static TrainConnection fromNbt(CompoundTag nbt) {
        return new TrainConnection(
            nbt.getString(NBT_TRAIN_NAME),
            nbt.getUUID(NBT_TRAIN_ID),
            new ResourceLocation(nbt.getString(NBT_TRAIN_ICON)),
            nbt.getInt(NBT_TICKS),
            nbt.getString(NBT_SCHEDULE_TITLE),
            StationInfo.fromNbt(nbt)
        );
    }
}
