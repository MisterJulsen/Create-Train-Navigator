package de.mrjulsen.crn.event.events;

import java.util.Map;

import com.simibubi.create.content.trains.display.GlobalTrainDisplayData.TrainDeparturePrediction;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;

import de.mrjulsen.crn.event.CRNEventsManager.AbstractCRNEvent;
import de.mrjulsen.crn.data.schedule.instruction.IStationPredictableInstruction;

public final class CreateTrainPredictionEvent extends AbstractCRNEvent<CreateTrainPredictionEvent.ICreateTrainPredictionEventData> {
    public void run(Train train, ScheduleRuntime schedule, Map<Class<? extends IStationPredictableInstruction>, IStationPredictableInstruction> predictables, int index, int stayDuration, int minStayDuration, TrainDeparturePrediction prediction) {
        listeners.values().forEach(x -> x.run(train, schedule, predictables, index, stayDuration, minStayDuration, prediction));
        tickPost();
    }

    @FunctionalInterface
    public static interface ICreateTrainPredictionEventData {
        void run(Train train, ScheduleRuntime schedule, Map<Class<? extends IStationPredictableInstruction>, IStationPredictableInstruction> predictables, int index, int stayDuration, int minStayDuration, TrainDeparturePrediction prediction);
    }
}
