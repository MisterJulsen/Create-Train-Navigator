package de.mrjulsen.crn.backend.timing;

import de.mrjulsen.crn.config.ModCommonConfig;
import net.minecraft.nbt.CompoundTag;

/**
 * All timing data the backend maintains for one stop of a train:
 * <ul>
 *   <li>the learned duration of the leg leading to this stop,</li>
 *   <li>the learned dwell (stay) duration at this stop,</li>
 *   <li>the scheduled times (the "timetable"),</li>
 *   <li>the current real-time estimates,</li>
 *   <li>the actually measured times of the most recent visit.</li>
 * </ul>
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

    private final int entryIndex;

    /** Learned transit duration from the previous stop to this stop. */
    private final MedianDurationTracker legDuration;
    private long dwellDuration = 0;

    private volatile StopTimes scheduled = StopTimes.UNKNOWN;
    private volatile StopTimes realtime = StopTimes.UNKNOWN;

    /**
     * The nominal timetable projection for this stop: the times the train would have with zero
     * carried-over delay (full wait, no catch-up shortening), chained from the previous stop's
     * nominal departure. Used as the anchor value on a soft reset so that (a) a flexible stop's
     * catch-up buffer is not baked away and (b) the anchored legs stay consistent with the learned
     * legs (scheduled leg == real leg), instead of leaving a buffer-sized phantom delay per leg.
     * Transient (recomputed every projection), {@code UNKNOWN} until first set.
     */
    private volatile StopTimes nominalTimes = StopTimes.UNKNOWN;

    /* Times of the previous (completed) visit, for history purposes. */
    private volatile StopTimes previousScheduled = StopTimes.UNKNOWN;
    private volatile StopTimes previousRealtime = StopTimes.UNKNOWN;

    private volatile long lastActualArrival = -1;
    private volatile long lastActualDeparture = -1;
    private volatile int completedVisits = 0;

    public StopTimings(int entryIndex) {
        this.entryIndex = entryIndex;
        this.legDuration = new MedianDurationTracker(ModCommonConfig.TOTAL_DURATION_BUFFER_SIZE.get(), ModCommonConfig.TOTAL_DURATION_DEVIATION_THRESHOLD.get());
    }

    public int getEntryIndex() {
        return entryIndex;
    }

    public MedianDurationTracker legDuration() {
        return legDuration;
    }

    public long dwellDuration() {
        return dwellDuration;
    }

    public StopTimes getScheduled() {
        return scheduled;
    }

    public StopTimes getRealtime() {
        return realtime;
    }

    public StopTimes getPreviousScheduled() {
        return previousScheduled;
    }

    public StopTimes getPreviousRealtime() {
        return previousRealtime;
    }

    /** The measured arrival time of the current/most recent visit, or {@code -1}. */
    public long getLastActualArrival() {
        return lastActualArrival;
    }

    /** The measured departure time of the most recent visit, or {@code -1}. */
    public long getLastActualDeparture() {
        return lastActualDeparture;
    }

    /** How often the train has completed (departed from) this stop while being tracked. */
    public int getCompletedVisits() {
        return completedVisits;
    }

    public void setRealtime(StopTimes realtime) {
        this.realtime = realtime;
    }

    /** Sets the nominal timetable projection of the current update (see {@link #nominalTimes}). */
    public void setNominalTimes(StopTimes nominalTimes) {
        this.nominalTimes = nominalTimes;
    }

    public void setScheduled(StopTimes scheduled) {
        this.scheduled = scheduled;
    }

    /** Whether both scheduled and real-time data is available for this stop. */
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

    public long getMaxDeviation() {
        return Math.max(getArrivalDeviation(), getDepartureDeviation());
    }

    public boolean isArrivalDelayed(long thresholdTicks) {
        return getArrivalDeviation() > thresholdTicks;
    }

    public boolean isDepartureDelayed(long thresholdTicks) {
        return getDepartureDeviation() > thresholdTicks;
    }

    public boolean isDelayed(long thresholdTicks) {
        return getMaxDeviation() > thresholdTicks;
    }

    /** Records the actual arrival at this stop. */
    public void recordArrival(long time, int measuredLegTicks, boolean countMeasurement) {
        this.lastActualArrival = time;
        if (countMeasurement) {
            legDuration.record(measuredLegTicks);
        }
    }

    /**
     * Records the actual departure from this stop and moves the current times into the
     * "previous visit" slots.
     */
    public synchronized void recordDeparture(long time, long measuredDwellTicks) {
        this.lastActualDeparture = time;
        this.dwellDuration = measuredDwellTicks;

        this.previousScheduled = scheduled;
        this.previousRealtime = new StopTimes(lastActualArrival >= 0 ? lastActualArrival : time, time, time);
        this.completedVisits++;
        this.lastActualArrival = -1;
    }

    /** Moves the scheduled times one cycle ahead (for cyclic schedules). */
    public void advanceScheduledCycle(long cycleDuration) {
        if (scheduled.isKnown() && cycleDuration > 0) {
            this.scheduled = scheduled.shifted(cycleDuration);
        }
    }

    /**
     * Moves the real-time estimate one cycle ahead as well, so it stays in sync with
     * {@link #advanceScheduledCycle(long)} until the next full update recomputes the
     * authoritative projection for the new cycle. Without this, the stale real-time value of
     * the just-completed visit would be compared against the already-advanced schedule and
     * report a deviation of roughly {@code -cycleDuration}.
     */
    public void advanceRealtimeCycle(long cycleDuration) {
        if (realtime.isKnown() && cycleDuration > 0) {
            this.realtime = realtime.shifted(cycleDuration);
        }
    }

    /**
     * Resets the timetable of this stop to the nominal projection ({@link #nominalTimes}): the
     * ideal times with zero carried-over delay. This preserves a flexible stop's catch-up buffer
     * (unlike anchoring the shortened {@link #realtime} departure, which would collapse it into a
     * permanent phantom delay) and keeps the anchored legs consistent with the learned legs (so a
     * normally running train departs and arrives exactly on schedule). Falls back to the real-time
     * values if no nominal projection is available yet.
     */
    public void anchorScheduleToRealtime() {
        if (nominalTimes.isKnown()) {
            this.scheduled = nominalTimes;
        } else if (realtime.isKnown()) {
            this.scheduled = realtime;
        }
    }

    /** Shifts all absolute timestamps, e.g. after a world time jump. */
    public synchronized void shiftTimes(long ticks) {
        this.scheduled = scheduled.shifted(ticks);
        this.realtime = realtime.shifted(ticks);
        this.previousScheduled = previousScheduled.shifted(ticks);
        this.previousRealtime = previousRealtime.shifted(ticks);
        if (lastActualArrival >= 0) lastActualArrival += ticks;
        if (lastActualDeparture >= 0) lastActualDeparture += ticks;
        this.nominalTimes = nominalTimes.shifted(ticks);
    }

    /** Clears all learned and live data. */
    public void reset() {
        legDuration.reset();
        dwellDuration = 0;
        scheduled = StopTimes.UNKNOWN;
        realtime = StopTimes.UNKNOWN;
        nominalTimes = StopTimes.UNKNOWN;
        previousScheduled = StopTimes.UNKNOWN;
        previousRealtime = StopTimes.UNKNOWN;
        lastActualArrival = -1;
        lastActualDeparture = -1;
        completedVisits = 0;
    }

    public CompoundTag toNbt(String stationName) {
        CompoundTag nbt = new CompoundTag();
        nbt.put(NBT_LEG, legDuration.toNbt());
        nbt.putLong(NBT_DWELL, dwellDuration);
        if (scheduled.isKnown()) nbt.put(NBT_SCHEDULED, scheduled.toNbt());
        nbt.putLong(NBT_LAST_ARRIVAL, lastActualArrival);
        nbt.putLong(NBT_LAST_DEPARTURE, lastActualDeparture);
        nbt.putInt(NBT_VISITS, completedVisits);
        if (stationName != null) nbt.putString(NBT_STATION, stationName);
        return nbt;
    }

    public void loadNbt(CompoundTag nbt) {
        legDuration.loadNbt(nbt.getCompound(NBT_LEG));
        this.dwellDuration = nbt.getLong(NBT_DWELL);
        if (nbt.contains(NBT_SCHEDULED)) {
            this.scheduled = StopTimes.fromNbt(nbt.getCompound(NBT_SCHEDULED));
        }
        this.lastActualArrival = nbt.getLong(NBT_LAST_ARRIVAL);
        this.lastActualDeparture = nbt.getLong(NBT_LAST_DEPARTURE);
        this.completedVisits = nbt.getInt(NBT_VISITS);
    }

    public static String loadStationName(CompoundTag nbt) {
        return nbt.getString(NBT_STATION);
    }
}
