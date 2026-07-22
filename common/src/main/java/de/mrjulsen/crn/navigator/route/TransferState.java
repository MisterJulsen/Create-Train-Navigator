package de.mrjulsen.crn.navigator.route;

/**
 * How a change of trains is doing, from comfortable to lost.
 * <p>
 * Worked out from what the two trains are currently reporting, so it moves with them: a change
 * planned with plenty of room can tighten as the feeding train loses time, slip out of reach, and
 * finally be settled once the connecting train leaves.
 * <p>
 * The first three all mean the change still works. They differ in what is left over once the feeding
 * train's present delay is allowed for a second time - the question a traveller actually asks, which
 * is "what if it loses that much again on the way".
 */
public enum TransferState {

    /** Comfortable: the change still works even if the feeding train loses more time. */
    SAFE,

    /** Tight: it works now, but a further delay of the feeding train would break it. */
    TIGHT,

    /** Risky: only the requested minimum is available, with nothing to absorb a further delay. */
    RISKY,

    /**
     * Out of reach: the feeding train no longer gets in early enough to make it. Not settled yet
     * though - the connecting train has not left, so it may still be held or lose time itself.
     */
    ENDANGERED,

    /** Lost: the connecting train has gone without the traveller. */
    MISSED;

    /** Whether the change can still be made at all as things stand. */
    public boolean isReachable() {
        return compareTo(ENDANGERED) < 0;
    }

    /** Whether a traveller should be warned about this change. */
    public boolean isWarning() {
        return this != SAFE;
    }

    /** The more worrying of two states. */
    public TransferState worse(TransferState other) {
        return other == null || compareTo(other) >= 0 ? this : other;
    }
}
