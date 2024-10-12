package de.mrjulsen.crn.event.events;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.station.GlobalStation;

import de.mrjulsen.crn.event.CRNEventsManager.AbstractCRNEvent;

public final class TrainDestinationChangedEvent extends AbstractCRNEvent<TrainDestinationChangedEvent.ITrainDepartureEventData> {
    public void run(Train train, GlobalStation current, GlobalStation next, int nextIndex) {
        listeners.values().forEach(x -> x.run(train, current, next, nextIndex));
        tickPost();
    }

    @FunctionalInterface
    public static interface ITrainDepartureEventData {
        void run(Train train, GlobalStation current, GlobalStation next, int nextIndex);
    }
}
