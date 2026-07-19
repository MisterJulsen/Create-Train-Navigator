package de.mrjulsen.crn.backend.core;

/** How reliable the data of a tracked train currently is. */
public enum TrainLifecycleState {
    /** The train was just discovered. No usable timing data exists yet. */
    PREPARING,
    /** The train is being observed, but not all legs of its journey have been measured yet. */
    LEARNING,
    /** All timing data is available. Schedule and real-time data are reliable. */
    READY,
    /** The train is disrupted and reported as cancelled. */
    CANCELLED,
    /** The train has finished its non-cyclic schedule and has nothing to do. */
    IDLE;

    /** Whether the train's times are reliable enough to publish. */
    public boolean isUsable() {
        return this == READY;
    }
}
