package de.mrjulsen.crn.backend.api;

import java.util.UUID;

import de.mrjulsen.crn.data.TrainCategory;
import de.mrjulsen.mcdragonlib.util.DLColor;
import net.minecraft.nbt.CompoundTag;

/**
 * A train category as this API refers to it: the identity and the two properties anything
 * displaying a category actually needs, copied out of the mutable {@link TrainCategory} it was
 * taken from.
 * <p>
 * A snapshot is a moment in time, so the category it names is one too. Editing the category
 * afterwards does not change a snapshot that already went out - re-query to see the new name or
 * colour.
 *
 * @param id    The category's id.
 * @param name  The category's name. May be empty.
 * @param color The category's colour.
 */
public record CategoryRef(UUID id, String name, DLColor color) {

    /** No category, for a train or section that carries none. */
    public static final CategoryRef NONE = new CategoryRef(null, "", DLColor.TRANSPARENT);

    private static final String NBT_ID = "Id";
    private static final String NBT_NAME = "Name";
    private static final String NBT_COLOR = "Color";

    public CategoryRef {
        name = name == null ? "" : name;
        color = color == null ? DLColor.TRANSPARENT : color;
    }

    /** The given category as a value, or {@link #NONE} if there is none. */
    public static CategoryRef of(TrainCategory category) {
        return category == null ? NONE : new CategoryRef(category.getId(), category.getCategoryName(), category.getColor());
    }

    /** Whether this refers to a category at all. */
    public boolean isKnown() {
        return id != null;
    }

    /** Whether this category carries a usable name. */
    public boolean hasName() {
        return !name.isBlank();
    }

    /** Serializes this category. */
    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        if (id != null) {
            nbt.putUUID(NBT_ID, id);
        }
        nbt.putString(NBT_NAME, name);
        nbt.putInt(NBT_COLOR, color.getAsARGB());
        return nbt;
    }

    /** Deserializes a category written by {@link #toNbt()}. */
    public static CategoryRef fromNbt(CompoundTag nbt) {
        if (nbt == null || nbt.isEmpty() || !nbt.hasUUID(NBT_ID)) {
            return NONE;
        }
        return new CategoryRef(nbt.getUUID(NBT_ID), nbt.getString(NBT_NAME), DLColor.fromInt(nbt.getInt(NBT_COLOR)));
    }

    @Override
    public String toString() {
        return name;
    }
}
