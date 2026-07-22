package de.mrjulsen.crn.backend.api;

import de.mrjulsen.crn.backend.core.TrackedTrain;
import de.mrjulsen.crn.backend.schedule.JourneySection;
import de.mrjulsen.crn.backend.schedule.JourneyStop;
import de.mrjulsen.crn.backend.timing.CycleProjector;
import de.mrjulsen.crn.backend.timing.StopTimes;
import de.mrjulsen.crn.backend.timing.StopTimings;
import de.mrjulsen.crn.config.ModCommonConfig;
import net.minecraft.nbt.CompoundTag;

/**
 * An immutable snapshot of one stop of a train's journey: where it is, when the train is meant to
 * be there, when it actually will be, and what the backend has measured about it.
 * <p>
 * Everything here is measured rather than computed. How late the train is, whether that counts as
 * delayed, and where the stop sits relative to the train are all derivable - the first two from the
 * times below, the last from the {@link JourneySnapshot} the stop belongs to - and are offered as
 * methods instead of being stored.
 *
 * @param entryIndex         The index of this stop's instruction in the underlying schedule.
 * @param stopIndex          The position of this stop in travel order, counting from zero.
 * @param sectionIndex       The position of the section this stop belongs to, or {@code -1}.
 * @param stationFilter      The raw station filter, which may contain wildcards.
 * @param scheduledStation   The station the timetable plans for, with its tag attached. A wildcard
 *                           filter resolves to whichever station the stop has served most often.
 * @param realtimeStation    The station the train is actually heading to. A wildcard filter can send
 *                           it somewhere else than planned - to another platform, or to a station in
 *                           an entirely different tag - so this is not always
 *                           {@link #scheduledStation()}. {@link #isDiverted()} answers whether it is.
 * @param title              The schedule title the train carries towards this stop. May be empty.
 * @param scheduled          The timetable times, against which any delay is measured. Falls back to
 *                           {@link #realtime()} while no timetable has been anchored, so this is
 *                           known whenever the projection is.
 * @param realtime           The current projected times.
 * @param previousActual     What the train actually did on its previous visit, or
 *                           {@link StopTimes#UNKNOWN} if it has not completed one.
 * @param legDurationTicks   The learned duration of the leg leading to this stop, or {@code -1}.
 * @param dwellDurationTicks How long the train stayed here on its last visit, in ticks. Measured
 *                           directly rather than taken from {@link #previousActual()}, which
 *                           carries projected times.
 * @param completedVisits    How often the train has departed from this stop while being tracked.
 */
