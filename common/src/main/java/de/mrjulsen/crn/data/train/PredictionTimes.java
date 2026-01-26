package de.mrjulsen.crn.data.train;

import java.util.Objects;

import net.minecraft.nbt.CompoundTag;

public class PredictionTimes {

    public static record DepartureTime(long defaultDepartureTime, long minDepartureTime) {}

    private static final String NBT_REFRESHED = "Refreshed";
    private static final String NBT_ARRIVAL = "Arrival";
    private static final String NBT_DEPARTURE = "Departure";
    private static final String NBT_MIN_DEPARTURE = "MinDeparture";

    private final TrainPrediction prediction;

    private long refreshTime;
    private long arrivalTime;
    private long departureTime;
    private long minDepartureTime;

    public PredictionTimes(TrainPrediction prediction, long refreshTime, long arrivalTime, long departureTime, long minDepartureTime) {
        this.prediction = prediction;
        this.set(refreshTime, arrivalTime, departureTime, minDepartureTime);
    }

    void set(long refreshTime, long arrivalTime, long departureTime, long minDepartureTime) {
        this.refreshTime = refreshTime;
        this.arrivalTime = Math.max(this.refreshTime, arrivalTime);
        this.departureTime = Math.max(this.arrivalTime, departureTime);
        this.minDepartureTime = Math.max(this.arrivalTime, minDepartureTime);
    }

    void shift(long amount, boolean updateRefreshTime) {            
        if (updateRefreshTime) {
            this.refreshTime += amount;
        }
        this.arrivalTime += amount;
        DepartureTime dt = TrainPrediction.estimateDepartures(prediction.getData().getTrain(), prediction.getEntryIndex(), arrivalTime);
        this.departureTime = dt.defaultDepartureTime();
        this.minDepartureTime = dt.minDepartureTime();
    }

    public long refreshTime() {
        return refreshTime;
    }

    public long arrivalTime() {
        return arrivalTime;
    }

    /** The default Departure Time of the train, according to the Schedule. */
    public long defaultDepartureTime() {
        return departureTime;
    }

    /** The actual Departure Time of the train, that was calculated based on all other values. This value is used for all calculations. */
    public long departureTime() {
        if (minStayDuration() < stayDuration() && prediction.scheduled() != null && prediction.scheduled() != this && prediction.getData().isInitialized()) {
            return Math.max(minDepartureTime(), prediction.scheduled().defaultDepartureTime());
        }
        return defaultDepartureTime();
    }

    /** The earliest possible Departure Time, according to the Schedule. */
    public long minDepartureTime() {
        return minDepartureTime;
    }

    public long arrivalIn() {
        return arrivalTime() - refreshTime();
    }

    public long departureIn() {
        return departureTime() - refreshTime();
    }

    public long minDepartureIn() {
        return minDepartureTime() - refreshTime();
    }

    public long stayDuration() {
        return Math.max(0, defaultDepartureTime() - arrivalTime());
    }

    public long minStayDuration() {
        return Math.max(0, minDepartureTime() - arrivalTime());
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putLong(NBT_REFRESHED, refreshTime);
        nbt.putLong(NBT_ARRIVAL, arrivalTime);
        nbt.putLong(NBT_DEPARTURE, departureTime);
        nbt.putLong(NBT_MIN_DEPARTURE, minDepartureTime);
        return nbt;
    }

    public static PredictionTimes fromNbt(TrainPrediction prediction, CompoundTag nbt) {
        long refreshTime = nbt.getLong(NBT_REFRESHED);
        long arrivalTime = nbt.getLong(NBT_ARRIVAL);
        long departureTime = nbt.getLong(NBT_DEPARTURE);
        long minDepartureTime = nbt.getLong(NBT_MIN_DEPARTURE);
        PredictionTimes t = new PredictionTimes(prediction, refreshTime, arrivalTime, departureTime, minDepartureTime);
        return t;
    }

    @Override
    public final boolean equals(Object other) {
        if (other instanceof PredictionTimes o) {
            return refreshTime() == o.refreshTime() && arrivalTime() == o.arrivalTime();
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return 31 * Objects.hash(refreshTime(), arrivalTime());
    }

    @Override
    public final String toString() {
        return String.format("PredictionTimes[R: %s, A: %s, D: %s, At: %s, Dt: %s, d: %s]", refreshTime(), arrivalTime(), departureTime(), arrivalIn(), departureIn(), stayDuration());
    }
}
