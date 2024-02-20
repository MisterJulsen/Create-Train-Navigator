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

import de.mrjulsen.crn.event.listeners.TrainListener;

public class SimulatedTrainSchedule {
    private final Collection<TrainStop> stationOrder;
    private final SimulationData data;

    public SimulatedTrainSchedule(Collection<TrainStop> stations, SimulationData data) {
        this.stationOrder = makeDiractional(stations);
        this.data = data;
    }

    private List<TrainStop> makeDiractional(Collection<TrainStop> raw) {
        List<TrainStop> newList = new ArrayList<>();
        boolean isRepeating = false;
        for (TrainStop stop : raw) {            
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
        return newList;
    }

    public Collection<TrainStop> getAllStops() {
        return stationOrder;
    }

    public Optional<TrainStop> getFirstStopOf(TrainStationAlias station) {
        return getAllStops().stream().filter(x -> x.getStationAlias().equals(station)).findFirst();
    }

    public boolean hasStation(GlobalStation station) {
        return getAllStops().stream().anyMatch(x -> x.getStationAlias().contains(station.name));
    }

    public boolean hasStationAlias(TrainStationAlias station) {
        return getAllStops().stream().anyMatch(x -> x.getStationAlias().equals(station));
    }

    public SimulationData getSimulationData() {
        return data;
    }

    public boolean isInDirection(TrainStationAlias start, TrainStationAlias end) {
        for (TrainStop stop : getAllStops()) {
            if (stop.getStationAlias().equals(start)) {
                return true;
            } else if (stop.getStationAlias().equals(end)) {
                return false;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SimulatedTrainSchedule other) {
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
        if (obj instanceof SimulatedTrainSchedule other) {  
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
        return 17 * Objects.hash(getAllStops());
    }

    @Override
    public String toString() {
        return Arrays.toString(getAllStops().toArray());
    }

    public static int getTrainCycleDuration(Train train) {
        return TrainListener.getInstance().getApproximatedTrainDuration(train);
    }

    public static record SimulationData(Train train, int simulationTime, int simulationCorrection) {}
}
