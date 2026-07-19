package de.mrjulsen.crn.backend.delay;

/**
 * How important a train status reason is for the traveller-facing display, ordered from least to
 * most severe. Used to sort and colour the reasons shown on a board / in the GUI.
 */
public enum DelaySeverity {
    /** Purely informational (e.g. a special trip), not an actual problem. */
    INFO,
    /** The train is, or is about to become, delayed. */
    DELAY,
    /** The train is not operating as planned (cancelled, derailed, ...). */
    IMPORTANT;

    /** Whether this severity marks an actual delay of the train. */
    public boolean isDelay() {
        return this == DELAY;
    }
}
