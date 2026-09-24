package de.mrjulsen.crn.core.delay;

/** Where a delay reason came from. */
public enum DelayOrigin {

    /** The backend worked the reason out for itself. */
    DETECTED,

    /** The reason was reported from outside through the API. */
    REPORTED
}
