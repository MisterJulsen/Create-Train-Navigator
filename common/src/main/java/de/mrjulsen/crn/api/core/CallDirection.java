package de.mrjulsen.crn.api.core;

/** Which side of a station call is meant, the train coming in or going out. */
public enum CallDirection {

    /** The train arriving at the station. */
    ARRIVAL,

    /** The train leaving the station. */
    DEPARTURE;

    /** Whether this is the arrival side. */
    public boolean isArrival() {
        return this == ARRIVAL;
    }

    /** Whether this is the departure side. */
    public boolean isDeparture() {
        return this == DEPARTURE;
    }
}
