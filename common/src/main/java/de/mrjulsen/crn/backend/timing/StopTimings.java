package de.mrjulsen.crn.backend.timing;

import de.mrjulsen.crn.backend.util.FrequencyStringSelector;
import de.mrjulsen.crn.config.ModCommonConfig;
import net.minecraft.nbt.CompoundTag;

/**
 * All timing data the backend maintains for one stop: the learned leg and dwell durations, the
 * scheduled times, the current projection and the measured times of the most recent visit.
 * All timestamps are transformed game ticks.
 */
public final class StopTimings {

    private static final String NBT_LEG = "Leg";
    private static final String NBT_DWELL = "Dwell";
    private static final String NBT_SCHEDULED = "Scheduled";
    private static final String NBT_LAST_ARRIVAL = "LastArrival";
    private static final String NBT_LAST_DEPARTURE = "LastDeparture";
    private static final String NBT_VISITS = "Visits";
    private static final String NBT_STATION = "Station";
    private static final String NBT_STATION_GUESS = "StationGuess";

    /** How many recent visits feed the station guess of an ambiguous stop. */
    private static final int STATION_GUESS_WINDOW = 10;

    private final int entryIndex;

    /** Learned transit duration from the previous stop to this stop. */
    private final MedianDurationTracker legDuration;
    private long dwellDuration = 0;

    /**
     * Tracks which concrete station this stop most often resolved to, so a likely one can be shown
     * for an ambiguous destination before the train has committed. Only relevant while the filter
     * is not itself a concrete station.
     */
    private final FrequencyStringSelector stationGuess = new FrequencyStringSelector(STATION_GUESS_WINDOW);

    private volatile StopTimes scheduled = StopTimes.UNKNOWN;
    private volatile StopTimes realtime = StopTimes.UNKNOWN;

    /**
     * The nominal projection for this stop: the times the train would have with zero carried-over
     * delay, chained from the previous stop's nominal departure. Used as the anchor on a soft
     * reset, so a flexible stop keeps its catch-up buffer and the anchored legs stay consistent
     * with the learned ones. Recomputed by every projection.
     */
    private volatile StopTimes nominalTimes = StopTimes.UNKNOWN;

    /** Times of the previous, completed visit. */
    private volatile StopTimes previousScheduled = StopTimes.UNKNOWN;
    private volatile StopTimes previousRealtime = StopTimes.UNKNOWN;

    private volatile long lastActualArrival = -1;
    private volatile long lastActualDeparture = -1;
    private volatile int completedVisits = 0;

    public StopTimings(int entryIndex) {
        this.entryIndex = entryIndex;
        this.legDuration = new MedianDurationTracker(ModCommonConfig.TOTAL_DURATION_BUFFER_SIZE.get(), ModCommonConfig.TOTAL_DURATION_DEVIATION_THRESHOLD.get());
    }

    /** The schedule entry index of the stop this data belongs to. */
    public int getEntryIndex() {
        return entryIndex;
    }

    /** The learned transit duration from the previous stop to this one. */
    public MedianDurationTracker legDuration() {
        return legDuration;
    }

    /** The dwell duration in ticks measured on the most recent visit. */
    public long dwellDuration() {
        return dwellDuration;
    }

    /** The timetable times, {@link StopTimes#UNKNOWN} until one has been anchored. */
    public StopTimes getScheduled() {
        return scheduled;
    }

    /** The current projected times. */
    public StopTimes getRealtime() {
        return realtime;
    }

    /**
     * The scheduled times as they should be published. While no timetable has been anchored yet the
     * projection stands in, so consumers never see an unknown scheduled time next to a known
     * projected one and the train simply reports as running on time.
     * <p>
     * For output only. The projection keeps using {@link #getScheduled()}, which stays unknown
     * until a real timetable exists, so this fallback can never be learned as one.
     */
    public StopTimes getPublishedScheduled() {
        return scheduled.isKnown() ? scheduled : realtime;
    }

