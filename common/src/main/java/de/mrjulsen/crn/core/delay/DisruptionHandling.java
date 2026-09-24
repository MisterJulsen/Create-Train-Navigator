package de.mrjulsen.crn.core.delay;

/** Whether a train being out of service should be treated as its own fault. */
public enum DisruptionHandling {

    /** The train was taken out of service on purpose, not because of a fault. */
    DELIBERATE,

    /** The train is out of service because something went wrong. */
    FAULT
}
