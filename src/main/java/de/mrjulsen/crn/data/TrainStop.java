package de.mrjulsen.crn.data;

import com.simibubi.create.content.trains.station.GlobalStation;

public class TrainStop {
    
    private TrainStationAlias station;
    private DeparturePrediction prediction;

    public TrainStop(TrainStationAlias station, DeparturePrediction prediction) {
        this.station = station;
        this.prediction = prediction;
    }

    public TrainStationAlias getStationAlias() {
        return station;
    }

    public DeparturePrediction getPrediction() {
        return prediction;
    }

    public boolean isStation(GlobalStation station) {
        return getStationAlias().contains(station.name);
    }

    public boolean isStationAlias(TrainStationAlias alias) {
        return getStationAlias().equals(alias);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TrainStop other) {
            return getStationAlias().equals(other.getStationAlias());
        }
        return false;        
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", getStationAlias(), prediction.getTicks());
    }
}
