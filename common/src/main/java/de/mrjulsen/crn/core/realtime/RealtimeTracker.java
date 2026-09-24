package de.mrjulsen.crn.core.realtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;

import de.mrjulsen.crn.core.train.LiveTrainState;
import de.mrjulsen.crn.util.TrainUtils;

public final class RealtimeTracker {

    public interface Listener {
        void onArrival(int entryIndex, int transitTicks, boolean traveled);

        void onDeparture(int entryIndex, int dwellTicks);

        boolean isStopAtCurrentStation(int entryIndex);
    }

    private volatile LiveTrainState liveState = LiveTrainState.NO_SCHEDULE;

    private volatile int transitTicks = 0;
    private volatile int dwellTicks = 0;
    private volatile int stalledTicksCurrentLeg = 0;
    private volatile int noPathTicksCurrentLeg = 0;

    private static final int OCCUPANT_RESOLVE_DELAY = 40;

    private volatile UUID currentSignalId = null;
    private volatile int currentSignalTicks = 0;
    private volatile Set<String> currentSignalOccupiedBy = Set.of();
    private volatile Set<UUID> currentSignalOccupiedByIds = Set.of();
    private boolean occupantsResolved = false;
    private final List<SignalWait> signalWaitsCurrentLeg = Collections.synchronizedList(new ArrayList<>());
    private volatile int completedSignalWaitTicks = 0;

    private boolean wasAtStation = false;
    private int lastStationEntryIndex = -1;
    private boolean hasTraveledSinceLastStop = false;

    public void tick(Train train, Listener listener) {
        LiveTrainState state = determineBaseState(train);
        if (!state.isOperating()) {
            this.liveState = state;
            return;
        }

        boolean isAtStation = train.navigation.destination == null && train.runtime.state == ScheduleRuntime.State.POST_TRANSIT;
        int currentEntry = train.runtime.currentEntry;

        if (isAtStation != wasAtStation) {
            if (isAtStation) {
                handleArrival(currentEntry, listener);
            } else {
                handleDeparture(listener);
            }
            wasAtStation = isAtStation;
        } else if (isAtStation && lastStationEntryIndex >= 0 && currentEntry != lastStationEntryIndex) {
            handleDeparture(listener);
            if (listener.isStopAtCurrentStation(currentEntry)) {
                handleArrival(currentEntry, listener);
            } else {
                lastStationEntryIndex = -1;
            }
        }

        if (isAtStation) {
            dwellTicks++;
        } else {
            transitTicks++;
            hasTraveledSinceLastStop = true;
            if (train.navigation.destination == null) {
                noPathTicksCurrentLeg++;
            }
        }

        boolean isWaitingForSignal = !isAtStation && train.navigation.waitingForSignal != null;
        if (isWaitingForSignal) {
            UUID signalId = train.navigation.waitingForSignal.getFirst();
            if (!signalId.equals(currentSignalId)) {
                finishSignalWait();
                currentSignalId = signalId;
                currentSignalTicks = 0;
                occupantsResolved = false;
            }
            currentSignalTicks++;
            if (!occupantsResolved && currentSignalTicks >= OCCUPANT_RESOLVE_DELAY) {
                resolveCurrentSignalOccupants(train);
            }
        } else {
            finishSignalWait();
        }

        boolean stalled = false;
        if (!isAtStation) {
            for (Carriage carriage : train.carriages) {
                if (carriage.stalled) {
                    stalled = true;
                    break;
                }
            }
            if (stalled) {
                stalledTicksCurrentLeg++;
            }
        }

        this.liveState = isAtStation ? LiveTrainState.AT_STATION
            : isWaitingForSignal ? LiveTrainState.WAITING_FOR_SIGNAL
            : stalled ? LiveTrainState.STALLED
            : LiveTrainState.EN_ROUTE;
    }

    private LiveTrainState determineBaseState(Train train) {
        if (train == null || train.runtime == null || train.runtime.getSchedule() == null || train.navigation == null) {
            return LiveTrainState.NO_SCHEDULE;
        }
        if (train.derailed) {
            return LiveTrainState.DERAILED;
        }
        if (train.runtime.paused) {
            return LiveTrainState.SCHEDULE_PAUSED;
        }
        if (train.runtime.completed) {
            return LiveTrainState.SCHEDULE_COMPLETED;
        }
        return LiveTrainState.EN_ROUTE;
    }

