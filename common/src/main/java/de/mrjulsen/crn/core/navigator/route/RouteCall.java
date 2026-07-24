package de.mrjulsen.crn.core.navigator.route;

import java.util.Objects;

import de.mrjulsen.crn.api.core.StationRef;
import de.mrjulsen.crn.core.timing.StopTimes;
import de.mrjulsen.crn.config.ModCommonConfig;
import net.minecraft.nbt.CompoundTag;

public final class RouteCall {

    private final StationRef scheduledStation;
    private final int entryIndex;
    private final int cycle;
    private final StopTimes scheduled;

    private StationRef realtimeStation;
    private StopTimes realtime;
    private boolean passed;

    public RouteCall(StationRef scheduledStation, StationRef realtimeStation, int entryIndex, int cycle, StopTimes scheduled, StopTimes realtime) {
        this.scheduledStation = scheduledStation == null ? StationRef.NONE : scheduledStation;
        this.realtimeStation = realtimeStation == null ? StationRef.NONE : realtimeStation;
        this.entryIndex = entryIndex;
        this.cycle = cycle;
        this.scheduled = scheduled == null ? StopTimes.UNKNOWN : scheduled;
        this.realtime = realtime == null ? StopTimes.UNKNOWN : realtime;
    }

    public StationRef scheduledStation() {
        return scheduledStation;
    }

    public StationRef realtimeStation() {
        return realtimeStation;
    }

    public int entryIndex() {
        return entryIndex;
    }

    public int cycle() {
        return cycle;
    }

    public StopTimes scheduled() {
        return scheduled;
    }

    public StopTimes realtime() {
        return realtime;
    }

    public boolean passed() {
        return passed;
    }

    public void applyRealtime(StationRef station, StopTimes times) {
        if (passed) {
            return;
        }
        if (station != null) {
            this.realtimeStation = station;
        }
        if (times != null) {
            this.realtime = times;
        }
    }

    public void markPassed() {
        this.passed = true;
    }

    public String realtimeStationName() {
        return realtimeStation.name();
    }

    public String scheduledStationName() {
        return scheduledStation.name();
    }

    public String realtimePlatform() {
        return realtimeStation.platform();
    }

    public String scheduledPlatform() {
        return scheduledStation.platform();
    }

    public boolean isDiverted() {
        return scheduledStation.isKnown() && realtimeStation.isKnown()
            && !scheduledStation.name().equals(realtimeStation.name());
    }

    public boolean hasChangedTag() {
        return isDiverted() && !scheduledStation.displayName().equals(realtimeStation.displayName());
    }

    public long stayDuration() {
        return realtime.stayDuration();
    }

    public long arrivalDeviation() {
        return scheduled.isKnown() ? realtime.arrival() - scheduled.arrival() : 0;
    }

    public long departureDeviation() {
        return scheduled.isKnown() ? realtime.departure() - scheduled.departure() : 0;
    }

    public boolean hasRealtime() {
        return scheduled.isKnown();
    }

    public boolean isArrivalDelayed() {
        return arrivalDeviation() >= ModCommonConfig.SCHEDULE_DEVIATION_THRESHOLD.get();
    }

    public boolean isDepartureDelayed() {
        return departureDeviation() >= ModCommonConfig.SCHEDULE_DEVIATION_THRESHOLD.get();
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.put(NBT_SCHEDULED_STATION, scheduledStation.toNbt());
        nbt.put(NBT_REALTIME_STATION, realtimeStation.toNbt());
        nbt.putInt(NBT_ENTRY_INDEX, entryIndex);
        nbt.putInt(NBT_CYCLE, cycle);
        nbt.put(NBT_SCHEDULED, scheduled.toNbt());
        nbt.put(NBT_REALTIME, realtime.toNbt());
        nbt.putBoolean(NBT_PASSED, passed);
        return nbt;
    }

    public static RouteCall fromNbt(CompoundTag nbt) {
        RouteCall call = new RouteCall(
            StationRef.fromNbt(nbt.getCompound(NBT_SCHEDULED_STATION)),
            StationRef.fromNbt(nbt.getCompound(NBT_REALTIME_STATION)),
            nbt.getInt(NBT_ENTRY_INDEX),
            nbt.getInt(NBT_CYCLE),
            StopTimes.fromNbt(nbt.getCompound(NBT_SCHEDULED)),
            StopTimes.fromNbt(nbt.getCompound(NBT_REALTIME))
        );
        if (nbt.getBoolean(NBT_PASSED)) {
            call.markPassed();
        }
        return call;
    }

    private static final String NBT_SCHEDULED_STATION = "ScheduledStation";
    private static final String NBT_REALTIME_STATION = "RealtimeStation";
    private static final String NBT_ENTRY_INDEX = "EntryIndex";
    private static final String NBT_CYCLE = "Cycle";
    private static final String NBT_SCHEDULED = "Scheduled";
    private static final String NBT_REALTIME = "Realtime";
    private static final String NBT_PASSED = "Passed";

    @Override
    public boolean equals(Object obj) {
        return obj instanceof RouteCall other && entryIndex == other.entryIndex && cycle == other.cycle;
    }

    @Override
    public int hashCode() {
        return Objects.hash(entryIndex, cycle);
    }

    @Override
    public String toString() {
        return realtimeStation.name() + "@" + realtime.arrival() + (passed ? " (passed)" : "");
    }
}
