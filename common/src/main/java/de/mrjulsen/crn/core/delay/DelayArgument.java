package de.mrjulsen.crn.core.delay;

import net.minecraft.nbt.CompoundTag;

public record DelayArgument(DelayArgumentType type, String value) {

    private static final String NBT_TYPE = "Type";
    private static final String NBT_VALUE = "Value";

    public DelayArgument {
        type = type == null ? DelayArgumentType.TEXT : type;
        value = value == null ? "" : value;
    }

    public static DelayArgument text(String value) {
        return new DelayArgument(DelayArgumentType.TEXT, value);
    }

    public static DelayArgument trainName(String trainName) {
        return new DelayArgument(DelayArgumentType.TRAIN_NAME, trainName);
    }

    public static DelayArgument stationName(String stationName) {
        return new DelayArgument(DelayArgumentType.STATION_NAME, stationName);
    }

    public static DelayArgument lineName(String lineName) {
        return new DelayArgument(DelayArgumentType.LINE_NAME, lineName);
    }

    public static DelayArgument duration(long ticks) {
        return new DelayArgument(DelayArgumentType.DURATION_TICKS, Long.toString(ticks));
    }

    public static DelayArgument number(long number) {
        return new DelayArgument(DelayArgumentType.NUMBER, Long.toString(number));
    }

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
