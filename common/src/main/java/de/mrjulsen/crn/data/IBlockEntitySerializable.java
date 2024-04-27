package de.mrjulsen.crn.data;

import net.minecraft.nbt.CompoundTag;

public interface IBlockEntitySerializable {
    CompoundTag serialize();
    void deserialize(CompoundTag nbt);
}
