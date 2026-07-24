package de.mrjulsen.crn.core.train;

public record LiveUpdateState(
    boolean available,
    ServiceState service,
    int currentEntry,
    boolean atStation,
    int remainingTransitTicks
) {

    public static final LiveUpdateState UNAVAILABLE = new LiveUpdateState(false, ServiceState.IN_SERVICE, -1, false, 0);

    public static LiveUpdateState outOfService(ServiceState service) {
        return new LiveUpdateState(true, service, -1, false, 0);
    }
}
