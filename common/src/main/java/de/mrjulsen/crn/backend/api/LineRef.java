package de.mrjulsen.crn.backend.api;

import java.util.UUID;

import de.mrjulsen.crn.data.TrainLine;
import de.mrjulsen.mcdragonlib.util.DLColor;
import net.minecraft.nbt.CompoundTag;

/**
 * A train line as this API refers to it: the identity and the two properties anything displaying a
 * line actually needs, copied out of the mutable {@link TrainLine} it was taken from.
 * <p>
 * A snapshot is a moment in time, so the line it names is one too. Editing the line afterwards does
 * not change a snapshot that already went out - re-query to see the new name or colour.
 *
 * @param id    The line's id.
 * @param name  The line's name. May be empty.
 * @param color The line's colour.
 */
public record LineRef(UUID id, String name, DLColor color) {

    /** No line, for a train or section that carries none. */
    public static final LineRef NONE = new LineRef(null, "", DLColor.TRANSPARENT);

    private static final String NBT_ID = "Id";
    private static final String NBT_NAME = "Name";
    private static final String NBT_COLOR = "Color";

    public LineRef {
        name = name == null ? "" : name;
        color = color == null ? DLColor.TRANSPARENT : color;
    }

    /** The given line as a value, or {@link #NONE} if there is none. */
    public static LineRef of(TrainLine line) {
        return line == null ? NONE : new LineRef(line.getId(), line.getLineName(), line.getColor());
    }

    /** Whether this refers to a line at all. */
    public boolean isKnown() {
        return id != null;
    }

    /** Whether this line carries a usable name. */
    public boolean hasName() {
        return !name.isBlank();
    }

    /** Serializes this line. */
    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        if (id != null) {
            nbt.putUUID(NBT_ID, id);
        }
        nbt.putString(NBT_NAME, name);
        nbt.putInt(NBT_COLOR, color.getAsARGB());
        return nbt;
    }

    /** Deserializes a line written by {@link #toNbt()}. */
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
