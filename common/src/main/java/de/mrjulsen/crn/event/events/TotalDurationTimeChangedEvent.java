package de.mrjulsen.crn.event.events;

import com.simibubi.create.content.trains.entity.Train;
import de.mrjulsen.crn.event.CRNEventsManager.AbstractCRNEvent;

public final class TotalDurationTimeChangedEvent extends AbstractCRNEvent<TotalDurationTimeChangedEvent.ITotalDurationTimeChangedEventData> {
    public void run(Train train, long oldTotalDuration, long totalDuration) {
        listeners.values().forEach(x -> x.run(train, oldTotalDuration, totalDuration));
        tickPost();
    }

    @FunctionalInterface
    public static interface ITotalDurationTimeChangedEventData {
        void run(Train train, long oldTotalDuration, long totalDuration);
    }
}
