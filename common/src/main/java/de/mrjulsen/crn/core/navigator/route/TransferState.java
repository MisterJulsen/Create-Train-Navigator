package de.mrjulsen.crn.core.navigator.route;

/** How safe a transfer is: whether it can still be made, and with how much margin. */
public enum TransferState {

    /** Comfortably makeable. */
    SAFE,

    /** Makeable, but with little time to spare. */
    TIGHT,

    /** Makeable only as long as the feeder train is not delayed any further. */
    RISKY,

    /** No longer makeable as things stand, though the connecting train has not yet left. */
    ENDANGERED,

    /** The connecting train has already left. */
    MISSED;

    /** Whether the transfer can still be made. */
    public boolean isReachable() {
        return compareTo(ENDANGERED) < 0;
    }

    /** Whether the transfer is anything less than safe, and so worth flagging. */
    public boolean isWarning() {
        return this != SAFE;
    }

    /** The worse of this state and the given one. */
    public TransferState worse(TransferState other) {
        return other == null || compareTo(other) >= 0 ? this : other;
    }
}
