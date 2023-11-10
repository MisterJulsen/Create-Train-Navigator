package de.mrjulsen.crn.data;

import net.minecraft.nbt.CompoundTag;

public class AliasName {

    private static final String NBT_NAME = "Name";
    private String name;

    public AliasName(String name) {
        this.name = name;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString(NBT_NAME, get());
        return nbt;
    }

    public static AliasName fromNbt(CompoundTag nbt) {
        return new AliasName(nbt.getString(NBT_NAME));
    }

    public String get() {
        return name;
    }

    public String set(String name) {
        return this.name = name;
    }

    public static AliasName of(String name) {
        return new AliasName(name);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AliasName aliasName) {
            return name.equals(aliasName.get());
        }

        return false;
    }
}

