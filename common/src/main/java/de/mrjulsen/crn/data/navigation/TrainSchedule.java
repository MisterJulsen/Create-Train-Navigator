package de.mrjulsen.crn.data.navigation;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.train.TrainListener;
import de.mrjulsen.crn.data.train.TrainStop;
import de.mrjulsen.mcdragonlib.data.Cache;

public class TrainSchedule {
    private final UUID sessionId;
    private final Train train;
    private final List<TrainStop> stops;
    private final Cache<List<TrainStop>> stopsChronologically = new Cache<>(() -> getAllStops().stream().sorted((a, b) -> Long.compare(a.getScheduledArrivalTime(), b.getScheduledArrivalTime())).toList());

    private boolean simulated;
    private long simulationTime;

    private TrainSchedule(UUID sessionId, Train train, List<TrainStop> stops) {
        this.sessionId = sessionId;
        this.train = train;
        this.stops = stops;
    }

    public TrainSchedule(UUID sessionId, Train train) {
        this(TrainListener.data.get(train.id).getSessionId(), train, TrainListener.data.get(train.id).getPredictions().stream().map(x -> new TrainStop(x)).toList());
    }

    public static TrainSchedule empty() {
        return new TrainSchedule(new UUID(0, 0), null, List.of());
    }

    public static TrainSchedule ofSectionForIndex(UUID sessionId, Train train, int stationSectionIndex, int targetStationIndex, long simulationTime) {
        return new TrainSchedule(sessionId, train, TrainListener.data.get(train.id).getSectionForIndex(stationSectionIndex).getAllStops(simulationTime, targetStationIndex));
    }
    
    public List<TrainStop> getAllStops() {
        return stops;
    }

    public List<TrainStop> getAllStopsChronologically() {
        return stopsChronologically.get();
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public Train getTrain() {
        return train;
    }

    public boolean stopsAt(StationTag tag) {
        return stops.stream().anyMatch(x -> x.getTag().equals(tag));
    }

    public TrainSchedule simulate(long ticks) {
        simulated = true;
        simulationTime += ticks;
        return new TrainSchedule(sessionId, train, stops.stream().map(x -> x.copy()).peek(x -> x.simulateTicks(ticks)).toList());
    }

    public boolean isSimulated() {
        return simulated;
    }

    public long getSimulationTime() {
        return simulationTime;
    }

    public boolean isEqual(TrainSchedule other) {
        if (other == null) {
            return false;
        }
        if (getAllStops().size() != other.getAllStops().size()) {
            return false;
        }
        Set<String> tagsA = new HashSet<>();
        Set<String> tagsB = new HashSet<>();
        getAllStops().stream().forEach(x -> tagsA.add(x.getTag().getTagName().get()));
        other.getAllStops().stream().forEach(x -> tagsB.add(x.getTag().getTagName().get()));        
        return tagsA.size() == tagsB.size() && tagsA.stream().allMatch(x -> tagsB.contains(x));
    }
}
