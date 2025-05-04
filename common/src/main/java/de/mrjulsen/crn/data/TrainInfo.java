package de.mrjulsen.crn.data;

import net.minecraft.nbt.CompoundTag;

public record TrainInfo(TrainLine line, TrainCategory category) {

    private static final String NBT_TRAIN_CATEGORY = "Category";
    private static final String NBT_TRAIN_LINE = "Line";

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        if (category != null) nbt.put(NBT_TRAIN_CATEGORY, category.toNbt());
        if (line != null) nbt.put(NBT_TRAIN_LINE, line.toNbt());

        return nbt;
    }

    public static TrainInfo fromNbt(CompoundTag nbt) {
        return new TrainInfo(
            nbt.contains(NBT_TRAIN_LINE) ? TrainLine.fromNbt(nbt.getCompound(NBT_TRAIN_LINE)) : null, 
            nbt.contains(NBT_TRAIN_CATEGORY) ? TrainCategory.fromNbt(nbt.getCompound(NBT_TRAIN_CATEGORY)) : null
        );
    }

    public static TrainInfo empty() {
        return new TrainInfo(null, null);
    }
}
