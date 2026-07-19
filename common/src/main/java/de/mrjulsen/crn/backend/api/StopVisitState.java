package de.mrjulsen.crn.backend.api;

/**
 * Where a stop lies relative to the train's current position on its journey. Lets a consumer show
 * a run as a whole - what the train has already done and what is still to come - rather than only
 * the part still ahead.
 */
public enum StopVisitState {

    /** The train has already departed from this stop on its current run. */
    PASSED,

    /** The stop the train is at or currently traveling towards. */
    CURRENT,

    /** The train has yet to reach this stop. */
    UPCOMING;

    /** Whether the train still has to serve this stop. */
    public boolean isPending() {
        return this != PASSED;
    }
}
