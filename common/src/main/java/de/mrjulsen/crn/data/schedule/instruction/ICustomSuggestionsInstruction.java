package de.mrjulsen.crn.data.schedule.instruction;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;

import de.mrjulsen.crn.data.train.TrainData;

public interface ICustomSuggestionsInstruction {
    void run(ScheduleRuntime runtime, TrainData data, Train train, int currentScheduleIndex);
}
