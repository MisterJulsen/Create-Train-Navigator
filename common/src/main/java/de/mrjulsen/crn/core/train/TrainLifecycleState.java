package de.mrjulsen.crn.core.train;

public enum TrainLifecycleState {
    PREPARING,
    LEARNING,
    READY,
    CANCELLED,
    IDLE;

    public boolean isUsable() {
        return this == READY;
    }
}
