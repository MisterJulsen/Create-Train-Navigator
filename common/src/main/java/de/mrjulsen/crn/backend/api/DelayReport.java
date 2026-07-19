package de.mrjulsen.crn.backend.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import de.mrjulsen.crn.backend.core.TrackedTrain;
import de.mrjulsen.crn.backend.delay.DelayInstance;
import de.mrjulsen.crn.backend.delay.DelaySeverity;
import de.mrjulsen.crn.backend.realtime.RealtimeTracker;
import de.mrjulsen.crn.backend.timing.StopTimings;

/**
 * An immutable report on why a train is late, and by how much.
 * <p>
 * The deviations are what the backend measured; the causes explain them. A cause may state how much
 * of the delay it accounts for, but those figures are informational - they are supplied by whoever
 * detected the cause and need not add up to {@link #maxDeviation()}.
 *
 * @param trainId          The id of the train.
 * @param trainName        The train's own name.
 * @param delayed          Whether the train is late beyond the configured threshold.
 * @param cancelled        Whether the train is out of service because of a disruption.
 * @param maxDeviation     The highest deviation from the timetable across all stops, in ticks.
 * @param delayOffset      Deviation carried over from a previous section, in ticks.
 * @param nextStopArrivalDeviation   Deviation of the arrival at the current stop, in ticks.
 * @param nextStopDepartureDeviation Deviation of the departure from the current stop, in ticks.
 * @param signalWaitTicks  How long the train has been held at signals during the current leg.
 * @param stalledTicks     How long a carriage has been stalled during the current leg.
 * @param dwellTicks       How long the train has been standing at the current station.
 * @param blockingTrains   The names of trains that held this one up during the current leg.
 * @param causes           Every status reason currently applying, most important first.
 */
public record DelayReport(
    UUID trainId,
    String trainName,
    boolean delayed,
    boolean cancelled,
    long maxDeviation,
    long delayOffset,
    long nextStopArrivalDeviation,
    long nextStopDepartureDeviation,
    int signalWaitTicks,
    int stalledTicks,
    int dwellTicks,
    List<String> blockingTrains,
    List<DelayInstance> causes
) {

    public DelayReport {
        blockingTrains = blockingTrains == null ? List.of() : List.copyOf(blockingTrains);
        causes = causes == null ? List.of() : List.copyOf(causes);
    }

    /** Captures the current delay situation of the given train. */
    public static DelayReport of(TrackedTrain train) {
        StopTimings current = train.getCurrentStop().map(train::getTimings).orElse(null);
        RealtimeTracker realtime = train.getRealtime();

        return new DelayReport(
            train.getTrainId(),
            train.getTrainName(),
            train.isDelayed(),
            train.isCancelled(),
            train.getMaxDeviation(),
            train.getDelayOffset(),
            current == null ? 0 : current.getArrivalDeviation(),
            current == null ? 0 : current.getDepartureDeviation(),
            realtime.getTotalSignalWaitTicks(),
            realtime.getStalledTicks(),
            realtime.getDwellTicks(),
            List.copyOf(realtime.getBlockingTrainNames()),
            train.getActiveDelays()
        );
    }

    /** The delay in ticks, i.e. the deviation not carried over from a previous section. */
    public long currentDelay() {
        return Math.max(0, maxDeviation - delayOffset);
    }

    /** Whether any cause is currently known. */
    public boolean hasCauses() {
        return !causes.isEmpty();
    }

    /** The most important reason currently applying, if there is any. */
    public Optional<DelayInstance> primaryCause() {
        return causes.isEmpty() ? Optional.empty() : Optional.of(causes.get(0));
    }

    /** The reasons that take the train out of service, as opposed to merely delaying it. */
    public List<DelayInstance> operationalCauses() {
        return causes.stream().filter(x -> x.severity() == DelaySeverity.IMPORTANT).toList();
    }

    /** The reasons that explain a delay without taking the train out of service. */
    public List<DelayInstance> delayCauses() {
        return causes.stream().filter(x -> x.severity() == DelaySeverity.DELAY).toList();
    }

    /**
     * The sum of the delay contributions the causes were able to quantify, in ticks. Causes that
     * could not quantify their share are left out, so this is a lower bound rather than a total.
     */
    public long attributedDelay() {
        return causes.stream().filter(DelayInstance::hasEstimatedDelay).mapToLong(DelayInstance::estimatedDelayTicks).sum();
    }
}
