package de.mrjulsen.crn.backend.core;

/**
 * Whether a train is in a state in which it can produce data worth computing.
 * <p>
 * Determined once per full update on the server thread. Everything except {@link #IN_SERVICE} skips
 * the whole calculation, since a train that cannot move produces no new timings. Its existing data
 * is left untouched, so displays keep showing where it was and why it stopped.
 */
public enum ServiceState {

    /** The train is operating normally and its data is updated. */
    IN_SERVICE,

    /**
     * The train cannot run: derailed, paused, or otherwise unusable. Reported to travellers as a
     * cancellation with a reason.
     */
    DISRUPTED,

    /**
     * The train has nothing left to do, having completed a non-cyclic schedule. Not a disruption, so
     * it drops off the boards without a fault reason.
     */
    IDLE;

    /** Whether the full update should run for this train. */
    public boolean isActive() {
        return this == IN_SERVICE;
    }
}
