package de.mrjulsen.crn.api.event;

import de.mrjulsen.crn.api.core.snapshot.StopSnapshot;
import de.mrjulsen.crn.api.core.snapshot.TrainSnapshot;
import de.mrjulsen.crn.core.train.ServiceState;

/**
 * Notified of what the trains are doing. Register an implementation with
 * {@link de.mrjulsen.crn.api.core.RailwayBackendApi#addListener(RailwayBackendListener)}. Every
 * method has an empty default, so implement only those of interest.
 *
 * <h2>Delivery</h2>
 * Events are queued where they occur and delivered on the server thread during the backend's tick,
 * so an implementation may touch game state directly. It should still return quickly, because every
 * listener is called in turn on the tick. A listener that throws is logged and skipped; it stays
 * registered and will be called again.
 * <p>
 * The snapshots passed in describe the train as it is when the event is delivered, which is not
 * quite the moment the event occurred. If events are raised faster than they can be delivered, the
 * oldest are dropped, so a listener must not depend on seeing every event to keep its own state
 * consistent.
 * <p>
 * Blacklisted trains raise no events.
 */
public interface RailwayBackendListener {

    /** A train has reached a stop and come to a stand. */
    default void onArrival(TrainSnapshot train, StopSnapshot stop) {}

    /** A train has left a stop. */
    default void onDeparture(TrainSnapshot train, StopSnapshot stop) {}

    /** A train has entered or left service, for instance by being disrupted or recovering. */
    default void onServiceStateChanged(TrainSnapshot train, ServiceState previous, ServiceState current) {}

    /** The reasons why a train is late have changed, or it is no longer late. */
    default void onDelaysChanged(TrainSnapshot train) {}

    /**
     * A train's timetable has been discarded and is being learned afresh, so its scheduled times
     * are not comparable with those reported before.
     */
    default void onTimetableReset(TrainSnapshot train) {}

    /** A train has been given a different schedule, so its run has changed. */
    default void onScheduleChanged(TrainSnapshot train) {}

    /** The backend has begun tracking a train. Its data is not dependable yet. */
    default void onTrainTracked(TrainSnapshot train) {}

    /** The backend has dropped a train and will report nothing further about it. */
    default void onTrainForgotten(java.util.UUID trainId) {}
}
