package de.mrjulsen.crn.backend.delay;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.crn.backend.api.RailwayBackendApi;
import de.mrjulsen.crn.backend.core.LiveTrainState;
import de.mrjulsen.crn.backend.core.TrackedTrain;
import de.mrjulsen.crn.backend.realtime.RealtimeTracker;
import de.mrjulsen.crn.backend.schedule.JourneyStop;
import de.mrjulsen.crn.backend.timing.StopTimings;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.mixin.TrainStatusAccessor;

/**
 * Everything a {@link DelayCause} needs to decide whether it currently applies to a train, exposed
 * as read-only accessors so a cause stays a short, declarative check rather than having to reach
 * into the backend internals. A fresh context is created for each detection pass.
 */
public final class DelayContext {

    /** Signal waits shorter than this, in ticks, are not reported as a delay reason. */
    public static final int SIGNAL_WAIT_REPORT_THRESHOLD = 150;
    /** Stalls shorter than this, in ticks, are not reported as a delay reason. */
    public static final int STALL_REPORT_THRESHOLD = 100;

    private final TrackedTrain train;
    private final long now;

    /*
     * Per-pass caches. A context is used by a single thread, so these are plain fields. Several
     * causes ask the same questions, and both answers are expensive enough to be worth keeping.
     */
    private Set<String> blockingTrainNames;
    private Set<UUID> blockingTrainIds;
    private Optional<String> delayedBlockingTrainName;

    public DelayContext(TrackedTrain train, long now) {
        this.train = train;
        this.now = now;
    }

    /** The train being examined. */
    public TrackedTrain train() {
        return train;
    }

    /** The underlying live train object. */
    public Train createTrain() {
        return train.getTrain();
    }

    /** The live measurements of the train. */
    public RealtimeTracker realtime() {
        return train.getRealtime();
    }

    /** The train's current live activity. */
    public LiveTrainState liveState() {
        return train.getLiveState();
    }

    /** The current transformed game time of this detection pass. */
    public long now() {
        return now;
    }

    /** Whether the train is delayed beyond the configured threshold. */
    public boolean isSectionDelayed() {
        return train.isDelayed();
    }

    /** Deviation in ticks carried over from a previous section. */
    public long delayOffset() {
        return train.getDelayOffset();
    }

    /** The stop the train is currently at or traveling to. */
    public Optional<JourneyStop> currentStop() {
        return train.getCurrentStop();
    }

    /** The timing data of the stop the train is currently at or traveling to, or {@code null}. */
    public StopTimings currentTiming() {
        return currentStop().map(train::getTimings).orElse(null);
    }

    /** The configured deviation in ticks above which a train counts as delayed. */
    public long delayThreshold() {
        return ModCommonConfig.SCHEDULE_DEVIATION_THRESHOLD.get();
    }

    /** Whether the train is currently dwelling at a station. */
    public boolean isAtStation() {
        return liveState() == LiveTrainState.AT_STATION;
    }

    /** Total ticks lost at signals during the current leg. */
    public int signalWaitTicks() {
        return realtime().getTotalSignalWaitTicks();
    }

    /** Total ticks a carriage was stalled during the current leg. */
    public int stalledTicks() {
        return realtime().getStalledTicks();
    }

    /** Ticks the train has been waiting at the station, or {@code 0} while en route. */
    public int dwellTicks() {
        return realtime().getDwellTicks();
    }

    /** The names of all trains involved in signal waits during the current leg. */
    public Set<String> blockingTrainNames() {
        if (blockingTrainNames == null) {
            blockingTrainNames = realtime().getBlockingTrainNames();
        }
        return blockingTrainNames;
    }

    /** The ids of all trains involved in signal waits during the current leg. */
    public Set<UUID> blockingTrainIds() {
        if (blockingTrainIds == null) {
            blockingTrainIds = realtime().getBlockingTrainIds();
        }
        return blockingTrainIds;
    }

    /** Whether the accumulated signal wait of the current leg is long enough to report. */
    public boolean hasSignificantSignalWait() {
        return signalWaitTicks() > SIGNAL_WAIT_REPORT_THRESHOLD;
    }

    /** Whether the accumulated stall time of the current leg is long enough to report. */
    public boolean isStalledSignificantly() {
        return stalledTicks() > STALL_REPORT_THRESHOLD;
    }

    /** Any train blocking this one, if known. */
    public Optional<String> anyBlockingTrainName() {
        return blockingTrainNames().stream().findFirst();
    }

    /** The name of a train blocking this one that is itself delayed, if there is any. */
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

    /** Whether the train is right now physically held at a signal. */
    public boolean isWaitingForSignal() {
        return liveState() == LiveTrainState.WAITING_FOR_SIGNAL;
    }

    /** Whether the signal the train is held at is red, as opposed to a soft or priority wait. */
    public boolean isRedSignal() {
        var navigation = createTrain().navigation;
        if (navigation == null) {
            return false;
        }
        // Read once: the server thread can clear this the moment the train is released.
        var waitingForSignal = navigation.waitingForSignal;
        return waitingForSignal != null && Boolean.TRUE.equals(waitingForSignal.getSecond());
    }

    /** Whether no path to the train's next destination could be found. */
    public boolean createFailedNavigation() {
        TrainStatusAccessor status = statusFlag();
        return status != null && status.crn$navigation();
    }

    /** Whether the track ahead of the train ended or is broken. */
    public boolean createBrokenTrack() {
        TrainStatusAccessor status = statusFlag();
        return status != null && status.crn$track();
    }

    /** Whether the train is missing its conductor. */
    public boolean createMissingConductor() {
        TrainStatusAccessor status = statusFlag();
        return status != null && status.crn$conductor();
    }

    private TrainStatusAccessor statusFlag() {
        var status = createTrain().status;
        return status == null ? null : (TrainStatusAccessor) status;
    }
}
