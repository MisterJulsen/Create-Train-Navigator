package de.mrjulsen.crn.data.schedule.instruction;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;

import de.mrjulsen.crn.data.train.TrainData;

@FunctionalInterface
public interface IPredictableInstruction {
    void predict(TrainData data, ScheduleRuntime runtime, int indexInSchedule, Train train);
}