public record StopSnapshot(
    int entryIndex,
    int stopIndex,
    int sectionIndex,
    String stationFilter,
    StationRef scheduledStation,
    StationRef realtimeStation,
    String title,
    StopTimes scheduled,
    StopTimes realtime,
    StopTimes previousActual,
    int legDurationTicks,
    long dwellDurationTicks,
    int completedVisits
) {

    private static final String NBT_ENTRY_INDEX = "EntryIndex";
    private static final String NBT_STOP_INDEX = "StopIndex";
    private static final String NBT_SECTION_INDEX = "SectionIndex";
    private static final String NBT_STATION_FILTER = "StationFilter";
    private static final String NBT_SCHEDULED_STATION = "ScheduledStation";
    private static final String NBT_REALTIME_STATION = "RealtimeStation";
    private static final String NBT_TITLE = "Title";
    private static final String NBT_SCHEDULED = "Scheduled";
    private static final String NBT_REALTIME = "Realtime";
    private static final String NBT_PREVIOUS_ACTUAL = "PreviousActual";
    private static final String NBT_LEG_DURATION = "LegDuration";
    private static final String NBT_DWELL_DURATION = "DwellDuration";
    private static final String NBT_COMPLETED_VISITS = "CompletedVisits";

    public StopSnapshot {
        stationFilter = stationFilter == null ? "" : stationFilter;
        scheduledStation = scheduledStation == null ? StationRef.NONE : scheduledStation;
        realtimeStation = realtimeStation == null ? StationRef.NONE : realtimeStation;
        title = title == null ? "" : title;
        scheduled = scheduled == null ? StopTimes.UNKNOWN : scheduled;
        realtime = realtime == null ? StopTimes.UNKNOWN : realtime;
        previousActual = previousActual == null ? StopTimes.UNKNOWN : previousActual;
    }

    /** Captures the given stop of the given train. */
    public static StopSnapshot of(TrackedTrain train, JourneyStop stop) {
        StopTimings timing = train.getTimings(stop);
        JourneySection section = stop.getSection();

        return new StopSnapshot(
            stop.entryIndex(),
            stop.getOrderIndex(),
            section == null ? -1 : section.getSectionIndex(),
            stop.getStationFilter(),
            StationRef.of(train.getScheduledStationName(stop)),
            StationRef.of(train.getDisplayStationName(stop)),
            stop.getTitle() == null ? "" : stop.getTitle(),
            timing == null ? StopTimes.UNKNOWN : timing.getPublishedScheduled(),
            timing == null ? StopTimes.UNKNOWN : timing.getRealtime(),
            timing == null ? StopTimes.UNKNOWN : timing.getPreviousRealtime(),
            timing == null ? -1 : timing.legDuration().get(),
            timing == null ? 0 : timing.dwellDuration(),
            timing == null ? 0 : timing.getCompletedVisits()
        );
    }

    /** How much later than the timetable the train gets here, in ticks. Negative when it is early. */
    public long arrivalDeviation() {
        return scheduled.isKnown() && realtime.isKnown() ? realtime.arrival() - scheduled.arrival() : 0;
    }

    /** How much later than the timetable the train leaves here, in ticks. */
    public long departureDeviation() {
        return scheduled.isKnown() && realtime.isKnown() ? realtime.departure() - scheduled.departure() : 0;
    }

    /** Whether either deviation reaches the given threshold in ticks. */
    public boolean isDelayed(long thresholdTicks) {
        return arrivalDeviation() >= thresholdTicks || departureDeviation() >= thresholdTicks;
    }

    /** Whether either deviation reaches the configured threshold. */
    public boolean isDelayed() {
        return isDelayed(ModCommonConfig.SCHEDULE_DEVIATION_THRESHOLD.get());
    }

    /** Ticks until the train arrives here. Negative once the arrival time has passed. */
    public long arrivalIn(long now) {
        return realtime.arrivalIn(now);
    }

    /** Ticks until the train departs from here. Negative once the departure time has passed. */
    public long departureIn(long now) {
        return realtime.departureIn(now);
    }

    /** How long the train is scheduled to stay here, in ticks. */
    public long scheduledStayDuration() {
        return scheduled.stayDuration();
    }

    /** Whether any times are known for this stop at all. */
    public boolean hasTimes() {
        return realtime.isKnown();
    }

    /** The name of the station actually served. */
    public String realtimeStationName() {
        return realtimeStation.name();
    }

    /** The name of the station the timetable plans for. */
    public String scheduledStationName() {
        return scheduledStation.name();
    }

    /**
     * Whether the train is heading somewhere other than the timetable plans, which happens when a
     * wildcard filter resolves to a different station than usual.
     */
    public boolean isDiverted() {
        return scheduledStation.isKnown() && realtimeStation.isKnown()
            && !scheduledStation.name().equals(realtimeStation.name());
    }

    /** Whether the station actually served sits in a different tag than the one planned for. */
    public boolean hasChangedTag() {
        return isDiverted() && !scheduledStation.displayName().equals(realtimeStation.displayName());
    }

    /** The platform the train is actually expected at, or empty if none is known. */
    public String realtimePlatform() {
        return realtimeStation.platform();
    }

    /** The platform the timetable plans for, or empty if none is known. */
    public String scheduledPlatform() {
        return scheduledStation.platform();
    }

    /**
     * This stop as it recurs {@code cycles} journey cycles later, with both its timetable and
     * projected times moved along. Everything else describes the stop rather than the visit and is
     * carried over unchanged.
     * <p>
     * Used to reason about runs the train has not started - see
     * {@link de.mrjulsen.crn.backend.timing.CycleProjector} for what such a projection is worth.
     *
     * @param cycles        How many cycles to advance. Zero or less returns this stop unchanged.
     * @param cycleDuration How long one full cycle takes, in ticks.
     */
    public StopSnapshot advancedBy(int cycles, long cycleDuration) {
        if (cycles <= 0 || cycleDuration <= 0) {
            return this;
        }
        return new StopSnapshot(
            entryIndex, stopIndex, sectionIndex, stationFilter, scheduledStation, realtimeStation, title,
            CycleProjector.advancedBy(scheduled, cycleDuration, cycles),
            CycleProjector.advancedBy(realtime, cycleDuration, cycles),
            previousActual,
            legDurationTicks, dwellDurationTicks, completedVisits
        );
    }

    /**
     * This stop as it recurs at or after the given time, i.e. the next occurrence a traveller at
     * that moment could still catch. Returns this stop unchanged if it already lies at or after it.
     */
    public StopSnapshot atOrAfter(long notBefore, long cycleDuration) {
        return advancedBy(CycleProjector.cyclesUntil(realtime, cycleDuration, notBefore), cycleDuration);
    }

    /** Serializes this stop. */
    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt(NBT_ENTRY_INDEX, entryIndex);
        nbt.putInt(NBT_STOP_INDEX, stopIndex);
        nbt.putInt(NBT_SECTION_INDEX, sectionIndex);
        nbt.putString(NBT_STATION_FILTER, stationFilter);
        nbt.put(NBT_SCHEDULED_STATION, scheduledStation.toNbt());
        nbt.put(NBT_REALTIME_STATION, realtimeStation.toNbt());
        nbt.putString(NBT_TITLE, title);
        nbt.put(NBT_SCHEDULED, scheduled.toNbt());
        nbt.put(NBT_REALTIME, realtime.toNbt());
        nbt.put(NBT_PREVIOUS_ACTUAL, previousActual.toNbt());
        nbt.putInt(NBT_LEG_DURATION, legDurationTicks);
        nbt.putLong(NBT_DWELL_DURATION, dwellDurationTicks);
        nbt.putInt(NBT_COMPLETED_VISITS, completedVisits);
        return nbt;
    }

    /** Deserializes a stop written by {@link #toNbt()}. */
    public static StopSnapshot fromNbt(CompoundTag nbt) {
        return new StopSnapshot(
            nbt.getInt(NBT_ENTRY_INDEX),
            nbt.getInt(NBT_STOP_INDEX),
            nbt.getInt(NBT_SECTION_INDEX),
            nbt.getString(NBT_STATION_FILTER),
            StationRef.fromNbt(nbt.getCompound(NBT_SCHEDULED_STATION)),
            StationRef.fromNbt(nbt.getCompound(NBT_REALTIME_STATION)),
            nbt.getString(NBT_TITLE),
            StopTimes.fromNbt(nbt.getCompound(NBT_SCHEDULED)),
            StopTimes.fromNbt(nbt.getCompound(NBT_REALTIME)),
            StopTimes.fromNbt(nbt.getCompound(NBT_PREVIOUS_ACTUAL)),
            nbt.getInt(NBT_LEG_DURATION),
            nbt.getLong(NBT_DWELL_DURATION),
            nbt.getInt(NBT_COMPLETED_VISITS)
        );
    }
}
