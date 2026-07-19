package de.mrjulsen.crn.backend.realtime;

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

import de.mrjulsen.crn.backend.core.LiveTrainState;
import de.mrjulsen.crn.data.train.TrainUtils;

/**
 * Observes a single train every tick and measures everything the schedule alone cannot provide:
 * actual transit and dwell durations, time lost at signals (including which trains blocked them)
 * and stalling. Arrival and departure transitions are reported to a {@link Listener}.
 */
public final class RealtimeTracker {

    /** Receives arrival/departure transitions detected by the tracker. */
    public interface Listener {
        /**
         * @param entryIndex   The schedule entry index of the reached stop.
         * @param transitTicks How long the train traveled since the last departure.
         * @param traveled     Whether the train actually traveled to this stop (measurement is only
         *                     meaningful in that case).
         */
        void onArrival(int entryIndex, int transitTicks, boolean traveled);

        /**
         * @param entryIndex The schedule entry index of the stop the train departed from.
         * @param dwellTicks How long the train stayed at the stop.
         */
        void onDeparture(int entryIndex, int dwellTicks);

        /**
         * Whether the given schedule entry is a stop targeting the station the train is
         * currently parked at. Used to distinguish consecutive stops at the same station
         * from the short pre-transit phase before the train actually departs.
         */
        boolean isStopAtCurrentStation(int entryIndex);
    }

    /*
     * Written exclusively by the server thread in tick(), but read by the worker and by API callers
     * on other threads, hence volatile. A reader may see a value a few ticks old, which is
     * irrelevant at the resolution these measurements are used at.
     */

    private volatile LiveTrainState liveState = LiveTrainState.NO_SCHEDULE;

    private volatile int transitTicks = 0;
    private volatile int dwellTicks = 0;
    private volatile int stalledTicksCurrentLeg = 0;
    private volatile int noPathTicksCurrentLeg = 0;

    /**
     * How long a train has to be held at a signal before the trains blocking it are looked up. Kept
     * below {@code DelayContext.SIGNAL_WAIT_REPORT_THRESHOLD}, so the answer is always available by
     * the time a delay reason could name it.
     */
    private static final int OCCUPANT_RESOLVE_DELAY = 40;

    private volatile UUID currentSignalId = null;
    private volatile int currentSignalTicks = 0;
    private volatile Set<String> currentSignalOccupiedBy = Set.of();
    private volatile Set<UUID> currentSignalOccupiedByIds = Set.of();
    /** Whether the occupants of the current signal have already been looked up. */
    private boolean occupantsResolved = false;
    private final List<SignalWait> signalWaitsCurrentLeg = Collections.synchronizedList(new ArrayList<>());
    /**
     * Running sum of the wait ticks of all completed signal waits of the current leg, kept in
     * lockstep with {@link #signalWaitsCurrentLeg}. The total is asked for repeatedly per detection
     * pass, and summing it from the list meant copying that list under its monitor every time.
     */
    private volatile int completedSignalWaitTicks = 0;

    private boolean wasAtStation = false;
    private int lastStationEntryIndex = -1;
    private boolean hasTraveledSinceLastStop = false;

    /**
     * Advances the observation by one tick. Must be called exactly once per tick from the server
     * thread; this is the only place the tracker is written and the live train state is read.
     */
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

    /**
     * Starts a fresh leg. Kept in one place so the signal-wait list and its running tick sum can
     * never be cleared independently of each other.
     */
    private void resetLegMeasurements() {
        transitTicks = 0;
        dwellTicks = 0;
        stalledTicksCurrentLeg = 0;
        noPathTicksCurrentLeg = 0;
        signalWaitsCurrentLeg.clear();
        completedSignalWaitTicks = 0;
        hasTraveledSinceLastStop = false;
    }

    /** Looks up which trains are holding up the current signal. Server thread only. */
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

    /** Aligns the tracker with the train's current situation, e.g. after loading persisted data. */
    public void sync(Train train) {
        this.wasAtStation = train.navigation != null && train.navigation.destination == null
            && train.runtime != null && train.runtime.state == ScheduleRuntime.State.POST_TRANSIT;
        this.lastStationEntryIndex = wasAtStation && train.runtime != null ? train.runtime.currentEntry : -1;
        this.hasTraveledSinceLastStop = false;
    }

    /** The train's current live activity. */
    public LiveTrainState getLiveState() {
        return liveState;
    }

    /** Ticks traveled since the last departure. */
    public int getTransitTicks() {
        return transitTicks;
    }

    /** Ticks waited at the current station. */
    public int getDwellTicks() {
        return dwellTicks;
    }

    /** Ticks stalled during the current leg. */
    public int getStalledTicks() {
        return stalledTicksCurrentLeg;
    }

    /** Ticks spent stranded without a destination (failed navigation) during the current leg. */
    public int getNoPathTicks() {
        return noPathTicksCurrentLeg;
    }

    /** The signal the train is currently waiting at, or {@code null}. */
    public UUID getCurrentSignalId() {
        return currentSignalId;
    }

    /** Ticks spent at the signal the train is currently held at. */
    public int getCurrentSignalTicks() {
        return currentSignalTicks;
    }

    /** The names of the trains occupying the block behind the current signal. */
    public Set<String> getCurrentSignalOccupiedBy() {
        return currentSignalOccupiedBy;
    }

    /** All completed signal waits of the current leg, plus the ongoing one if there is any. */
    public List<SignalWait> getSignalWaits() {
        List<SignalWait> result;
        // Copying a synchronized list is only atomic while holding its monitor.
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

    /** The sum of all ticks lost at signals during the current leg, including the ongoing wait. */
    public int getTotalSignalWaitTicks() {
        return completedSignalWaitTicks + Math.max(0, currentSignalTicks);
    }

    /** The names of all trains involved in signal waits during the current leg. */
    public Set<String> getBlockingTrainNames() {
        Set<String> names = new HashSet<>();
        for (SignalWait wait : getSignalWaits()) {
            names.addAll(wait.occupyingTrains());
        }
        return names;
    }

    /** The ids of all trains involved in signal waits during the current leg. */
    public Set<UUID> getBlockingTrainIds() {
        Set<UUID> ids = new HashSet<>();
        for (SignalWait wait : getSignalWaits()) {
            ids.addAll(wait.occupyingTrainIds());
        }
        return ids;
    }
}
