package de.mrjulsen.crn.core.timing;

import net.minecraft.nbt.CompoundTag;

/**
 * The arrival and departure times of one call, in game ticks on the backend's time base. Used both
 * for the timetabled times and for the projected ones; their difference is the delay.
 *
 * @param arrival      When the train arrives, or a negative value if the times are not known.
 * @param departure    When the train departs.
 * @param minDeparture The earliest the train may depart; the gap up to {@code departure} is buffer
 *                     time it can give up to make up delay.
 */
public record StopTimes(long arrival, long departure, long minDeparture) {

    /** Stands for times that are not known. */
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

    /** Whether these times are known at all. */
    public boolean isKnown() {
        return arrival >= 0;
    }

    /** How long the train stands here, in ticks. */
    public long stayDuration() {
        return isKnown() ? Math.max(0, departure - arrival) : 0;
    }

    /** The least time the train must stand here, in ticks. */
    public long minStayDuration() {
        return isKnown() ? Math.max(0, minDeparture - arrival) : 0;
    }

    /** The spare standing time that could be given up to make up delay, in ticks. */
    public long bufferTime() {
        return Math.max(0, stayDuration() - minStayDuration());
    }

    /** How long until arrival from the given time, in ticks. */
    public long arrivalIn(long now) {
        return arrival - now;
    }

    /** How long until departure from the given time, in ticks. */
    public long departureIn(long now) {
        return departure - now;
    }

    /** How long until the earliest departure from the given time, in ticks. */
    public long minDepartureIn(long now) {
        return minDeparture - now;
    }

    /** The same times moved by the given number of ticks. */
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
