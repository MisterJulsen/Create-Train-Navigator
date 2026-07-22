package de.mrjulsen.crn.client.journey;

/**
 * Where a traveller stands within a journey they are following.
 * <p>
 * These are the states the route overlay actually distinguishes. The old system fired an event for
 * every conceivable moment - arrival, departure, announcement, each of them again per stop kind -
 * and left it to the listener to work out what any of them meant. Everything that used to be told
 * apart by the event's name is now told apart by looking at the data: which call is next, whether
 * that call is diverted, how late it is.
 */
public enum JourneyPhase {

    /** The first train has not left the boarding station yet. */
    BEFORE_DEPARTURE,

    /** The traveller is on a train, somewhere between getting on and getting off. */
    RIDING,

    /** The traveller has got off and is waiting for a connecting train. */
    TRANSFERRING,

    /** The last train has reached the destination. */
    COMPLETED,

    /** A change could not be made, so the rest of the plan is out of reach. */
    CONNECTION_MISSED,

    /** A train still needed for this journey is out of service. */
    TRAIN_CANCELLED;

    /** Whether the journey can still be travelled as planned. */
    public boolean isTravelling() {
        return this == BEFORE_DEPARTURE || this == RIDING || this == TRANSFERRING;
    }

    /** Whether this phase is bad news the traveller has to act on. */
    public boolean isDisrupted() {
        return this == CONNECTION_MISSED || this == TRAIN_CANCELLED;
    }
}
