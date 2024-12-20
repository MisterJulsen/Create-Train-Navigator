package de.mrjulsen.crn.data.schedule.instruction;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;

import de.mrjulsen.crn.data.train.TrainData;
import de.mrjulsen.crn.data.train.TrainPrediction;

@FunctionalInterface
public interface IStationPredictableInstruction {
    void predictForStation(TrainData data, TrainPrediction prediction, ScheduleRuntime runtime, int indexInSchedule, Train train);
}
