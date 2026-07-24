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

public final class DelayContext {

    public static final int SIGNAL_WAIT_REPORT_THRESHOLD = 150;
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

    public TrackedTrain train() {
        return train;
    }

    public Train createTrain() {
        return train.getTrain();
    }

    public RealtimeTracker realtime() {
        return train.getRealtime();
    }

    public LiveTrainState liveState() {
        return train.getLiveState();
    }

    public long now() {
        return now;
    }

    public boolean isSectionDelayed() {
        return train.isDelayed();
    }

    public long delayOffset() {
        return train.getDelayOffset();
    }

    public Optional<JourneyStop> currentStop() {
        return train.getCurrentStop();
    }

    public StopTimings currentTiming() {
        return currentStop().map(train::getTimings).orElse(null);
    }

    public long delayThreshold() {
        return ModCommonConfig.SCHEDULE_DEVIATION_THRESHOLD.get();
    }

    public boolean isAtStation() {
        return liveState() == LiveTrainState.AT_STATION;
    }

    public int signalWaitTicks() {
        return realtime().getTotalSignalWaitTicks();
    }

    public int stalledTicks() {
        return realtime().getStalledTicks();
    }

    public int dwellTicks() {
        return realtime().getDwellTicks();
    }

    public Set<String> blockingTrainNames() {
        if (blockingTrainNames == null) {
            blockingTrainNames = realtime().getBlockingTrainNames();
        }
        return blockingTrainNames;
    }

    public Set<UUID> blockingTrainIds() {
        if (blockingTrainIds == null) {
            blockingTrainIds = realtime().getBlockingTrainIds();
        }
        return blockingTrainIds;
    }

    public boolean hasSignificantSignalWait() {
        return signalWaitTicks() > SIGNAL_WAIT_REPORT_THRESHOLD;
    }

    public boolean isStalledSignificantly() {
        return stalledTicks() > STALL_REPORT_THRESHOLD;
    }

    public Optional<String> anyBlockingTrainName() {
        return blockingTrainNames().stream().findFirst();
    }

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

    public boolean isWaitingForSignal() {
        return liveState() == LiveTrainState.WAITING_FOR_SIGNAL;
    }

    public boolean isRedSignal() {
        var navigation = createTrain().navigation;
        if (navigation == null) {
            return false;
        }
        var waitingForSignal = navigation.waitingForSignal;
        return waitingForSignal != null && Boolean.TRUE.equals(waitingForSignal.getSecond());
    }

    public boolean createFailedNavigation() {
        TrainStatusAccessor status = statusFlag();
        return status != null && status.crn$navigation();
    }

    public boolean createBrokenTrack() {
        TrainStatusAccessor status = statusFlag();
        return status != null && status.crn$track();
    }

    public boolean createMissingConductor() {
        TrainStatusAccessor status = statusFlag();
        return status != null && status.crn$conductor();
    }

    private TrainStatusAccessor statusFlag() {
        var status = createTrain().status;
        return status == null ? null : (TrainStatusAccessor) status;
    }
}
