package de.mrjulsen.crn.core.delay;

import net.minecraft.nbt.CompoundTag;

/**
 * One value filled into a delay reason's description, such as a blocking train's name or a length of
 * time. The type tells the display how to format the value.
 *
 * @param type  What kind of value this is.
 * @param value The value itself, held as text.
 */
public record DelayArgument(DelayArgumentType type, String value) {

    private static final String NBT_TYPE = "Type";
    private static final String NBT_VALUE = "Value";

    public DelayArgument {
        type = type == null ? DelayArgumentType.TEXT : type;
        value = value == null ? "" : value;
    }

    /** An argument holding plain text. */
    public static DelayArgument text(String value) {
        return new DelayArgument(DelayArgumentType.TEXT, value);
    }

    /** An argument holding a train's name. */
    public static DelayArgument trainName(String trainName) {
        return new DelayArgument(DelayArgumentType.TRAIN_NAME, trainName);
    }

    /** An argument holding a station's name. */
    public static DelayArgument stationName(String stationName) {
        return new DelayArgument(DelayArgumentType.STATION_NAME, stationName);
    }

    /** An argument holding a line's name. */
    public static DelayArgument lineName(String lineName) {
        return new DelayArgument(DelayArgumentType.LINE_NAME, lineName);
    }

    /** An argument holding a duration, in ticks. */
    public static DelayArgument duration(long ticks) {
        return new DelayArgument(DelayArgumentType.DURATION_TICKS, Long.toString(ticks));
    }

    /** An argument holding a number. */
    public static DelayArgument number(long number) {
        return new DelayArgument(DelayArgumentType.NUMBER, Long.toString(number));
    }

    /** The value as a number, or the given fallback where it is not one. */
    public long asLong(long fallback) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString(NBT_TYPE, type.name());
        nbt.putString(NBT_VALUE, value);
        return nbt;
    }

    public static DelayArgument fromNbt(CompoundTag nbt) {
        DelayArgumentType type;
        try {
            type = DelayArgumentType.valueOf(nbt.getString(NBT_TYPE));
        } catch (IllegalArgumentException e) {
            type = DelayArgumentType.TEXT;
        }
        return new DelayArgument(type, nbt.getString(NBT_VALUE));
    }
}