    /** The timetable times of the previous, completed visit. */
    public StopTimes getPreviousScheduled() {
        return previousScheduled;
    }

    /** The measured times of the previous, completed visit. */
    public StopTimes getPreviousRealtime() {
        return previousRealtime;
    }

    /** The measured arrival time of the current visit, or {@code -1} if the train is not here. */
    public long getLastActualArrival() {
        return lastActualArrival;
    }

    /** The measured departure time of the most recent visit, or {@code -1}. */
    public long getLastActualDeparture() {
        return lastActualDeparture;
    }

    /** How often the train has departed from this stop while being tracked. */
    public int getCompletedVisits() {
        return completedVisits;
    }

    /** Sets the current projected times. */
    public void setRealtime(StopTimes realtime) {
        this.realtime = realtime;
    }

    /** Sets the nominal projection of the current update. */
    public void setNominalTimes(StopTimes nominalTimes) {
        this.nominalTimes = nominalTimes;
    }

    /** Overwrites the timetable times. */
    public void setScheduled(StopTimes scheduled) {
        this.scheduled = scheduled;
    }

    /** Whether both timetable and projected times are available, so a deviation is meaningful. */
    public boolean isComparable() {
        return scheduled.isKnown() && realtime.isKnown();
    }

    /** Positive when the train arrives later than scheduled. */
    public long getArrivalDeviation() {
        return isComparable() ? realtime.arrival() - scheduled.arrival() : 0;
    }

    /** Positive when the train departs later than scheduled. */
    public long getDepartureDeviation() {
        return isComparable() ? realtime.departure() - scheduled.departure() : 0;
    }

    /** The larger of the arrival and departure deviation, in ticks. */
    public long getMaxDeviation() {
        return Math.max(getArrivalDeviation(), getDepartureDeviation());
    }

    /** Whether the arrival deviation exceeds the given threshold. */
    public boolean isArrivalDelayed(long thresholdTicks) {
        return getArrivalDeviation() > thresholdTicks;
    }

    /** Whether the departure deviation exceeds the given threshold. */
    public boolean isDepartureDelayed(long thresholdTicks) {
        return getDepartureDeviation() > thresholdTicks;
    }

    /** Whether either deviation exceeds the given threshold. */
    public boolean isDelayed(long thresholdTicks) {
        return getMaxDeviation() > thresholdTicks;
    }

    /**
     * Records the actual arrival at this stop.
     *
     * @param countMeasurement Whether the measured leg duration may become a learned reference.
     */
    public void recordArrival(long time, int measuredLegTicks, boolean countMeasurement) {
        this.lastActualArrival = time;
        if (countMeasurement) {
            legDuration.record(measuredLegTicks);
        }
    }

    /** Remembers the concrete station this stop resolved to on this visit. */
    public void recordVisitedStation(String stationName) {
        stationGuess.add(stationName);
    }

    /** The most likely concrete station for this stop, or {@code null} if none has been seen. */
    public String getEstimatedStationName() {
        return stationGuess.getPrimary();
    }

    /** Records the actual departure and moves the current times into the previous-visit slots. */
    public synchronized void recordDeparture(long time, long measuredDwellTicks) {
        this.lastActualDeparture = time;
        this.dwellDuration = measuredDwellTicks;

        this.previousScheduled = scheduled;
        this.previousRealtime = new StopTimes(lastActualArrival >= 0 ? lastActualArrival : time, time, time);
        this.completedVisits++;
        this.lastActualArrival = -1;
    }

    /** Moves the timetable times one cycle ahead, for cyclic schedules. */
    public void advanceScheduledCycle(long cycleDuration) {
        if (scheduled.isKnown() && cycleDuration > 0) {
            this.scheduled = scheduled.shifted(cycleDuration);
        }
    }

