package de.mrjulsen.crn.navigator.route;

import java.util.Objects;

import de.mrjulsen.crn.backend.api.StationRef;
import de.mrjulsen.crn.backend.timing.StopTimes;
import de.mrjulsen.crn.config.ModCommonConfig;
import net.minecraft.nbt.CompoundTag;

/**
 * One station a leg of a route calls at, with the times the traveller experiences there.
 * <p>
 * Everything here exists twice, and the names say which is which: {@code scheduled...} is what the
 * timetable plans, {@code realtime...} is what the train is actually doing. Both the station and the
 * times have those two sides, because a filter containing wildcards only commits to a concrete
 * station when the train gets there - a blocked platform can send it to a different one, possibly in
 * an entirely different station tag - and because a train runs late.
 * <p>
 * There is deliberately no unqualified {@code station()} or {@code arrival()}: which of the two is
 * meant is never obvious enough to leave to a default. The derived methods below ({@link
 * #stayDuration()}, the deviations, the delay checks) are unambiguous and need no prefix.
 *
 * <h2>The two halves of a call</h2>
 * The planned half - the station the timetable names, which visit of it this is, and the times it
 * plans - is fixed the moment the route is found and never changes again. The realtime half is a
 * cell that {@linkplain de.mrjulsen.crn.client.journey.JourneyTracker a tracker} keeps writing while
 * the journey is being travelled, so a route that has been handed to a view goes on showing what the
 * train is doing without the view having to fetch anything.
 * <p>
 * Once the train has left, the cell is {@linkplain #markPassed() sealed} and keeps whatever it last
 * held. A stop in the past has a delay it really had, and that is what the traveller is shown from
 * then on - not the same station's next visit, which is a different journey they are not on.
 */
public final class RouteCall {

    private final StationRef scheduledStation;
    private final int entryIndex;
    private final int cycle;
    private final StopTimes scheduled;

    private StationRef realtimeStation;
    private StopTimes realtime;
    private boolean passed;

    /**
     * @param scheduledStation The station the timetable plans for, with its tag and platform attached.
     * @param realtimeStation  The station the train is actually heading to. Differs from
     *                         {@link #scheduledStation()} when the train has been diverted.
     * @param entryIndex The index of this call's instruction in the train's schedule.
     * @param cycle      Which visit of this station the call is, counted from the first one the backend
     *                   ever saw. A projection onto a later journey cycle has a higher one, which is how
     *                   a call still to come is told apart from the one the train is working on now.
     * @param scheduled  The timetable times, against which any delay is measured.
     * @param realtime   The projected times, which is what the route search plans with. At either end of
     *                   a leg these are collapsed: boarding, the traveller does not care when the train
     *                   got in, and alighting they do not care when it leaves again.
     */
    public RouteCall(StationRef scheduledStation, StationRef realtimeStation, int entryIndex, int cycle, StopTimes scheduled, StopTimes realtime) {
        this.scheduledStation = scheduledStation == null ? StationRef.NONE : scheduledStation;
        this.realtimeStation = realtimeStation == null ? StationRef.NONE : realtimeStation;
        this.entryIndex = entryIndex;
        this.cycle = cycle;
        this.scheduled = scheduled == null ? StopTimes.UNKNOWN : scheduled;
        this.realtime = realtime == null ? StopTimes.UNKNOWN : realtime;
    }

    /** The station the timetable plans for. */
    public StationRef scheduledStation() {
        return scheduledStation;
    }

    /** The station the train is actually heading to, as last reported. */
    public StationRef realtimeStation() {
        return realtimeStation;
    }

    /** The index of this call's instruction in the train's schedule. */
    public int entryIndex() {
        return entryIndex;
    }

    /** Which visit of the station this call is. See the constructor. */
    public int cycle() {
        return cycle;
    }

    /** The timetable times. */
    public StopTimes scheduled() {
        return scheduled;
    }

    /** The times the train is actually running to, as last reported. */
    public StopTimes realtime() {
        return realtime;
    }

    /** Whether the train has left this call behind, which is what sealed its realtime half. */
    public boolean passed() {
        return passed;
    }

    /**
     * Writes what the train is currently reporting for this call. Ignored once the call has been
     * passed, so a stop in the past cannot be overwritten with a later visit of the same station.
     */
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

    /** Seals the realtime half at what it currently holds. Cannot be undone. */
    public void markPassed() {
        this.passed = true;
    }

    /** The name of the station the train is actually heading to. */
    public String realtimeStationName() {
        return realtimeStation.name();
    }

    /** The name of the station the timetable plans for. */
    public String scheduledStationName() {
        return scheduledStation.name();
    }

    /** The platform the train is actually expected at, or empty if none is known. */
    public String realtimePlatform() {
        return realtimeStation.platform();
    }

    /** The platform the timetable plans for, or empty if none is known. */
    public String scheduledPlatform() {
        return scheduledStation.platform();
    }

    /** Whether the train calls somewhere other than the timetable plans. */
    public boolean isDiverted() {
        return scheduledStation.isKnown() && realtimeStation.isKnown()
            && !scheduledStation.name().equals(realtimeStation.name());
    }

    /** Whether the station actually called at sits in a different tag than the one planned for. */
    public boolean hasChangedTag() {
        return isDiverted() && !scheduledStation.displayName().equals(realtimeStation.displayName());
    }

    /** How long the train stays here, in ticks. */
    public long stayDuration() {
        return realtime.stayDuration();
    }

    /** How much later than the timetable the train gets here, in ticks. */
    public long arrivalDeviation() {
        return scheduled.isKnown() ? realtime.arrival() - scheduled.arrival() : 0;
    }

    /** How much later than the timetable the train leaves here, in ticks. */
    public long departureDeviation() {
        return scheduled.isKnown() ? realtime.departure() - scheduled.departure() : 0;
    }

    /** Whether the timetable is known here, so a projected time can be shown against it. */
    public boolean hasRealtime() {
        return scheduled.isKnown();
    }

    /** Whether the train gets here later than the configured threshold allows. */
    public boolean isArrivalDelayed() {
        return arrivalDeviation() >= ModCommonConfig.SCHEDULE_DEVIATION_THRESHOLD.get();
    }

    /** Whether the train leaves here later than the configured threshold allows. */
    public boolean isDepartureDelayed() {
        return departureDeviation() >= ModCommonConfig.SCHEDULE_DEVIATION_THRESHOLD.get();
    }

    /** Serializes this call, live half included, so a saved route keeps the state it was left in. */
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

    /** Deserializes a call written by {@link #toNbt()}. */
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

    /**
     * Two calls are the same call when they mean the same visit of the same schedule entry. Only the
     * planned half takes part: the realtime half moves while the journey is being travelled, and a
     * call that stopped equalling itself would break every {@code indexOf} and every comparison a
     * view makes against the call it showed a moment ago.
     */
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
