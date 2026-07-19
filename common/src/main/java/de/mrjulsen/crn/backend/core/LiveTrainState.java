package de.mrjulsen.crn.backend.core;

/** The current live activity of a train, updated every tick. */
public enum LiveTrainState {
    /** No schedule data is available for this train. */
    NO_SCHEDULE,
    /** The schedule of the train is paused. */
    SCHEDULE_PAUSED,
    /** The (non-cyclic) schedule has been completed. */
    SCHEDULE_COMPLETED,
    /** The train has derailed. */
    DERAILED,
    /** The train is waiting at a station. */
    AT_STATION,
    /** The train is traveling between two stops. */
    EN_ROUTE,
    /** The train is en route but currently waiting for a signal. */
    WAITING_FOR_SIGNAL,
    /** The train is en route but at least one carriage is stalled. */
    STALLED;

    /** Whether the train is between two stops, moving or not. */
    public boolean isEnRoute() {
        return this == EN_ROUTE || this == WAITING_FOR_SIGNAL || this == STALLED;
    }

    /** Whether the train is running its schedule at all. */
    public boolean isOperating() {
        return isEnRoute() || this == AT_STATION;
    }
}
