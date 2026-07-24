package de.mrjulsen.crn.core.train;

public enum LiveTrainState {
    NO_SCHEDULE,
    SCHEDULE_PAUSED,
    SCHEDULE_COMPLETED,
    DERAILED,
    AT_STATION,
    EN_ROUTE,
    WAITING_FOR_SIGNAL,
    STALLED;

    public boolean isEnRoute() {
        return this == EN_ROUTE || this == WAITING_FOR_SIGNAL || this == STALLED;
    }

    public boolean isOperating() {
        return isEnRoute() || this == AT_STATION;
    }
}
