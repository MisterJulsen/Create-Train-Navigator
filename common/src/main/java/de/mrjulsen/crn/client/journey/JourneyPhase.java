package de.mrjulsen.crn.client.journey;

public enum JourneyPhase {

    BEFORE_DEPARTURE,

    RIDING,

    TRANSFERRING,

    COMPLETED,

    CONNECTION_MISSED,

    TRAIN_CANCELLED;

    public boolean isTravelling() {
        return this == BEFORE_DEPARTURE || this == RIDING || this == TRANSFERRING;
    }

    public boolean isDisrupted() {
        return this == CONNECTION_MISSED || this == TRAIN_CANCELLED;
    }
}
