package de.mrjulsen.crn.backend.core;

/**
 * An immutable capture of everything a full update needs from the live train objects, taken on the
 * server thread by {@link TrackedTrain#prepareFullUpdate(long)} and consumed by
 * {@link TrackedTrain#fullUpdate(long)} on the worker thread.
 * <p>
 * Capturing these values rather than reading them again during the calculation keeps the projection
 * working on one consistent view of the train, and keeps the reads on the thread that owns the data.
 *
 * @param available             Whether the train has a usable journey at all. When {@code false}
 *                              all other values are meaningless and the update is skipped.
 * @param service               Whether the train can produce data at all. Only
 *                              {@link ServiceState#IN_SERVICE} runs the calculation; the other
 *                              states are captured here so the worker knows why it is skipping.
 * @param currentEntry          The schedule entry the train is currently working on.
 * @param atStation             Whether the train is genuinely dwelling, as opposed to merely having
 *                              no destination - a failed navigation leaves it stranded en route.
 * @param remainingTransitTicks Ticks still needed to reach the current destination, including any
 *                              reported speed limits. Resolved here because the providers are
 *                              handed the live train object and must run on the server thread.
 */
public record LiveUpdateState(
    boolean available,
    ServiceState service,
    int currentEntry,
    boolean atStation,
    int remainingTransitTicks
) {

    /** Used while the train has no journey yet. */
    public static final LiveUpdateState UNAVAILABLE = new LiveUpdateState(false, ServiceState.IN_SERVICE, -1, false, 0);

    /** For a train that cannot run: only the reason, since nothing else will be calculated. */
    public static LiveUpdateState outOfService(ServiceState service) {
        return new LiveUpdateState(true, service, -1, false, 0);
    }
}
