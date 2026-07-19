package de.mrjulsen.crn.navigator.route;

import de.mrjulsen.crn.backend.api.StationRef;

/**
 * The gap between two legs of a route: where the traveller changes, how long they have, and whether
 * that is comfortable.
 *
 * @param station   The station the change happens at.
 * @param arrival   When the feeding train gets in, in transformed game ticks.
 * @param departure When the connecting train leaves.
 * @param risk      How likely the change is to work out.
 * @param sameTrain Whether the traveller stays seated because it is physically the same train,
 *                  continuing under a different service. No walking is involved and no minimum
 *                  transfer time applies.
 */
public record RouteTransfer(StationRef station, long arrival, long departure, TransferRisk risk, boolean sameTrain) {

    /** How long the traveller has for the change, in ticks. */
    public long duration() {
        return Math.max(0, departure - arrival);
    }

    /** The station's name, as a shorthand for {@code station().name()}. */
    public String stationName() {
        return station.name();
    }

    /** Whether the traveller has to change platform, i.e. actually leave the train. */
    public boolean requiresChangingTrains() {
        return !sameTrain;
    }

    @Override
    public String toString() {
        return station.name() + " +" + duration() + (sameTrain ? " (through)" : "") + " [" + risk + "]";
    }
}
