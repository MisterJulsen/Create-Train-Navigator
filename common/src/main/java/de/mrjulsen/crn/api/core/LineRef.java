package de.mrjulsen.crn.api.core;

import java.util.UUID;

import de.mrjulsen.crn.data.settings.TrainLine;
import de.mrjulsen.mcdragonlib.util.DLColor;
import net.minecraft.nbt.CompoundTag;

/**
 * A train line as referred to from elsewhere in the API. Holds only what is needed to identify and
 * display the line, so it stays valid even if the line is edited afterwards. Can be written to NBT
 * and read back.
 *
 * @param id    The line's id, or {@code null} if this refers to no line.
 * @param name  The line's name, never {@code null} but possibly empty.
 * @param color The line's colour, transparent if none was configured.
 */
public record LineRef(UUID id, String name, DLColor color) {

    /** Stands for "no line". {@link #isKnown()} is false. */
    public static final LineRef NONE = new LineRef(null, "", DLColor.TRANSPARENT);

    private static final String NBT_ID = "Id";
    private static final String NBT_NAME = "Name";
    private static final String NBT_COLOR = "Color";

    public LineRef {
        name = name == null ? "" : name;
        color = color == null ? DLColor.TRANSPARENT : color;
    }

    public static LineRef of(TrainLine line) {
        return line == null ? NONE : new LineRef(line.getId(), line.getLineName(), line.getColor());
    }

    /** Whether this refers to a line at all, as opposed to being {@link #NONE}. */
    public boolean isKnown() {
        return id != null;
    }

    public boolean hasName() {
        return !name.isBlank();
    }

    /**
     * The line's name, or the given fallback where it has none. What a train is called to
     * travellers: the line it works, and only failing that its own name.
     */
    public String nameOr(String fallback) {
        return hasName() ? name : fallback;
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

    public static LineRef fromNbt(CompoundTag nbt) {
        if (nbt == null || nbt.isEmpty() || !nbt.hasUUID(NBT_ID)) {
            return NONE;
        }
        return new LineRef(nbt.getUUID(NBT_ID), nbt.getString(NBT_NAME), DLColor.fromInt(nbt.getInt(NBT_COLOR)));
    }

    @Override
    public String toString() {
        return name;
    }
}
