package de.mrjulsen.crn.backend.api.event;

import de.mrjulsen.crn.backend.api.StopSnapshot;
import de.mrjulsen.crn.backend.api.TrainSnapshot;
import de.mrjulsen.crn.backend.core.ServiceState;

/**
 * Receives what happens to the trains the backend tracks, so a display, a route or an add-on can
 * react to it instead of asking again and again whether anything has changed.
 * <p>
 * Every method has an empty default, so an implementation only overrides what it cares about and
 * adding a new kind of event later never breaks an existing listener.
 *
 * <h2>Threading</h2>
 * Every event is delivered on the <b>server thread</b>, one at a time, during the tick after it
 * occurred. Listeners therefore need no synchronization of their own and may read the live world -
 * but they also run inside the tick, so anything expensive belongs on another thread.
 *
 * <h2>Blacklisted trains</h2>
 * Events are not raised for trains withheld by the blacklist, matching what the queries return. A
 * listener never has to filter them itself.
 *
 * @see RailwayBackendEvents
 */
public interface RailwayBackendListener {

    /** The train arrived at a stop and is now dwelling there. */
    default void onArrival(TrainSnapshot train, StopSnapshot stop) {}

    /** The train left a stop and is now under way to the next one. */
    default void onDeparture(TrainSnapshot train, StopSnapshot stop) {}

    /**
     * The train's ability to run changed - it broke down, was paused, finished its schedule or
     * returned to service.
     */
    default void onServiceStateChanged(TrainSnapshot train, ServiceState previous, ServiceState current) {}

    /**
     * The reasons the train is late or out of service changed: one appeared, disappeared, or the
     * set of them is no longer the same. The current ones are on the snapshot.
     */
    default void onDelaysChanged(TrainSnapshot train) {}

    /**
     * The train anchored a new timetable, so every scheduled time it publishes has moved. Anything
     * caching those times should discard what it has.
     */
    default void onTimetableReset(TrainSnapshot train) {}

    /**
     * The train received a different schedule. Its journey, its stops and everything it had learned
     * are gone, and its {@linkplain TrainSnapshot#sessionId() session} is a new one - data from
     * before must not be related to what comes after.
     */
    default void onScheduleChanged(TrainSnapshot train) {}

    /** The backend started tracking a train, either newly built or newly discovered. */
    default void onTrainTracked(TrainSnapshot train) {}

    /**
     * The backend stopped tracking a train and discarded its data. Only the id is passed: by the
     * time this arrives there is nothing left to describe.
     */
    default void onTrainForgotten(java.util.UUID trainId) {}
}
