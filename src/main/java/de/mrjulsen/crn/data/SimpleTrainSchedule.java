package de.mrjulsen.crn.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.station.GlobalStation;

public class SimpleTrainSchedule {
    private Collection<TrainStop> stops;

    public SimpleTrainSchedule(Train train) {
        this(GlobalTrainData.getInstance().getAllStopsSorted(train));
    }

    private SimpleTrainSchedule(Collection<TrainStop> stations) {
        this.stops = stations;
    }

    public static SimpleTrainSchedule of(Collection<TrainStop> stations) {
        return new SimpleTrainSchedule(stations);
    }

    public Collection<TrainStop> getAllStops() {
        return stops;
    }

    public SimpleTrainSchedule copy() {
        return new SimpleTrainSchedule(stops.stream().map(x -> x.copy()).toList());
    }

    public SimpleTrainSchedule getAllStopsFrom(TrainStationAlias alias, boolean preventDuplicates) {
        List<TrainStop> newList = new ArrayList<>();
        int idx = 0;
        for (TrainStop stop : getAllStops()) {            
            if (preventDuplicates && newList.contains(stop)) {
                continue;
            }

            if (stop.getStationAlias().equals(alias)) {
                idx = 0;
            }

            newList.add(idx, stop);
            idx++;
        }
        return SimpleTrainSchedule.of(newList);
    }

    public SimpleTrainSchedule getDirectionalScheduleFrom(TrainStationAlias alias) {
        List<TrainStop> newList = new ArrayList<>();
        for (TrainStop stop : getAllStopsFrom(alias, false).getAllStops()) {     
            if (newList.contains(stop)) {
                break;
            }

            newList.add(stop);
        }
        return SimpleTrainSchedule.of(newList);
    }

    public SimpleTrainSchedule getDirectionalSchedule() {
        List<TrainStop> newList = new ArrayList<>();
        boolean isRepeating = false;
        for (TrainStop stop : getAllStops()) {            
            if (newList.contains(stop)) {
                isRepeating = true;
                continue;
            }

            if (isRepeating) {
                newList.add(0, stop);
            } else {
                newList.add(stop);
            }
        }
        return SimpleTrainSchedule.of(newList);
    }

    public boolean hasStation(GlobalStation station) {
        return getAllStops().stream().anyMatch(x -> x.isStation(station));
    }

    public boolean hasStationAlias(TrainStationAlias station) {
        return getAllStops().stream().anyMatch(x -> x.isStationAlias(station));
    }

    public Optional<TrainStop> getLastStop(TrainStationAlias start) {
        Optional<TrainStop> lastStop = this.getAllStops().stream().reduce((a, b) -> b);
        return lastStop.get().getStationAlias().equals(start) ? getFirstStop() : lastStop;
    }

    public Optional<TrainStop> getFirstStop() {
        return this.getAllStops().stream().findFirst();
    }

    public Optional<TrainStop> getNextStop() {
        return this.getAllStops().stream().min((a, b) -> a.getPrediction().getTicks()); 
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SimpleTrainSchedule other) {  
            /*
            if (getAllStops().size() != other.getAllStops().size()) {
                return false;
            }
            
            return getAllStops().stream().allMatch(x -> other.getAllStops().stream().anyMatch(y -> x.equals(y)));
            */
            Set<TrainStop> thisStops = new HashSet<>(getAllStops());
            Set<TrainStop> otherStops = new HashSet<>(other.getAllStops());

            if (thisStops.size() != otherStops.size()) {
                return false;
            }

            return thisStops.containsAll(otherStops);
        }
        return false;
    }

    public boolean exactEquals(Object obj) {
        if (obj instanceof SimpleTrainSchedule other) {  
            if (getAllStops().size() != other.getAllStops().size()) {
                return false;
            }
            
            TrainStop[] a = getAllStops().toArray(TrainStop[]::new);
            TrainStop[] b = other.getAllStops().toArray(TrainStop[]::new);
            for (int i = 0; i < a.length; i++) {
                if (!a[i].equals(b[i]))
                    return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 17 * Objects.hash(stops);
    }

    @Override
    public String toString() {
        return Arrays.toString(stops.toArray());
    }
}
