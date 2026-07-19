package de.mrjulsen.crn.backend.api;

import de.mrjulsen.crn.backend.core.TrackedTrain;
import de.mrjulsen.crn.backend.schedule.JourneySection;
import de.mrjulsen.crn.backend.schedule.JourneyStop;
import de.mrjulsen.crn.backend.timing.CycleProjector;
import de.mrjulsen.crn.backend.timing.StopTimes;
import de.mrjulsen.crn.backend.timing.StopTimings;
import de.mrjulsen.crn.config.ModCommonConfig;

/**
 * An immutable snapshot of one stop of a train's journey: where it is, when the train is meant to
 * be there, when it actually will be, and how it relates to the run as a whole.
 *
 * @param entryIndex         The index of this stop's instruction in the underlying schedule.
 * @param stopIndex          The position of this stop in travel order, counting from zero.
 * @param sectionIndex       The position of the section this stop belongs to, or {@code -1}.
 * @param stationFilter      The raw station filter, which may contain wildcards.
 * @param station            The station called at, with any filter resolved and its tag attached.
 * @param title              The schedule title the train carries towards this stop. May be empty.
 * @param visitState         Whether the train has passed this stop, is at it, or is yet to reach it.
 * @param scheduled          The timetable times. Falls back to {@link #realtime()} while no
 *                           timetable has been anchored, so this is known whenever the projection is.
 * @param realtime           The current projected times.
 * @param previousActual     What the train actually did on its previous visit, or
 *                           {@link StopTimes#UNKNOWN} if it has not completed one.
 * @param arrivalDeviation   How much later than scheduled the train arrives, in ticks. Negative
 *                           when it is early.
 * @param departureDeviation How much later than scheduled the train departs, in ticks.
 * @param delayed            Whether either deviation exceeds the configured threshold.
 * @param legDurationTicks   The learned duration of the leg leading to this stop, or {@code -1}.
 * @param dwellDurationTicks How long the train stayed here on its last visit, in ticks.
 * @param completedVisits    How often the train has departed from this stop while being tracked.
 * @param firstStopOfSection Whether this is the first stop of its section.
 * @param lastStopOfSection  Whether this is the last stop of its section.
 * @param sectionUsable      Whether this stop's section may be used for travel.
 */
public record StopSnapshot(
    int entryIndex,
    int stopIndex,
    int sectionIndex,
    String stationFilter,
    StationRef station,
    String title,
    StopVisitState visitState,
    StopTimes scheduled,
    StopTimes realtime,
    StopTimes previousActual,
    long arrivalDeviation,
    long departureDeviation,
    boolean delayed,
    int legDurationTicks,
    long dwellDurationTicks,
    int completedVisits,
    boolean firstStopOfSection,
    boolean lastStopOfSection,
    boolean sectionUsable
) {

    /** Captures the given stop of the given train. */
    public static StopSnapshot of(TrackedTrain train, JourneyStop stop, StopVisitState visitState) {
        StopTimings timing = train.getTimings(stop);
        JourneySection section = stop.getSection();
        long threshold = ModCommonConfig.SCHEDULE_DEVIATION_THRESHOLD.get();

        return new StopSnapshot(
            stop.entryIndex(),
            stop.getOrderIndex(),
            section == null ? -1 : section.getSectionIndex(),
            stop.getStationFilter(),
            StationRef.of(train.getDisplayStationName(stop)),
            stop.getTitle() == null ? "" : stop.getTitle(),
            visitState,
            timing == null ? StopTimes.UNKNOWN : timing.getPublishedScheduled(),
            timing == null ? StopTimes.UNKNOWN : timing.getRealtime(),
            timing == null ? StopTimes.UNKNOWN : timing.getPreviousRealtime(),
            timing == null ? 0 : timing.getArrivalDeviation(),
            timing == null ? 0 : timing.getDepartureDeviation(),
            timing != null && timing.isDelayed(threshold),
            timing == null ? -1 : timing.legDuration().get(),
            timing == null ? 0 : timing.dwellDuration(),
            timing == null ? 0 : timing.getCompletedVisits(),
            section != null && section.isFirstStop(stop),
            section != null && section.isLastStop(stop),
            section == null || section.isUsable()
        );
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

    /** The station's name, as a shorthand for {@code station().name()}. */
    public String stationName() {
        return station.name();
    }

    /** The platform this call uses, or empty if none is known. */
    public String platform() {
        return station.platform();
    }

    /**
     * This stop as it recurs {@code cycles} journey cycles later, with both its timetable and
     * projected times moved along. Everything else describes the stop rather than the visit and is
     * carried over unchanged; the visit state becomes {@link StopVisitState#UPCOMING}, since a
     * future occurrence has by definition not happened yet.
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
            entryIndex, stopIndex, sectionIndex, stationFilter, station, title,
            StopVisitState.UPCOMING,
            CycleProjector.advancedBy(scheduled, cycleDuration, cycles),
            CycleProjector.advancedBy(realtime, cycleDuration, cycles),
            previousActual,
            arrivalDeviation, departureDeviation, delayed,
            legDurationTicks, dwellDurationTicks, completedVisits,
            firstStopOfSection, lastStopOfSection, sectionUsable
        );
    }

    /**
     * This stop as it recurs at or after the given time, i.e. the next occurrence a traveller at
     * that moment could still catch. Returns this stop unchanged if it already lies at or after it.
     */
    public StopSnapshot atOrAfter(long notBefore, long cycleDuration) {
        return advancedBy(CycleProjector.cyclesUntil(realtime, cycleDuration, notBefore), cycleDuration);
    }
}
