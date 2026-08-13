package de.mrjulsen.crn.api.core.ref;

import java.util.UUID;

import de.mrjulsen.crn.data.settings.TrainCategory;
import de.mrjulsen.mcdragonlib.util.DLColor;
import net.minecraft.nbt.CompoundTag;

/**
 * A train category as referred to from elsewhere in the API. Holds only what is needed to identify
 * and display the category, so it stays valid even if the category is edited afterwards. Can be
 * written to NBT and read back.
 *
 * @param id    The category's id, or {@code null} if this refers to no category.
 * @param name  The category's name, never {@code null} but possibly empty.
 * @param color The category's colour, transparent if none was configured.
 */
public record TrainCategoryRef(UUID id, String name, DLColor color) {

    /** Stands for "no category". {@link #isKnown()} is false. */
    public static final TrainCategoryRef NONE = new TrainCategoryRef(null, "", DLColor.TRANSPARENT);

    private static final String NBT_ID = "Id";
    private static final String NBT_NAME = "Name";
    private static final String NBT_COLOR = "Color";

    public TrainCategoryRef {
        name = name == null ? "" : name;
        color = color == null ? DLColor.TRANSPARENT : color;
    }

    public static TrainCategoryRef of(TrainCategory category) {
        return category == null ? NONE : new TrainCategoryRef(category.getId(), category.getCategoryName(), category.getColor());
    }

    /** Whether this refers to a category at all, as opposed to being {@link #NONE}. */
    public boolean isKnown() {
        return id != null;
    }

    public boolean hasName() {
        return !name.isBlank();
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        if (id != null) {
            nbt.putUUID(NBT_ID, id);
        }
        nbt.putString(NBT_NAME, name);
        nbt.putInt(NBT_COLOR, color.getAsARGB());
        return nbt;
    }

    public static TrainCategoryRef fromNbt(CompoundTag nbt) {
        if (nbt == null || nbt.isEmpty() || !nbt.hasUUID(NBT_ID)) {
            return NONE;
        }
        return new TrainCategoryRef(nbt.getUUID(NBT_ID), nbt.getString(NBT_NAME), DLColor.fromInt(nbt.getInt(NBT_COLOR)));
    }

    @Override
    public String toString() {
        return name;
    }
}
