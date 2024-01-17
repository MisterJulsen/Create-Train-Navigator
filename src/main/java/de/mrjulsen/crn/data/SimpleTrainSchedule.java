package de.mrjulsen.crn.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.station.GlobalStation;

import de.mrjulsen.crn.data.SimulatedTrainSchedule.SimulationData;
import de.mrjulsen.crn.event.listeners.TrainListener;

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

    public Collection<TrainStop> getAllStopsFrom(TrainStationAlias alias) {
        final boolean[] startFound = new boolean[] { false };

        return stops.stream().dropWhile(x -> {
            if (x.getStationAlias().equals(alias)) {
                startFound[0] = true;
            }
            return !startFound[0];
        }).toList();
    }

    public SimpleTrainSchedule copy() {
        return new SimpleTrainSchedule(stops.stream().map(x -> x.copy()).toList());
    }

    public SimpleTrainSchedule makeScheduleFrom(TrainStationAlias alias, boolean preventDuplicates) {
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

    public SimpleTrainSchedule makeDirectionalScheduleFrom(TrainStationAlias alias) {
        List<TrainStop> newList = new ArrayList<>();
        for (TrainStop stop : makeScheduleFrom(alias, false).getAllStops()) {     
            if (newList.contains(stop)) {
                break;
            }

            newList.add(stop);
        }
        return SimpleTrainSchedule.of(newList);
    }

    public SimpleTrainSchedule makeDirectionalSchedule() {
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

    public Optional<TrainStop> getNextStopOf(TrainStationAlias alias) {
        return this.getAllStops().stream().filter(x -> x.getStationAlias().equals(alias)).min(Comparator.comparingInt(x -> x.getPrediction().getTicks()));
    }

    public List<TrainStop> getAllStopsOf(TrainStationAlias alias) {
        return this.getAllStops().stream().filter(x -> x.getStationAlias().equals(alias)).toList();
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
        if (obj instanceof SimpleTrainSchedule other) {
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

    public static int getTrainCycleDuration(Train train) {
        return TrainListener.getInstance().getApproximatedTrainDuration(train);
    }

    public SimulatedTrainSchedule simulate(Train train, int simulationTime, TrainStationAlias simulationTarget) {
        final int cycleDuration = getTrainCycleDuration(train);

        int timeToTargetAfterSim = getAllStopsOf(simulationTarget).stream().mapToInt(x -> {
            int v = (int)((double)(x.getPrediction().getTicks() - simulationTime) % cycleDuration);
            if (v < 0) {
                v += cycleDuration;
            }
            return v;
        }).min().getAsInt();
        int simToTargetTime = simulationTime + timeToTargetAfterSim;

        return new SimulatedTrainSchedule(getAllStops().parallelStream().map(x -> {
            int cycle = (int)((double)(x.getPrediction().getTicks() - simToTargetTime) / cycleDuration);
            int estimatedTicks = (x.getPrediction().getTicks() - simToTargetTime) % cycleDuration;
            while (estimatedTicks < 0) {
                estimatedTicks += cycleDuration;
                cycle++;
            }
            cycle += x.getPrediction().getCycle();
            return new TrainStop(x.getStationAlias(), new DeparturePrediction(x.getPrediction().getTrain(), estimatedTicks, x.getPrediction().getScheduleTitle(), x.getPrediction().getNextStopStation(), cycle));
        }).sorted(Comparator.comparingInt(x -> x.getPrediction().getTicks())).toList(), new SimulationData(getFirstStop().get().getPrediction().getTrain(), simulationTime, timeToTargetAfterSim));
    }

    
    public SimpleTrainSchedule simulate(Train train, int simulationTime) {
        final int cycleDuration = getTrainCycleDuration(train);

        return new SimpleTrainSchedule(getAllStops().parallelStream().map(x -> {
            int cycle = (int)((double)(x.getPrediction().getTicks() - simulationTime) / cycleDuration);
            int estimatedTicks = (x.getPrediction().getTicks() - simulationTime) % cycleDuration;
            while (estimatedTicks < 0) {
                estimatedTicks += cycleDuration;
                cycle++;
            }
            cycle += x.getPrediction().getCycle();
            return new TrainStop(x.getStationAlias(), new DeparturePrediction(x.getPrediction().getTrain(), estimatedTicks, x.getPrediction().getScheduleTitle(), x.getPrediction().getNextStopStation(), cycle));
        }).sorted(Comparator.comparingInt(x -> x.getPrediction().getTicks())).toList());
    }
    
}