    private void handleArrival(int entryIndex, Listener listener) {
        finishSignalWait();
        listener.onArrival(entryIndex, transitTicks, hasTraveledSinceLastStop);
        lastStationEntryIndex = entryIndex;
        resetLegMeasurements();
    }

    private void handleDeparture(Listener listener) {
        if (lastStationEntryIndex >= 0) {
            listener.onDeparture(lastStationEntryIndex, dwellTicks);
        }
        resetLegMeasurements();
    }

    private void resetLegMeasurements() {
        transitTicks = 0;
        dwellTicks = 0;
        stalledTicksCurrentLeg = 0;
        noPathTicksCurrentLeg = 0;
        signalWaitsCurrentLeg.clear();
        completedSignalWaitTicks = 0;
        hasTraveledSinceLastStop = false;
    }

    private void resolveCurrentSignalOccupants(Train train) {
        occupantsResolved = true;
        Set<Train> occupying = TrainUtils.isSignalOccupied(currentSignalId, Set.of(train.id));
        currentSignalOccupiedBy = occupying.stream().map(x -> x.name.getString()).collect(Collectors.toUnmodifiableSet());
        currentSignalOccupiedByIds = occupying.stream().map(x -> x.id).collect(Collectors.toUnmodifiableSet());
    }

    private void finishSignalWait() {
        if (currentSignalId != null && currentSignalTicks > 0) {
            signalWaitsCurrentLeg.add(new SignalWait(currentSignalId, currentSignalTicks, currentSignalOccupiedBy, currentSignalOccupiedByIds));
            completedSignalWaitTicks += currentSignalTicks;
        }
        currentSignalId = null;
        currentSignalTicks = 0;
        currentSignalOccupiedBy = Set.of();
        currentSignalOccupiedByIds = Set.of();
        occupantsResolved = false;
    }

    public void sync(Train train) {
        this.wasAtStation = train.navigation != null && train.navigation.destination == null
            && train.runtime != null && train.runtime.state == ScheduleRuntime.State.POST_TRANSIT;
        this.lastStationEntryIndex = wasAtStation && train.runtime != null ? train.runtime.currentEntry : -1;
        this.hasTraveledSinceLastStop = false;
    }

    public LiveTrainState getLiveState() {
        return liveState;
    }

    public int getTransitTicks() {
        return transitTicks;
    }

    public int getDwellTicks() {
        return dwellTicks;
    }

    public int getStalledTicks() {
        return stalledTicksCurrentLeg;
    }

    public int getNoPathTicks() {
        return noPathTicksCurrentLeg;
    }

    public UUID getCurrentSignalId() {
        return currentSignalId;
    }

    public int getCurrentSignalTicks() {
        return currentSignalTicks;
    }

    public Set<String> getCurrentSignalOccupiedBy() {
        return currentSignalOccupiedBy;
    }

    public List<SignalWait> getSignalWaits() {
        List<SignalWait> result;
        synchronized (signalWaitsCurrentLeg) {
            result = new ArrayList<>(signalWaitsCurrentLeg);
        }
        UUID signalId = currentSignalId;
        int signalTicks = currentSignalTicks;
        if (signalId != null && signalTicks > 0) {
            result.add(new SignalWait(signalId, signalTicks, currentSignalOccupiedBy, currentSignalOccupiedByIds));
        }
        return result;
    }

    public int getTotalSignalWaitTicks() {
        return completedSignalWaitTicks + Math.max(0, currentSignalTicks);
    }

    public Set<String> getBlockingTrainNames() {
        Set<String> names = new HashSet<>();
        for (SignalWait wait : getSignalWaits()) {
            names.addAll(wait.occupyingTrains());
        }
        return names;
    }

    public Set<UUID> getBlockingTrainIds() {
        Set<UUID> ids = new HashSet<>();
        for (SignalWait wait : getSignalWaits()) {
            ids.addAll(wait.occupyingTrainIds());
        }
        return ids;
    }
}
