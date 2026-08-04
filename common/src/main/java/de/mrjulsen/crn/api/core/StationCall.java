package de.mrjulsen.crn.api.core;

import de.mrjulsen.crn.api.core.ref.StationRef;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.core.timing.StopTimes;

/**
 * One call of a train at a station: where it stops and when.
 * <p>
 * Every call has two sides that can drift apart. The station is the one the train really takes,
 * which is not the timetabled one where it has been diverted; the times are the projected ones
 * beside the timetabled ones, and their difference is the delay. Implementations supply the four
 * values, everything derived from them is answered here, so a board entry, a stop of a run and a
 * call of a saved route all answer these questions the same way.
 * <p>
 * Times are in the unit described by {@link RailwayBackendApi#getCurrentTime()} and are not always
 * known; see {@link #hasTimes()}.
 */
public interface StationCall {

    /** The station the train really calls at. */
    StationRef station();

    /** The station the timetable expects, which is what the times were learned against. */
    StationRef scheduledStation();

    /** The timetable times for this call. */
    StopTimes scheduled();

    /** The currently projected times. */
    StopTimes realtime();

    default String stationName() {
        return station().name();
    }

    default String scheduledStationName() {
        return scheduledStation().name();
    }

    default String platform() {
        return station().platform();
    }

    default String scheduledPlatform() {
        return scheduledStation().platform();
    }

    /** Whether the train is taking a different station than the timetable expected. */
    default boolean isDiverted() {
        return scheduledStation().isKnown() && station().isKnown()
            && !scheduledStation().name().equals(station().name());
    }

    /**
     * Whether the diversion is one a traveller would notice, meaning the displayed name changes
     * rather than only the underlying station within the same tag.
     */
    default boolean hasChangedTag() {
        return isDiverted() && !scheduledStation().displayName().equals(station().displayName());
    }

    /** Whether a projection exists for this call, without which the times mean nothing. */
    default boolean hasTimes() {
        return realtime().isKnown();
    }

    /**
     * How much later than scheduled the train arrives, in ticks. Negative when it is early, zero
     * where either time is unknown.
     */
    default long arrivalDeviation() {
        return scheduled().isKnown() && realtime().isKnown() ? realtime().arrival() - scheduled().arrival() : 0;
    }

    /** The same for the departure. */
    default long departureDeviation() {
        return scheduled().isKnown() && realtime().isKnown() ? realtime().departure() - scheduled().departure() : 0;
    }

    /** The deviation on the chosen side of the call, in ticks. */
    default long deviation(CallDirection direction) {
        return direction.isArrival() ? arrivalDeviation() : departureDeviation();
    }

    /** Whether either deviation reaches the given number of ticks. */
    default boolean isDelayed(long thresholdTicks) {
        return arrivalDeviation() >= thresholdTicks || departureDeviation() >= thresholdTicks;
    }

    /** Whether this call counts as late by the server's configured threshold. */
    default boolean isDelayed() {
        return isDelayed(ModCommonConfig.SCHEDULE_DEVIATION_THRESHOLD.get());
    }

    /** Whether the chosen side of the call is late by the configured threshold. */
    default boolean isDelayed(CallDirection direction) {
        return deviation(direction) >= ModCommonConfig.SCHEDULE_DEVIATION_THRESHOLD.get();
    }

    /** The timetable time for the chosen side of the call. */
    default long scheduledTime(CallDirection direction) {
        return direction.isArrival() ? scheduled().arrival() : scheduled().departure();
    }

    /** The projected time for the chosen side of the call. */
    default long realtimeTime(CallDirection direction) {
        return direction.isArrival() ? realtime().arrival() : realtime().departure();
    }

    /** How long until the train arrives, in ticks, given the current time. */
    default long arrivalIn(long now) {
        return realtime().arrivalIn(now);
    }

    /** How long until the train departs, in ticks, given the current time. */
    default long departureIn(long now) {
        return realtime().departureIn(now);
    }

    /** How long the train is timetabled to stand here, in ticks. */
    default long scheduledStayDuration() {
        return scheduled().stayDuration();
    }

    /** How long the train is now projected to stand here, in ticks. */
    default long stayDuration() {
        return realtime().stayDuration();
    }
}
