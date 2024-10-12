package de.mrjulsen.crn.event.events;

import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.crn.event.CRNEventsManager.AbstractCRNEvent;

public final class ScheduleResetEvent extends AbstractCRNEvent<ScheduleResetEvent.IScheduleResetEventData> {
    public void run(Train train, boolean soft) {
        listeners.values().forEach(x -> x.run(train, soft));
        tickPost();
    }

    @FunctionalInterface
    public static interface IScheduleResetEventData {
        void run(Train train, boolean soft);
    }
}
