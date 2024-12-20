package de.mrjulsen.crn.event.events;

import java.util.Collection;

import com.simibubi.create.content.trains.display.GlobalTrainDisplayData.TrainDeparturePrediction;
import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.crn.event.CRNEventsManager.AbstractCRNEvent;

public final class SubmitTrainPredictionsEvent extends AbstractCRNEvent<SubmitTrainPredictionsEvent.ISubmitTrainPredictionsEventData> {
    public void run(Train train, Collection<TrainDeparturePrediction> predictions, int entryCount, int accumulatedTime, int current) {
        listeners.values().forEach(x -> x.run(train, predictions, entryCount, accumulatedTime, current));
        tickPost();
    }

    @FunctionalInterface
    public static interface ISubmitTrainPredictionsEventData {
        void run(Train train, Collection<TrainDeparturePrediction> predictions, int entryCount, int accumulatedTime, int current);
    }
}
