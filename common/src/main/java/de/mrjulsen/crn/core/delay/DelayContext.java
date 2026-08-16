package de.mrjulsen.crn.core.delay;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.core.train.LiveTrainState;
import de.mrjulsen.crn.core.train.TrackedTrain;
import de.mrjulsen.crn.core.realtime.RealtimeTracker;
import de.mrjulsen.crn.core.schedule.JourneyStop;
import de.mrjulsen.crn.core.timing.StopTimings;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.mixin.TrainStatusAccessor;

/**
 * What a {@link DelayCause} is given to decide whether it applies: everything known about one train
 * at the moment it is being examined. The methods answer the questions a cause commonly asks, from
 * how long the train has been held to which trains are in its way.
 * <p>
 * Times are in game ticks on the backend's time base.
 */
public final class DelayContext {

    /** The signal wait, in ticks, beyond which a wait is worth reporting. */
    public static final int SIGNAL_WAIT_REPORT_THRESHOLD = 150;
    /** The stall time, in ticks, beyond which a stall is worth reporting. */
    public static final int STALL_REPORT_THRESHOLD = 100;

    private final TrackedTrain train;
    private final long now;

    private Set<String> blockingTrainNames;
    private Set<UUID> blockingTrainIds;
    private Optional<String> delayedBlockingTrainName;

    public DelayContext(TrackedTrain train, long now) {
        this.train = train;
        this.now = now;
    }

    /** The tracked train being examined. */
    public TrackedTrain train() {
        return train;
    }

    /** The underlying Create train. */
    public Train createTrain() {
        return train.getTrain();
    }

    /** The train's live measurements. */
    public RealtimeTracker realtime() {
        return train.getRealtime();
    }

    /** What the train is doing at this moment. */
    public LiveTrainState liveState() {
        return train.getLiveState();
    }

    /** The time the examination is running at. */
    public long now() {
        return now;
    }

    /** Whether the train counts as late. */
    public boolean isSectionDelayed() {
        return train.isDelayed();
    }

    /** The part of the deviation deliberately not counted as delay, in ticks. */
    public long delayOffset() {
        return train.getDelayOffset();
    }

    /** The stop the train stands at, if any. */
    public Optional<JourneyStop> currentStop() {
        return train.getCurrentStop();
    }

    /** The timings of the current stop, or {@code null} where there is none. */
    public StopTimings currentTiming() {
        return currentStop().map(train::getTimings).orElse(null);
    }

    /** The deviation, in ticks, at which the server counts a train as late. */
    public long delayThreshold() {
        return ModCommonConfig.SCHEDULE_DEVIATION_THRESHOLD.get();
    }

    /** Whether the train is standing at a station. */
    public boolean isAtStation() {
        return liveState() == LiveTrainState.AT_STATION;
    }

    /** How long the train has been held at signals, in ticks. */
    public int signalWaitTicks() {
        return realtime().getTotalSignalWaitTicks();
    }

    /** How long the train has been unable to move, in ticks. */
    public int stalledTicks() {
        return realtime().getStalledTicks();
    }

    /** How long the train has stood at its current stop, in ticks. */
    public int dwellTicks() {
        return realtime().getDwellTicks();
    }

    /** How much longer the train is known to be held for train separation, in ticks. */
    public long separationHoldTicks() {
        return train.getLiveSeparationHoldTicks();
    }

    /** The names of the trains observed to be in the way. */
    public Set<String> blockingTrainNames() {
        if (blockingTrainNames == null) {
            blockingTrainNames = realtime().getBlockingTrainNames();
        }
        return blockingTrainNames;
    }

    /** The ids of the trains observed to be in the way. */
    public Set<UUID> blockingTrainIds() {
        if (blockingTrainIds == null) {
            blockingTrainIds = realtime().getBlockingTrainIds();
        }
        return blockingTrainIds;
    }

    /** Whether the train has waited at signals long enough to be worth reporting. */
    public boolean hasSignificantSignalWait() {
        return signalWaitTicks() > SIGNAL_WAIT_REPORT_THRESHOLD;
    }

    /** Whether the train has been stalled long enough to be worth reporting. */
    public boolean isStalledSignificantly() {
        return stalledTicks() > STALL_REPORT_THRESHOLD;
    }

    /** The name of a train in the way, if any. */
    public Optional<String> anyBlockingTrainName() {
        return blockingTrainNames().stream().findFirst();
    }

    /** The name of a train in the way that is itself running late, if any. */
    public Optional<String> delayedBlockingTrainName() {
        if (delayedBlockingTrainName == null) {
            delayedBlockingTrainName = resolveDelayedBlockingTrainName();
        }
        return delayedBlockingTrainName;
    }

    private Optional<String> resolveDelayedBlockingTrainName() {
        for (UUID id : blockingTrainIds()) {
            Optional<TrackedTrain> other = RailwayBackendApi.getTrackedTrain(id);
            if (other.isPresent() && other.get().isDelayed()) {
                return Optional.of(other.get().getTrainName());
            }
        }
        return Optional.empty();
    }

    /** Whether the train is being held at a signal. */
    public boolean isWaitingForSignal() {
        return liveState() == LiveTrainState.WAITING_FOR_SIGNAL;
    }

    /** Whether the signal the train waits at is showing red. */
    public boolean isRedSignal() {
        var navigation = createTrain().navigation;
        if (navigation == null) {
            return false;
        }
        var waitingForSignal = navigation.waitingForSignal;
        return waitingForSignal != null && Boolean.TRUE.equals(waitingForSignal.getSecond());
    }

    /** Whether Create reports that the train could not navigate to its destination. */
    public boolean createFailedNavigation() {
        TrainStatusAccessor status = statusFlag();
        return status != null && status.crn$navigation();
    }

    /** Whether Create reports a problem with the track. */
    public boolean createBrokenTrack() {
        TrainStatusAccessor status = statusFlag();
        return status != null && status.crn$track();
    }

    /** Whether Create reports a conductor is missing. */
    public boolean createMissingConductor() {
        TrainStatusAccessor status = statusFlag();
        return status != null && status.crn$conductor();
    }

    private TrainStatusAccessor statusFlag() {
        var status = createTrain().status;
        return status == null ? null : (TrainStatusAccessor) status;
    }
}
