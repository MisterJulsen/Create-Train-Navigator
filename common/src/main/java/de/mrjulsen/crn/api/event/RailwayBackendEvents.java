package de.mrjulsen.crn.api.event;

import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.api.core.snapshot.StopSnapshot;
import de.mrjulsen.crn.api.core.snapshot.TrainSnapshot;
import de.mrjulsen.crn.core.train.ServiceState;
import de.mrjulsen.crn.core.train.TrackedTrain;
import de.mrjulsen.crn.core.schedule.JourneyStop;

/**
 * Keeps the registered {@link RailwayBackendListener}s and delivers events to them.
 * <p>
 * Addons normally use
 * {@link de.mrjulsen.crn.api.core.RailwayBackendApi#addListener(RailwayBackendListener)} rather
 * than this class directly. The {@code fire...} methods and {@link #dispatchPending()} are called
 * by the backend.
 * <p>
 * Events may be raised from any thread and are queued; delivery happens on the server thread. The
 * queue is bounded, and once it is full the oldest undelivered events are dropped so that a slow or
 * absent consumer cannot exhaust memory.
 */
public final class RailwayBackendEvents {

    private static final int MAX_PENDING = 4096;

    private static final List<RailwayBackendListener> LISTENERS = new CopyOnWriteArrayList<>();
    private static final Queue<Consumer<RailwayBackendListener>> PENDING = new ConcurrentLinkedQueue<>();

    private static volatile boolean overflowReported = false;

    private RailwayBackendEvents() {}

    /** Adds a listener unless it is already registered. {@code null} is ignored. */
    public static void register(RailwayBackendListener listener) {
        if (listener != null && !LISTENERS.contains(listener)) {
            LISTENERS.add(listener);
        }
    }

    /** Removes a listener, reporting whether it was registered. */
    public static boolean unregister(RailwayBackendListener listener) {
        return LISTENERS.remove(listener);
    }

    public static boolean hasListeners() {
        return !LISTENERS.isEmpty();
    }

    /** Drops every listener and any undelivered events. Called by the backend on shutdown. */
    public static void clear() {
        LISTENERS.clear();
        PENDING.clear();
        overflowReported = false;
    }

    /**
     * Delivers the events queued so far. Called by the backend once per tick on the server thread;
     * events raised while this runs wait for the next call.
     */
    public static void dispatchPending() {
        if (PENDING.isEmpty()) {
            return;
        }
        int budget = PENDING.size();
        for (int i = 0; i < budget; i++) {
            Consumer<RailwayBackendListener> event = PENDING.poll();
            if (event == null) {
                break;
            }
            for (RailwayBackendListener listener : LISTENERS) {
                try {
                    event.accept(listener);
                } catch (Exception e) {
                    CreateRailwaysNavigator.LOGGER.error("[Backend] Listener {} failed to handle an event.", listener.getClass().getName(), e);
                }
            }
        }
    }

    private static void enqueue(Consumer<RailwayBackendListener> event) {
        if (PENDING.size() >= MAX_PENDING) {
            PENDING.poll();
            if (!overflowReported) {
                overflowReported = true;
                CreateRailwaysNavigator.LOGGER.warn("[Backend] More events are being raised than delivered; the oldest are being dropped.");
            }
        }
        PENDING.add(event);
    }

    private static boolean isReportable(TrackedTrain train) {
        return hasListeners() && train != null && !train.isBlacklisted();
    }

    public static void fireArrival(TrackedTrain train, JourneyStop stop) {
        if (!isReportable(train)) return;
        enqueue(listener -> listener.onArrival(TrainSnapshot.of(train), StopSnapshot.of(train, stop)));
    }

    public static void fireDeparture(TrackedTrain train, JourneyStop stop) {
        if (!isReportable(train)) return;
        enqueue(listener -> listener.onDeparture(TrainSnapshot.of(train), StopSnapshot.of(train, stop)));
    }

    public static void fireServiceStateChanged(TrackedTrain train, ServiceState previous, ServiceState current) {
        if (!isReportable(train)) return;
        enqueue(listener -> listener.onServiceStateChanged(TrainSnapshot.of(train), previous, current));
    }

    public static void fireDelaysChanged(TrackedTrain train) {
        if (!isReportable(train)) return;
        enqueue(listener -> listener.onDelaysChanged(TrainSnapshot.of(train)));
    }

    public static void fireTimetableReset(TrackedTrain train) {
        if (!isReportable(train)) return;
        enqueue(listener -> listener.onTimetableReset(TrainSnapshot.of(train)));
    }

    public static void fireScheduleChanged(TrackedTrain train) {
        if (!isReportable(train)) return;
        enqueue(listener -> listener.onScheduleChanged(TrainSnapshot.of(train)));
    }

    public static void fireTrainTracked(TrackedTrain train) {
        if (!isReportable(train)) return;
        enqueue(listener -> listener.onTrainTracked(TrainSnapshot.of(train)));
    }

    public static void fireTrainForgotten(UUID trainId) {
        if (!hasListeners() || trainId == null) return;
        enqueue(listener -> listener.onTrainForgotten(trainId));
    }
}
