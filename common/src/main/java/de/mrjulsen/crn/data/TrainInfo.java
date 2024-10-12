package de.mrjulsen.crn.data;

import net.minecraft.nbt.CompoundTag;

public record TrainInfo(TrainLine line, TrainGroup group) {

    private static final String NBT_TRAIN_GROUP = "Group";
    private static final String NBT_TRAIN_LINE = "Line";

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        if (group != null) nbt.put(NBT_TRAIN_GROUP, group.toNbt());
        if (line != null) nbt.put(NBT_TRAIN_LINE, line.toNbt());

        return nbt;
    }

    public static TrainInfo fromNbt(CompoundTag nbt) {
        return new TrainInfo(
            nbt.contains(NBT_TRAIN_LINE) ? TrainLine.fromNbt(nbt.getCompound(NBT_TRAIN_LINE)) : null, 
            nbt.contains(NBT_TRAIN_GROUP) ? TrainGroup.fromNbt(nbt.getCompound(NBT_TRAIN_GROUP)) : null
        );
    }

    public static TrainInfo empty() {
        return new TrainInfo(null, null);
    }
}
