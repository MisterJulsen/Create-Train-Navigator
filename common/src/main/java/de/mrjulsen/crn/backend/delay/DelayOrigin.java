package de.mrjulsen.crn.backend.delay;

/** Where a {@link DelayInstance} came from. */
public enum DelayOrigin {

    /** Produced by a {@link DelayCause} polled during a backend update. */
    DETECTED,

    /** Pushed in from outside the backend via {@link ExternalDelayReports}. */
    REPORTED
}
