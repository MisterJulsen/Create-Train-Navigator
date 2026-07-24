package de.mrjulsen.crn.api.core;

import java.util.UUID;

import de.mrjulsen.crn.data.settings.StationTag;
import net.minecraft.nbt.CompoundTag;

/**
 * A station tag as referred to from elsewhere in the API. A tag groups several stations that
 * travellers regard as one place. Can be written to NBT and read back.
 *
 * @param id   The tag's id, or {@code null} if this refers to no tag.
 * @param name The tag's name, never {@code null} but possibly empty.
 */
public record TagRef(UUID id, String name) {

    /** Stands for "no tag". {@link #isKnown()} is false. */
    public static final TagRef NONE = new TagRef(null, "");

    private static final String NBT_ID = "Id";
    private static final String NBT_NAME = "Name";

    public TagRef {
        name = name == null ? "" : name;
    }

    public static TagRef of(StationTag tag) {
        if (tag == null) {
            return NONE;
        }
        return new TagRef(tag.getId(), tag.getTagName() == null ? "" : tag.getTagName().get());
    }

    /** Whether this refers to a tag at all, as opposed to being {@link #NONE}. */
    public boolean isKnown() {
        return id != null;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        if (id != null) {
            nbt.putUUID(NBT_ID, id);
        }
        nbt.putString(NBT_NAME, name);
        return nbt;
    }

    public static TagRef fromNbt(CompoundTag nbt) {
        if (nbt == null || !nbt.hasUUID(NBT_ID)) {
            return NONE;
        }
        return new TagRef(nbt.getUUID(NBT_ID), nbt.getString(NBT_NAME));
    }

    @Override
    public String toString() {
        return name;
    }
}
