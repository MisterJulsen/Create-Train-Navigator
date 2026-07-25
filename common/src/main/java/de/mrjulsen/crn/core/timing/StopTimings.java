package de.mrjulsen.crn.core.timing;

import de.mrjulsen.crn.core.util.FrequencyStringSelector;
import de.mrjulsen.crn.config.ModCommonConfig;
import net.minecraft.nbt.CompoundTag;

public final class StopTimings {

    private static final String NBT_LEG = "Leg";
    private static final String NBT_DWELL = "Dwell";
    private static final String NBT_DWELL_RESIDUAL = "DwellResidual";
    private static final String NBT_SCHEDULED = "Scheduled";
    private static final String NBT_LAST_ARRIVAL = "LastArrival";
    private static final String NBT_LAST_DEPARTURE = "LastDeparture";
    private static final String NBT_VISITS = "Visits";
    private static final String NBT_STATION = "Station";
    private static final String NBT_STATION_GUESS = "StationGuess";

    private static final int STATION_GUESS_WINDOW = 10;

    private final int entryIndex;

    private final MedianDurationTracker legDuration;
    private final MedianDurationTracker dwellResidual;
    private long dwellDuration = 0;

    private final FrequencyStringSelector stationGuess = new FrequencyStringSelector(STATION_GUESS_WINDOW);

    private volatile StopTimes scheduled = StopTimes.UNKNOWN;
    private volatile StopTimes realtime = StopTimes.UNKNOWN;

    private volatile StopTimes nominalTimes = StopTimes.UNKNOWN;

    private volatile StopTimes previousScheduled = StopTimes.UNKNOWN;
    private volatile StopTimes previousRealtime = StopTimes.UNKNOWN;

    private volatile long lastActualArrival = -1;
    private volatile long lastActualDeparture = -1;
    private volatile int completedVisits = 0;

    public StopTimings(int entryIndex) {
        this.entryIndex = entryIndex;
        this.legDuration = new MedianDurationTracker(ModCommonConfig.TOTAL_DURATION_BUFFER_SIZE.get(), ModCommonConfig.TOTAL_DURATION_DEVIATION_THRESHOLD.get());
        this.dwellResidual = new MedianDurationTracker(ModCommonConfig.TOTAL_DURATION_BUFFER_SIZE.get(), ModCommonConfig.TOTAL_DURATION_DEVIATION_THRESHOLD.get());
        this.dwellResidual.seed(0);
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

    public MedianDurationTracker dwellResidual() {
        return dwellResidual;
    }

    public int dwellResidualTicks() {
        return Math.max(0, dwellResidual.get());
    }

    public StopTimes getScheduled() {
        return scheduled;
    }

    public StopTimes getRealtime() {
        return realtime;
    }

    public StopTimes getPublishedScheduled() {
        return scheduled.isKnown() ? scheduled : realtime;
    }

    public StopTimes getPreviousScheduled() {
        return previousScheduled;
    }

    public StopTimes getPreviousRealtime() {
        return previousRealtime;
    }

    public long getLastActualArrival() {
        return lastActualArrival;
    }

    public long getLastActualDeparture() {
        return lastActualDeparture;
    }

    public int getCompletedVisits() {
        return completedVisits;
    }

    public void setRealtime(StopTimes realtime) {
        this.realtime = realtime;
    }

    public void setNominalTimes(StopTimes nominalTimes) {
        this.nominalTimes = nominalTimes;
    }

    public void setScheduled(StopTimes scheduled) {
        this.scheduled = scheduled;
    }

    public boolean isComparable() {
        return scheduled.isKnown() && realtime.isKnown();
    }

    public long getArrivalDeviation() {
        return isComparable() ? realtime.arrival() - scheduled.arrival() : 0;
    }

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

    public void recordArrival(long time, int measuredLegTicks, boolean countMeasurement) {
        this.lastActualArrival = time;
        if (countMeasurement) {
            legDuration.record(measuredLegTicks);
        }
    }

    public void recordVisitedStation(String stationName) {
        stationGuess.add(stationName);
    }

    public String getEstimatedStationName() {
        return stationGuess.getPrimary();
    }

    public void recordDwellResidual(int measuredResidualTicks) {
        dwellResidual.record(measuredResidualTicks);
    }

    public synchronized void recordDeparture(long time, long measuredDwellTicks) {
        this.lastActualDeparture = time;
        this.dwellDuration = measuredDwellTicks;

        this.previousScheduled = scheduled;
        this.previousRealtime = new StopTimes(lastActualArrival >= 0 ? lastActualArrival : time, time, time);
        this.completedVisits++;
        this.lastActualArrival = -1;
    }

    public void advanceScheduledCycle(long cycleDuration) {
        if (scheduled.isKnown() && cycleDuration > 0) {
            this.scheduled = scheduled.shifted(cycleDuration);
        }
    }

    public void advanceRealtimeCycle(long cycleDuration) {
        if (realtime.isKnown() && cycleDuration > 0) {
            this.realtime = realtime.shifted(cycleDuration);
        }
    }

    public void anchorScheduleToRealtime() {
        if (nominalTimes.isKnown()) {
            this.scheduled = nominalTimes;
        } else if (realtime.isKnown()) {
            this.scheduled = realtime;
        }
    }

    public synchronized void shiftTimes(long ticks) {
        this.scheduled = scheduled.shifted(ticks);
        this.realtime = realtime.shifted(ticks);
        this.previousScheduled = previousScheduled.shifted(ticks);
        this.previousRealtime = previousRealtime.shifted(ticks);
        if (lastActualArrival >= 0) lastActualArrival += ticks;
        if (lastActualDeparture >= 0) lastActualDeparture += ticks;
        this.nominalTimes = nominalTimes.shifted(ticks);
    }

    public synchronized void resetRuntimeData() {
        scheduled = StopTimes.UNKNOWN;
        realtime = StopTimes.UNKNOWN;
        nominalTimes = StopTimes.UNKNOWN;
        previousScheduled = StopTimes.UNKNOWN;
        previousRealtime = StopTimes.UNKNOWN;
        lastActualArrival = -1;
        lastActualDeparture = -1;
    }

    public CompoundTag toNbt(String stationName) {
        CompoundTag nbt = new CompoundTag();
        nbt.put(NBT_LEG, legDuration.toNbt());
        nbt.put(NBT_DWELL_RESIDUAL, dwellResidual.toNbt());
        nbt.putLong(NBT_DWELL, dwellDuration);
        if (scheduled.isKnown()) nbt.put(NBT_SCHEDULED, scheduled.toNbt());
        nbt.putLong(NBT_LAST_ARRIVAL, lastActualArrival);
        nbt.putLong(NBT_LAST_DEPARTURE, lastActualDeparture);
        nbt.putInt(NBT_VISITS, completedVisits);
        if (stationName != null) nbt.putString(NBT_STATION, stationName);
        nbt.put(NBT_STATION_GUESS, stationGuess.toNbt());
        return nbt;
    }

    public void loadNbt(CompoundTag nbt) {
        legDuration.loadNbt(nbt.getCompound(NBT_LEG));
        if (nbt.contains(NBT_DWELL_RESIDUAL)) {
            dwellResidual.loadNbt(nbt.getCompound(NBT_DWELL_RESIDUAL));
        }
        dwellResidual.seed(0);
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

    public static String loadStationName(CompoundTag nbt) {
        return nbt.getString(NBT_STATION);
    }
}
