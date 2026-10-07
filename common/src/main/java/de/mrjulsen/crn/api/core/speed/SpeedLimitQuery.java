package de.mrjulsen.crn.api.core.speed;

import com.simibubi.create.content.trains.entity.Train;

/**
 * What an {@link ISpeedLimitProvider} is being asked about: which train, how far ahead, and what
 * the answer is wanted for.
 *
 * @param train       The train the restrictions are sought for.
 * @param horizon     How far ahead of the train to report, in blocks. Never negative.
 * @param purpose     What the caller intends to do with the answer.
 * @param cruiseSpeed The speed the train would hold if unrestricted, in blocks per tick. Never
 *                    negative.
 */
public record SpeedLimitQuery(Train train, double horizon, SpeedLimitPurpose purpose, double cruiseSpeed) {

    /** What the queried restrictions are wanted for. */
    public enum SpeedLimitPurpose {

        /** The answer feeds a travel time calculation, so only restrictions that cost time matter. */
        TRAVEL_TIME_ESTIMATE,

        /** The answer is shown to a player, so restrictions worth mentioning matter as well. */
        DISPLAY
    }

    public SpeedLimitQuery {
        horizon = Math.max(0, horizon);
        cruiseSpeed = Math.max(0, cruiseSpeed);
        purpose = purpose == null ? SpeedLimitPurpose.TRAVEL_TIME_ESTIMATE : purpose;
    }

    /** A query whose answer feeds a travel time calculation. */
    public static SpeedLimitQuery forEstimate(Train train, double horizon, double cruiseSpeed) {
        return new SpeedLimitQuery(train, horizon, SpeedLimitPurpose.TRAVEL_TIME_ESTIMATE, cruiseSpeed);
    }

    /** A query whose answer is shown to a player. */
    public static SpeedLimitQuery forDisplay(Train train, double horizon, double cruiseSpeed) {
        return new SpeedLimitQuery(train, horizon, SpeedLimitPurpose.DISPLAY, cruiseSpeed);
    }
}
