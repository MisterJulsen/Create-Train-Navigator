package de.mrjulsen.crn.api.core;

/** Which side of a station call is meant, the train coming in or going out. */
public enum CallDirection {

    ARRIVAL,

    DEPARTURE;

    public boolean isArrival() {
        return this == ARRIVAL;
    }

    public boolean isDeparture() {
        return this == DEPARTURE;
    }
}
