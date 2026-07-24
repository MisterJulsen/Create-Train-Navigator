package de.mrjulsen.crn.core.timing;

import net.minecraft.nbt.CompoundTag;

public record StopTimes(long arrival, long departure, long minDeparture) {

    public static final StopTimes UNKNOWN = new StopTimes(-1, -1, -1);

    private static final String NBT_ARRIVAL = "Arrival";
    private static final String NBT_DEPARTURE = "Departure";
    private static final String NBT_MIN_DEPARTURE = "MinDeparture";

    public StopTimes {
        if (arrival >= 0) {
            departure = Math.max(arrival, departure);
            minDeparture = Math.max(arrival, minDeparture);
        }
    }

    public boolean isKnown() {
        return arrival >= 0;
    }

    public long stayDuration() {
        return isKnown() ? Math.max(0, departure - arrival) : 0;
    }

    public long minStayDuration() {
        return isKnown() ? Math.max(0, minDeparture - arrival) : 0;
    }

    public long bufferTime() {
        return Math.max(0, stayDuration() - minStayDuration());
    }

    public long arrivalIn(long now) {
        return arrival - now;
    }

    public long departureIn(long now) {
        return departure - now;
    }

    public long minDepartureIn(long now) {
        return minDeparture - now;
    }

    public StopTimes shifted(long ticks) {
        return isKnown() ? new StopTimes(arrival + ticks, departure + ticks, minDeparture + ticks) : this;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putLong(NBT_ARRIVAL, arrival);
        nbt.putLong(NBT_DEPARTURE, departure);
        nbt.putLong(NBT_MIN_DEPARTURE, minDeparture);
        return nbt;
    }

    public static StopTimes fromNbt(CompoundTag nbt) {
        return new StopTimes(
                nbt.getLong(NBT_ARRIVAL),
                nbt.getLong(NBT_DEPARTURE),
                nbt.getLong(NBT_MIN_DEPARTURE)
        );
    }
}