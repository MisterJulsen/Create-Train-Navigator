package de.mrjulsen.crn.core.navigator.route;

import java.util.Objects;

import de.mrjulsen.crn.api.core.StationCall;
import de.mrjulsen.crn.api.core.ref.StationRef;
import de.mrjulsen.crn.core.timing.StopTimes;
import net.minecraft.nbt.CompoundTag;

/**
 * One call of a train at a station within a route leg. The live side of the call, the station really
 * taken and its projected times, is updated as the journey is tracked, while the scheduled side stays
 * fixed.
 * <p>
 * The station and time sides of the call, and everything derived from them, are described by
 * {@link StationCall}.
 */
public final class RouteCall implements StationCall {

    private static final String NBT_SCHEDULED_STATION = "ScheduledStation";
    private static final String NBT_REALTIME_STATION = "RealtimeStation";
    private static final String NBT_ENTRY_INDEX = "EntryIndex";
    private static final String NBT_CYCLE = "Cycle";
    private static final String NBT_SCHEDULED = "Scheduled";
    private static final String NBT_REALTIME = "Realtime";
    private static final String NBT_PASSED = "Passed";

    private final StationRef scheduledStation;
    private final int entryIndex;
    private final int cycle;
    private final StopTimes scheduled;

    private StationRef station;
    private StopTimes realtime;
    private boolean passed;

    public RouteCall(StationRef scheduledStation, StationRef station, int entryIndex, int cycle, StopTimes scheduled, StopTimes realtime) {
        this.scheduledStation = scheduledStation == null ? StationRef.NONE : scheduledStation;
        this.station = station == null ? StationRef.NONE : station;
        this.entryIndex = entryIndex;
        this.cycle = cycle;
        this.scheduled = scheduled == null ? StopTimes.UNKNOWN : scheduled;
        this.realtime = realtime == null ? StopTimes.UNKNOWN : realtime;
    }

    @Override
    public StationRef scheduledStation() {
        return scheduledStation;
    }

    @Override
    public StationRef station() {
        return station;
    }

    /** The call's position among the schedule's entries. */
    public int entryIndex() {
        return entryIndex;
    }

    /** Which cycle of a repeating run this call belongs to. */
    public int cycle() {
        return cycle;
    }

    @Override
    public StopTimes scheduled() {
        return scheduled;
    }

    @Override
    public StopTimes realtime() {
        return realtime;
    }

    /** Whether the train has already made this call. */
    public boolean passed() {
        return passed;
    }

    public void applyRealtime(StationRef station, StopTimes times) {
        if (passed) {
            return;
        }
        if (station != null) {
            this.station = station;
        }
        if (times != null) {
            this.realtime = times;
        }
    }

    public void markPassed() {
        this.passed = true;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.put(NBT_SCHEDULED_STATION, scheduledStation.toNbt());
        nbt.put(NBT_REALTIME_STATION, station.toNbt());
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
        return station.name() + "@" + realtime.arrival() + (passed ? " (passed)" : "");
    }
}