    /**
     * Moves the projection one cycle ahead as well, keeping it in sync with
     * {@link #advanceScheduledCycle(long)} until the next full update recomputes it. Without this
     * the stale value of the completed visit would be compared against the advanced timetable and
     * report a deviation of roughly one negative cycle.
     */
    public void advanceRealtimeCycle(long cycleDuration) {
        if (realtime.isKnown() && cycleDuration > 0) {
            this.realtime = realtime.shifted(cycleDuration);
        }
    }

    /**
     * Resets the timetable of this stop to the nominal projection, i.e. the times with zero
     * carried-over delay. This preserves a flexible stop's catch-up buffer, which anchoring the
     * shortened projection would collapse into a permanent phantom delay, and keeps the anchored
     * legs consistent with the learned ones. Falls back to the projection if no nominal one exists.
     */
    public void anchorScheduleToRealtime() {
        if (nominalTimes.isKnown()) {
            this.scheduled = nominalTimes;
        } else if (realtime.isKnown()) {
            this.scheduled = realtime;
        }
    }

    /** Shifts all absolute timestamps by the given amount, after a world time jump. */
    public synchronized void shiftTimes(long ticks) {
        this.scheduled = scheduled.shifted(ticks);
        this.realtime = realtime.shifted(ticks);
        this.previousScheduled = previousScheduled.shifted(ticks);
        this.previousRealtime = previousRealtime.shifted(ticks);
        if (lastActualArrival >= 0) lastActualArrival += ticks;
        if (lastActualDeparture >= 0) lastActualDeparture += ticks;
        this.nominalTimes = nominalTimes.shifted(ticks);
    }

    /**
     * Discards everything describing a concrete run of this stop while keeping what was learned.
     * Used when a train returns to service: its old times refer to the run that was interrupted and
     * would otherwise project forward as an enormous delay, but its durations have not changed.
     */
    public synchronized void resetRuntimeData() {
        scheduled = StopTimes.UNKNOWN;
        realtime = StopTimes.UNKNOWN;
        nominalTimes = StopTimes.UNKNOWN;
        previousScheduled = StopTimes.UNKNOWN;
        previousRealtime = StopTimes.UNKNOWN;
        lastActualArrival = -1;
        lastActualDeparture = -1;
    }

    /** Serializes this data, storing the given resolved station name alongside it. */
    public CompoundTag toNbt(String stationName) {
        CompoundTag nbt = new CompoundTag();
        nbt.put(NBT_LEG, legDuration.toNbt());
        nbt.putLong(NBT_DWELL, dwellDuration);
        if (scheduled.isKnown()) nbt.put(NBT_SCHEDULED, scheduled.toNbt());
        nbt.putLong(NBT_LAST_ARRIVAL, lastActualArrival);
        nbt.putLong(NBT_LAST_DEPARTURE, lastActualDeparture);
        nbt.putInt(NBT_VISITS, completedVisits);
        if (stationName != null) nbt.putString(NBT_STATION, stationName);
        nbt.put(NBT_STATION_GUESS, stationGuess.toNbt());
        return nbt;
    }

    /** Restores persisted data. */
    public void loadNbt(CompoundTag nbt) {
        legDuration.loadNbt(nbt.getCompound(NBT_LEG));
        this.dwellDuration = nbt.getLong(NBT_DWELL);
        if (nbt.contains(NBT_SCHEDULED)) {
            this.scheduled = StopTimes.fromNbt(nbt.getCompound(NBT_SCHEDULED));
        }
        this.lastActualArrival = nbt.getLong(NBT_LAST_ARRIVAL);
        this.lastActualDeparture = nbt.getLong(NBT_LAST_DEPARTURE);
        this.completedVisits = nbt.getInt(NBT_VISITS);
        if (nbt.contains(NBT_STATION_GUESS)) {
            stationGuess.loadNbt(nbt.getCompound(NBT_STATION_GUESS));
        }
    }

    /** Reads the resolved station name stored by {@link #toNbt(String)}. */
    public static String loadStationName(CompoundTag nbt) {
        return nbt.getString(NBT_STATION);
    }
}
