package de.mrjulsen.crn.event.events;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.station.GlobalStation;

import de.mrjulsen.crn.event.CRNEventsManager.AbstractCRNEvent;

public final class TrainArrivalAndDepartureEvent extends AbstractCRNEvent<TrainArrivalAndDepartureEvent.ITrainApprochEventData> {
    public void run(Train train, GlobalStation current, boolean arrival) {
        listeners.values().forEach(x -> x.run(train, current, arrival));
        tickPost();
    }

    @FunctionalInterface
    public static interface ITrainApprochEventData {
        /**
         * @param train The current train.
         * @param current The current station.
         * @param departure {@code true} if the train is arriving at the current station, {@code false} when leaving.
         */
        void run(Train train, GlobalStation current, boolean arrival);
    }
}
