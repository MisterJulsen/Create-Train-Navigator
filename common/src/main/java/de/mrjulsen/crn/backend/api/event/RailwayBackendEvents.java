package de.mrjulsen.crn.backend.api.event;

import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.backend.api.StopSnapshot;
import de.mrjulsen.crn.backend.api.StopVisitState;
import de.mrjulsen.crn.backend.api.TrainSnapshot;
import de.mrjulsen.crn.backend.core.ServiceState;
import de.mrjulsen.crn.backend.core.TrackedTrain;
import de.mrjulsen.crn.backend.schedule.JourneyStop;

/**
 * Where listeners are registered and where the backend raises its events.
 *
 * <h2>Why events are queued</h2>
 * The backend observes trains on the server thread but does its projection and delay detection on a
 * worker, so events originate on both. Delivering them where they happen would force every listener
 * to be thread-safe and would let one run in the middle of an update pass. Instead every event is
 * queued and delivered on the server thread during the next tick, which gives listeners a single,
 * predictable thread and a consistent view of the backend.
 * <p>
 * The snapshot a listener receives is therefore taken at delivery time rather than when the event
 * occurred - it describes the train as it is when the listener sees it, not one tick earlier.
 *
 * <h2>Cost when nobody is listening</h2>
 * Raising an event with no listeners registered costs a single boolean check and allocates nothing,
 * so the fire sites throughout the backend are free until someone actually subscribes.
 */
public final class RailwayBackendEvents {

    /** Beyond this many pending events the oldest are dropped rather than growing without end. */
    private static final int MAX_PENDING = 4096;

    private static final List<RailwayBackendListener> LISTENERS = new CopyOnWriteArrayList<>();
    private static final Queue<Consumer<RailwayBackendListener>> PENDING = new ConcurrentLinkedQueue<>();

    /** Whether the pending queue has already been reported as overflowing. */
    private static volatile boolean overflowReported = false;

    private RailwayBackendEvents() {}

    /** Registers a listener. Safe to call at any time from any thread. */
    public static void register(RailwayBackendListener listener) {
        if (listener != null && !LISTENERS.contains(listener)) {
            LISTENERS.add(listener);
        }
    }

    /** Removes a listener. Anything already queued for it is still delivered. */
    public static boolean unregister(RailwayBackendListener listener) {
        return LISTENERS.remove(listener);
    }

    /** Whether anyone is listening at all. */
    public static boolean hasListeners() {
        return !LISTENERS.isEmpty();
    }

    /** Removes every listener and discards everything queued. Called when the backend stops. */
    public static void clear() {
        LISTENERS.clear();
        PENDING.clear();
        overflowReported = false;
    }

    /**
     * Delivers everything queued since the last call. Called once per tick on the server thread by
     * {@link de.mrjulsen.crn.backend.RailwayBackend}.
     * <p>
     * A listener that throws is logged and skipped, so one faulty consumer cannot stop the others
     * from being notified or bring the tick down with it.
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

    /** Whether events should be raised for this train at all. */
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
