package de.mrjulsen.crn.backend.api;

import java.util.UUID;

import de.mrjulsen.crn.data.StationTag;
import net.minecraft.nbt.CompoundTag;

/**
 * A station tag as this API refers to it: its identity, without the set of stations behind it.
 * <p>
 * Used where a station names the groups it belongs to. The stations of a tag are a query of their
 * own rather than something every mention of the tag drags along.
 *
 * @param id   The tag's id.
 * @param name The tag's name. May be empty.
 */
public record TagRef(UUID id, String name) {

    /** No tag. */
    public static final TagRef NONE = new TagRef(null, "");

    private static final String NBT_ID = "Id";
    private static final String NBT_NAME = "Name";

    public TagRef {
        name = name == null ? "" : name;
    }

    /** The given tag as a value, or {@link #NONE} if there is none. */
    public static TagRef of(StationTag tag) {
        if (tag == null) {
            return NONE;
        }
        return new TagRef(tag.getId(), tag.getTagName() == null ? "" : tag.getTagName().get());
    }

    /** Whether this refers to a tag at all. */
    public boolean isKnown() {
        return id != null;
    }

    /** Serializes this tag. */
    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        if (id != null) {
            nbt.putUUID(NBT_ID, id);
        }
        nbt.putString(NBT_NAME, name);
        return nbt;
    }

    /** Deserializes a tag written by {@link #toNbt()}. */
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
