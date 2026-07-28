package de.mrjulsen.crn.api.core;

import de.mrjulsen.crn.core.train.TrackedTrain;
import de.mrjulsen.crn.core.schedule.JourneySection;
import de.mrjulsen.crn.core.schedule.JourneyStop;
import de.mrjulsen.crn.core.timing.CycleProjector;
import de.mrjulsen.crn.core.timing.StopTimes;
import de.mrjulsen.crn.core.timing.StopTimings;
import net.minecraft.nbt.CompoundTag;

/**
 * One stop of a train's run, with its times.
 * <p>
 * A stop is written in the schedule as a station filter, which may match several stations, so the
 * station the train is really taking can differ from the timetabled one; see {@link StationCall}
 * for what both sides answer.
 *
 * @param entryIndex        The stop's position among the schedule's entries.
 * @param stopIndex         The stop's position among the run's stops, counting stops only.
 * @param sectionIndex      The section this stop belongs to, or {@code -1} if none.
 * @param stationFilter     The station filter as written in the schedule.
 * @param scheduledStation  The station the timetable expects, which is what the times were learned
 *                          against.
 * @param station           The station the train is actually taking.
 * @param title             The schedule title in force at this stop, or empty.
 * @param scheduled         The timetable times. Where no timetable has been established yet, the
 *                          projection stands in, so a consumer never sees a known projected time
 *                          beside an unknown scheduled one.
 * @param realtime          The currently projected times.
 * @param previousActual    The measured times of the previous completed visit.
 * @param legDurationTicks  The learned travel time from the preceding stop, or {@code -1} if not
 *                          yet learned.
 * @param dwellDurationTicks How long the train stood here on its most recent visit.
 * @param completedVisits   How often the train has departed from this stop while being tracked.
 */
public record StopSnapshot(
    int entryIndex,
    int stopIndex,
    int sectionIndex,
    String stationFilter,
    StationRef scheduledStation,
    StationRef station,
    String title,
    StopTimes scheduled,
    StopTimes realtime,
    StopTimes previousActual,
    int legDurationTicks,
    long dwellDurationTicks,
    int completedVisits
) implements StationCall {

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
        station = station == null ? StationRef.NONE : station;
        title = title == null ? "" : title;
        scheduled = scheduled == null ? StopTimes.UNKNOWN : scheduled;
        realtime = realtime == null ? StopTimes.UNKNOWN : realtime;
        previousActual = previousActual == null ? StopTimes.UNKNOWN : previousActual;
    }

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

    /**
     * The same stop as it falls the given number of schedule cycles later. Only meaningful for a
     * cyclic schedule; returns this stop unchanged where the arguments do not permit projection.
     */
    public StopSnapshot advancedBy(int cycles, long cycleDuration) {
        if (cycles <= 0 || cycleDuration <= 0) {
            return this;
        }
        return new StopSnapshot(
            entryIndex, stopIndex, sectionIndex, stationFilter, scheduledStation, station, title,
            CycleProjector.advancedBy(scheduled, cycleDuration, cycles),
            CycleProjector.advancedBy(realtime, cycleDuration, cycles),
            previousActual,
            legDurationTicks, dwellDurationTicks, completedVisits
        );
    }

    /**
     * The next occurrence of this stop at or after the given time, projected forward by as many
     * whole cycles as that requires.
     */
    public StopSnapshot atOrAfter(long notBefore, long cycleDuration) {
        return advancedBy(CycleProjector.cyclesUntil(realtime, cycleDuration, notBefore), cycleDuration);
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt(NBT_ENTRY_INDEX, entryIndex);
        nbt.putInt(NBT_STOP_INDEX, stopIndex);
        nbt.putInt(NBT_SECTION_INDEX, sectionIndex);
        nbt.putString(NBT_STATION_FILTER, stationFilter);
        nbt.put(NBT_SCHEDULED_STATION, scheduledStation.toNbt());
        nbt.put(NBT_REALTIME_STATION, station.toNbt());
        nbt.putString(NBT_TITLE, title);
        nbt.put(NBT_SCHEDULED, scheduled.toNbt());
        nbt.put(NBT_REALTIME, realtime.toNbt());
        nbt.put(NBT_PREVIOUS_ACTUAL, previousActual.toNbt());
        nbt.putInt(NBT_LEG_DURATION, legDurationTicks);
        nbt.putLong(NBT_DWELL_DURATION, dwellDurationTicks);
        nbt.putInt(NBT_COMPLETED_VISITS, completedVisits);
        return nbt;
    }

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
