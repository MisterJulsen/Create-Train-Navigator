package de.mrjulsen.crn.backend.api;

/**
 * Which half of a call a board is showing: the train coming in, or the train going out.
 * <p>
 * The two are not the same service. A train may arrive as one line and leave as another, so the name,
 * the colour and the text beside it all depend on which of them is meant - see
 * {@link BoardEntry#displayName(CallDirection)}.
 */
public enum CallDirection {

    /** The train coming in, i.e. the service that carried its passengers to this station. */
    ARRIVAL,

    /** The train going out, i.e. the service passengers may board here. */
    DEPARTURE;

    public boolean isArrival() {
        return this == ARRIVAL;
    }

    public boolean isDeparture() {
        return this == DEPARTURE;
    }
}
