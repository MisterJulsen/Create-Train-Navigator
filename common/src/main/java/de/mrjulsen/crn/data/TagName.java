package de.mrjulsen.crn.data;

import net.minecraft.nbt.CompoundTag;

public class TagName {

    public static final TagName EMPTY = TagName.of("");
    private static final String NBT_NAME = "Name";
    private String name = "";

    public TagName(String name) {
        this.name = name;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString(NBT_NAME, get());
        return nbt;
    }

    public static TagName fromNbt(CompoundTag nbt) {
        return new TagName(nbt.getString(NBT_NAME));
    }

    public String get() {
        return name;
    }

    public String set(String name) {
        return this.name = name;
    }

    public static TagName of(String name) {
        return new TagName(name);
    }

    public boolean isEmpty() {
        return name == null || name.isBlank();
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TagName aliasName) {
            return name.equals(aliasName.get());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return get().hashCode();
    }
}

