package de.mrjulsen.crn.core.navigator.route;

public enum TransferState {

    SAFE,

    TIGHT,

    RISKY,

    ENDANGERED,

    MISSED;

    public boolean isReachable() {
        return compareTo(ENDANGERED) < 0;
    }

    public boolean isWarning() {
        return this != SAFE;
    }

    public TransferState worse(TransferState other) {
        return other == null || compareTo(other) >= 0 ? this : other;
    }
}
