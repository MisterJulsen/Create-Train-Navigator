package de.mrjulsen.crn.backend.delay;

import net.minecraft.nbt.CompoundTag;

/**
 * One value filling a placeholder in a delay cause's message, tagged with what kind of value it is.
 * <p>
 * The tag exists so a consumer can render the value in its own way - a duration as a formatted time
 * span, a station name as a link - without having to know which cause produced it or in which order
 * that cause happens to emit its arguments. A consumer that does not recognise the type can always
 * fall back to {@link #value()}.
 *
 * @param type  What kind of value this is.
 * @param value The printable representation. Never {@code null}.
 */
public record DelayArgument(DelayArgumentType type, String value) {

    private static final String NBT_TYPE = "Type";
    private static final String NBT_VALUE = "Value";

    public DelayArgument {
        type = type == null ? DelayArgumentType.TEXT : type;
        value = value == null ? "" : value;
    }

    /** Free text that needs no special treatment. */
    public static DelayArgument text(String value) {
        return new DelayArgument(DelayArgumentType.TEXT, value);
    }

    /** The name of a train. */
    public static DelayArgument trainName(String trainName) {
        return new DelayArgument(DelayArgumentType.TRAIN_NAME, trainName);
    }

    /** The name of a station. */
    public static DelayArgument stationName(String stationName) {
        return new DelayArgument(DelayArgumentType.STATION_NAME, stationName);
    }

    /** The name of a train line. */
    public static DelayArgument lineName(String lineName) {
        return new DelayArgument(DelayArgumentType.LINE_NAME, lineName);
    }

    /** A duration in game ticks, to be rendered as a time span. */
    public static DelayArgument duration(long ticks) {
        return new DelayArgument(DelayArgumentType.DURATION_TICKS, Long.toString(ticks));
    }

    /** A plain number. */
    public static DelayArgument number(long number) {
        return new DelayArgument(DelayArgumentType.NUMBER, Long.toString(number));
    }

    /**
     * This argument's value as a number.
     *
     * @return The parsed value, or {@code fallback} if it does not represent a number.
     */
    public long asLong(long fallback) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Serializes this argument. */
    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString(NBT_TYPE, type.name());
        nbt.putString(NBT_VALUE, value);
        return nbt;
    }

    /** Deserializes an argument written by {@link #toNbt()}, defaulting to text on unknown types. */
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
