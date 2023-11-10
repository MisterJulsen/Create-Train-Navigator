package de.mrjulsen.crn.data;

import com.simibubi.create.content.trains.display.GlobalTrainDisplayData.TrainDeparturePrediction;
import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.crn.event.listeners.TrainListener;

public class DeparturePrediction {

    private Train train;
    private int ticks;
    private String scheduleTitle;
    private String nextStop;

    private int cycle;

    public DeparturePrediction(Train train, int ticks, String scheduleTitle, String nextStop, int cycle) {
        this.train = train;
        this.ticks = ticks;
        this.scheduleTitle = scheduleTitle;
        this.nextStop = nextStop;
        this.cycle = cycle;
    }


    public DeparturePrediction() {

    }
    
    public DeparturePrediction(TrainDeparturePrediction prediction) {
        this(prediction.train, prediction.ticks, prediction.scheduleTitle.getString(), prediction.destination, 0);
    }

    public static DeparturePrediction withNextCycleTicks(DeparturePrediction current) {
        int cycle = current.getCycle() + 1;
        return new DeparturePrediction(current.getTrain(), (getTrainCycleDuration(current.getTrain()) * cycle) + current.getTicks(), current.getScheduleTitle(), current.getNextStopStation(), cycle);
    }

    public static DeparturePrediction withCycleTicks(DeparturePrediction current, int cycles) {
        return new DeparturePrediction(current.getTrain(), (getTrainCycleDuration(current.getTrain()) * cycles) + current.getTicks(), current.getScheduleTitle(), current.getNextStopStation(), cycles);
    }

    public Train getTrain() {
        return train;
    }

    public int getTicks() {
        return ticks;
    }

    public String getScheduleTitle() {
        return scheduleTitle;
    }

    public String getNextStopStation() {
        return nextStop;
    }

    public TrainStationAlias getNextStop() {
        return GlobalSettingsManager.getInstance().getSettingsData().getAliasFor(nextStop);
    }

    public int getCycle() {
        return cycle;
    }


    public int getTrainCycleDuration() {
        return getTrainCycleDuration(getTrain());
    }

    public static int getTrainCycleDuration(Train train) {
        return TrainListener.getInstance().getApproximatedTrainDuration(train);
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DeparturePrediction other) {
            return getTrain().id == other.getTrain().id && getTicks() == other.getTicks() && getScheduleTitle().equals(other.getScheduleTitle()) && getNextStopStation().equals(other.getNextStopStation());
        }

        return false;
    }

    public boolean similar(DeparturePrediction other) {
        return getTicks() == other.getTicks() && getNextStopStation().equals(other.getNextStopStation());
    }

    @Override
    public String toString() {
        return String.format("%s, Next stop: %s in %st", getTrain().name.getString(), getNextStop().getAliasName(), getTicks());
    }
}
