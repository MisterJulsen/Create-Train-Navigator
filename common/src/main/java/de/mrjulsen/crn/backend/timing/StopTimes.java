package de.mrjulsen.crn.backend.timing;

import net.minecraft.nbt.CompoundTag;

/**
 * An immutable set of times for one stop of a train, in transformed game ticks.
 *
 * @param arrival      The time at which the train arrives at the stop.
 * @param departure    The regular departure time, with all wait conditions fulfilled normally.
 * @param minDeparture The earliest possible departure time, if the waits can be shortened to catch
 *                     up a delay.
 */
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

    /** Whether these times have been determined at all. */
    public boolean isKnown() {
        return arrival >= 0;
    }

    /** How long the train stays at this stop under normal conditions, in ticks. */
    public long stayDuration() {
        return isKnown() ? Math.max(0, departure - arrival) : 0;
    }

    /** The shortest possible stay at this stop, in ticks. */
    public long minStayDuration() {
        return isKnown() ? Math.max(0, minDeparture - arrival) : 0;
    }

    /** The time this stop can give up to catch up a delay, in ticks. */
    public long bufferTime() {
        return Math.max(0, stayDuration() - minStayDuration());
    }

    /** Ticks until the arrival, negative once it has passed. */
    public long arrivalIn(long now) {
        return arrival - now;
    }

    /** Ticks until the departure, negative once it has passed. */
    public long departureIn(long now) {
        return departure - now;
    }

    /** Ticks until the earliest possible departure, negative once it has passed. */
    public long minDepartureIn(long now) {
        return minDeparture - now;
    }

    /** A copy with all times shifted by the given amount of ticks. */
    public StopTimes shifted(long ticks) {
        return isKnown() ? new StopTimes(arrival + ticks, departure + ticks, minDeparture + ticks) : this;
    }

    /** Serializes these times. */
    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putLong(NBT_ARRIVAL, arrival);
        nbt.putLong(NBT_DEPARTURE, departure);
        nbt.putLong(NBT_MIN_DEPARTURE, minDeparture);
        return nbt;
    }

    /** Deserializes times written by {@link #toNbt()}. */
    public static StopTimes fromNbt(CompoundTag nbt) {
        return new StopTimes(
                nbt.getLong(NBT_ARRIVAL),
                nbt.getLong(NBT_DEPARTURE),
                nbt.getLong(NBT_MIN_DEPARTURE)
        );
    }
}