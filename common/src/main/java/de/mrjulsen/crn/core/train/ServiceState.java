package de.mrjulsen.crn.core.train;

/** Whether a train is able to run its service. */
public enum ServiceState {

    /** The train is running, or ready to. */
    IN_SERVICE,

    /** The train is out of service because of a disruption. */
    DISRUPTED,

    /** The train has no schedule to run and is standing idle. */
    IDLE;

    /** Whether the train is in service. */
    public boolean isActive() {
        return this == IN_SERVICE;
    }
}
