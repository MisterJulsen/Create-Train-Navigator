package de.mrjulsen.crn.api.core.snapshot;

import com.simibubi.create.content.trains.entity.Navigation;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TrainStatus;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import com.simibubi.create.content.trains.station.GlobalStation;

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
