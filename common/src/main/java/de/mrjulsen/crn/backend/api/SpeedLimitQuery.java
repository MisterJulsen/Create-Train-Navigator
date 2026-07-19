package de.mrjulsen.crn.backend.api;

import com.simibubi.create.content.trains.entity.Train;

/**
 * What a {@link ISpeedLimitProvider} is being asked about.
 * <p>
 * The question is passed as a record rather than as loose parameters so it can gain further context
 * later without breaking existing providers: a provider matches on the fields it understands and
 * ignores the rest.
 *
 * @param train      The train being asked about. Never {@code null}.
 * @param horizon    How far ahead along the train's path the answer should reach, in blocks.
 *                   Segments beyond this are ignored, so a provider may stop looking there.
 * @param purpose    What the answer is used for. A provider may report differently for an estimate
 *                   than for something shown to a player.
 * @param cruiseSpeed The speed in blocks per tick the train would hold if nothing restricted it.
 *                   A provider never needs to report a limit above this; the backend already caps
 *                   the profile at it.
 */
public record SpeedLimitQuery(Train train, double horizon, SpeedLimitPurpose purpose, double cruiseSpeed) {

    /** Why the speed limits are being asked for. */
    public enum SpeedLimitPurpose {

        /** To estimate how long the train needs for the remaining distance. */
        TRAVEL_TIME_ESTIMATE,

        /** To show the limits themselves to a player. */
        DISPLAY
    }

    public SpeedLimitQuery {
        horizon = Math.max(0, horizon);
        cruiseSpeed = Math.max(0, cruiseSpeed);
        purpose = purpose == null ? SpeedLimitPurpose.TRAVEL_TIME_ESTIMATE : purpose;
    }

    /** A query for estimating the remaining travel time. */
    public static SpeedLimitQuery forEstimate(Train train, double horizon, double cruiseSpeed) {
        return new SpeedLimitQuery(train, horizon, SpeedLimitPurpose.TRAVEL_TIME_ESTIMATE, cruiseSpeed);
    }

    /** A query for showing the limits ahead to a player. */
    public static SpeedLimitQuery forDisplay(Train train, double horizon, double cruiseSpeed) {
        return new SpeedLimitQuery(train, horizon, SpeedLimitPurpose.DISPLAY, cruiseSpeed);
    }
}
