package de.mrjulsen.crn.core.train;

public enum ServiceState {

    IN_SERVICE,

    DISRUPTED,

    IDLE;

    public boolean isActive() {
        return this == IN_SERVICE;
    }
}
