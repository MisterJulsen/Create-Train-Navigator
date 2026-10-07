package de.mrjulsen.crn.api.core.snapshot;

import com.simibubi.create.content.trains.entity.Navigation;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TrainStatus;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import com.simibubi.create.content.trains.station.GlobalStation;

/**
 * A plain data view of the status Create keeps for a train: its schedule, navigation and problems.
 *
 * @param navigationFailed      Whether the train could not find a way to its destination.
 * @param trackProblem          Whether the train has reported a problem with the track.
 * @param conductorMissing      Whether the train is missing a conductor it needs.
 * @param stalled               Whether any carriage cannot move.
 * @param hasSchedule           Whether the train is running a schedule.
 * @param paused                Whether the schedule is paused.
 * @param completed             Whether the schedule has run to its end.
 * @param scheduleState         The schedule runtime's state, or {@code null} if it has no schedule.
 * @param title                 The schedule title in force now, or {@code null}.
 * @param destination           The name of the station the train is heading for, or {@code null}.
 * @param distanceToDestination How far the train still has to travel, in blocks.
 * @param waitingForSignal      Whether the train is being held at a signal.
 */
public record CreateTrainStatusSnapshot(
        boolean navigationFailed,
        boolean trackProblem,
        boolean conductorMissing,
        boolean stalled,
        boolean hasSchedule,
        boolean paused,
        boolean completed,
        String scheduleState,
        String title,
        String destination,
        double distanceToDestination,
        boolean waitingForSignal
) {

    public static CreateTrainStatusSnapshot of(Train train) {
        TrainStatus status = train.status;
        ScheduleRuntime runtime = train.runtime;
        Navigation navigation = train.navigation;
        boolean hasSchedule = runtime != null && runtime.schedule != null;
        GlobalStation destination = navigation == null ? null : navigation.destination;
        String title = runtime != null && runtime.currentTitle != null && !runtime.currentTitle.isEmpty() ? runtime.currentTitle : null;
        return new CreateTrainStatusSnapshot(
                status != null && status.navigation,
                status != null && status.track,
                status != null && status.conductor,
                train.carriages.stream().anyMatch(carriage -> carriage.stalled),
                hasSchedule,
                runtime != null && runtime.paused,
                runtime != null && runtime.completed,
                hasSchedule && runtime.state != null ? runtime.state.name() : null,
                title,
                destination == null ? null : destination.name,
                destination == null || navigation == null ? 0 : navigation.distanceToDestination,
                navigation != null && navigation.waitingForSignal != null
        );
    }
}
