package de.mrjulsen.crn.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.List;

import com.simibubi.create.content.trains.entity.Train;

public class RoutePart {
    private Train train;
    private TrainStop start;
    private TrainStop end;
    private Collection<TrainStop> stops;
    
    public RoutePart(Train train, TrainStationAlias start, TrainStationAlias end, int startTicks) {
        this.train = train;
        List<TrainStop> stops = new ArrayList<>(GlobalTrainData.getInstance().getAllStopoversOfTrainSortedNew(train, start, end, true, true));

        TrainStop startStop = stops.get(0);
        if (startStop.getPrediction().getTicks() < startTicks && startStop.getPrediction().getTrainCycleDuration() > 0) {
            int diffTicks = startTicks - startStop.getPrediction().getTicks();
            int mul = diffTicks / startStop.getPrediction().getTrainCycleDuration() + 1;

            stops = new ArrayList<>(stops.stream().map(x -> new TrainStop(x.getStationAlias(), DeparturePrediction.withCycleTicks(x.getPrediction(), mul))).toList());
        }

        this.end = stops.remove(stops.size() - 1);
        this.start = stops.remove(0);

        this.stops = stops;     
    }

    public Train getTrain() {
        return train;
    }

    public TrainStop getStartStation() {
        return start;
    }

    public TrainStop getEndStation() {
        return end;
    }

    public Collection<TrainStop> getStopovers() {
        return stops;
    }

    public int arrivesAtStartStationIn() {
        Optional<DeparturePrediction> data = GlobalTrainData.getInstance().getNextTrainStopDataAt(getTrain(), getStartStation().getStationAlias());
        if (data.isPresent()) {
            return data.get().getTicks();
        }
        return 0;
    }

    public int arrivesAtEndStationIn() {
        Optional<DeparturePrediction> data = GlobalTrainData.getInstance().getNextTrainStopDataAt(getTrain(), getEndStation().getStationAlias());
        if (data.isPresent()) {
            return data.get().getTicks();
        }
        return 0;
    }

    public int getStationCount(boolean includeStartEnd) {
        return getStopovers().size() + (includeStartEnd ? 2 : 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RoutePart other) {
            return getStartStation().equals(other.getStartStation()) &&
                getEndStation().equals(other.getEndStation()) &&
                getStopovers().size() == other.getStopovers().size() &&
                getStopovers().stream().allMatch(x -> other.getStopovers().stream().anyMatch(y -> y.equals(x)));
        }
        return false;
    }

    public boolean exactEquals(Object obj, boolean respectTrains) {
        if (obj instanceof RoutePart other) {
            if (!getStartStation().equals(other.getStartStation()) ||
                !getEndStation().equals(other.getEndStation()) ||
                getStopovers().size() != other.getStopovers().size())
                return false;
                
            TrainStop[] a = getStopovers().toArray(TrainStop[]::new);
            TrainStop[] b = other.getStopovers().toArray(TrainStop[]::new);

            for (int i = 0; i < a.length; i++) {
                if (!a[i].equals(b[i]))
                    return false;
            }
            return !respectTrains || train.equals(other.train);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s, From: %s (%s), To: %s (%s), via: %s",
            getTrain().name.getString(),
            getStartStation().getStationAlias().getAliasName(),
            arrivesAtStartStationIn(),
            getEndStation().getStationAlias().getAliasName(),
            arrivesAtEndStationIn(),
            Arrays.toString(stops.toArray())
        );

        
    }

    public String getString() {
       return String.format("Train: %s, From: %s in %st, To: %s in %s, Stops: %s",
            getTrain().name.getString(),
            getStartStation().getStationAlias().getAliasName(),
            arrivesAtStartStationIn(),
            getEndStation().getStationAlias().getAliasName(),
            arrivesAtEndStationIn(),
            Arrays.toString(stops.toArray()));
    }
}
