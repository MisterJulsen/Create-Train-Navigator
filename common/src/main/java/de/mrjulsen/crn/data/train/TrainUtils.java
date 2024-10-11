package de.mrjulsen.crn.data.train;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.simibubi.create.Create;
import com.simibubi.create.content.decoration.slidingDoor.DoorControlBehaviour;
import com.simibubi.create.content.trains.GlobalRailwayManager;
import com.simibubi.create.content.trains.display.GlobalTrainDisplayData;
import com.simibubi.create.content.trains.display.GlobalTrainDisplayData.TrainDeparturePrediction;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.signal.SignalBoundary;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import com.simibubi.create.foundation.utility.Couple;

import de.mrjulsen.crn.data.NearestTrackStationResult;
import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.TrainExitSide;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.event.ModCommonEvents;
import de.mrjulsen.crn.data.navigation.TrainSchedule;
import de.mrjulsen.mcdragonlib.data.Single.MutableSingle;
import de.mrjulsen.mcdragonlib.util.MathUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class TrainUtils {
    
    public static GlobalRailwayManager getRailwayManager() {
        return Create.RAILWAYS;
    }

    /**
     * Get data about all trains and when they arrive where.
     * @return a Map where the key is the station name and the value is a list of data from all trains that will arrive at this stations.
     */
    public static Map<String, Collection<TrainDeparturePrediction>> allPredictionsRaw() {
        return new HashMap<>(GlobalTrainDisplayData.statusByDestination);
    }    

    public static boolean isStationKnown(String station) {
        return allPredictionsRaw().keySet().stream().anyMatch(x -> TrainUtils.stationMatches(station, x));
    }

    /**
     * A list of all stations in the world.
     * @return a list containing all track stations.
     */
    public static Collection<GlobalStation> getAllStations() {        
        final Collection<GlobalStation> stations = new ArrayList<>();
        getRailwayManager().trackNetworks.forEach((uuid, graph) -> {
            Collection<GlobalStation> foundStations = graph.getPoints(EdgePointType.STATION);
            stations.addAll(foundStations);
        });
        return stations;
    }

    public static Optional<Train> getTrain(UUID trainId) { 
        return Optional.ofNullable(getRailwayManager().trains.get(trainId));
    }

    public static Set<UUID> getTrainIds() {
        return new HashSet<>(getRailwayManager().trains.keySet());
    }

    public static Set<Train> getTrains(boolean onlyValid) {
        return new HashSet<>(getRailwayManager().trains.values().stream().filter(x -> !onlyValid || isTrainValid(x)).toList());
    }

    public static Set<SignalBoundary> getAllSignals() {
        return new HashSet<>(getRailwayManager().trackNetworks.values().stream().flatMap(x -> x.getPoints(EdgePointType.SIGNAL).stream()).toList());
    }

    public static Set<Train> getDepartingTrainsAt(StationTag station) {
        return ImmutableMap.copyOf(GlobalTrainDisplayData.statusByDestination).entrySet().stream().filter(x -> station.contains(x.getKey())).flatMap(x -> x.getValue().stream()).map(x -> x.train).collect(Collectors.toSet());
    }

    public static Set<Train> getDepartingTrainsAt(String station) {
        return ImmutableMap.copyOf(GlobalTrainDisplayData.statusByDestination).entrySet().stream().filter(x -> station.equals(x.getKey())).flatMap(x -> x.getValue().stream()).map(x -> x.train).collect(Collectors.toSet());
    }

    public static List<TrainStop> getDeparturesAt(StationTag station, UUID selfTrain) {
        return getDeparturesAt(x -> x.getStationTag().equals(station), selfTrain);
    }

    public static List<TrainStop> getDeparturesAtStationName(String stationName, UUID selfTrain) {
        return getDeparturesAt(x -> TrainUtils.stationMatches(x.getStationName(), stationName), selfTrain);
    }

    public static List<TrainStop> getDeparturesAt(Predicate<TrainPrediction> stationFilter, UUID selfTrain) {

        MutableSingle<TrainSchedule> selfSchedule = new MutableSingle<TrainSchedule>(null);
        TrainUtils.getTrain(selfTrain).ifPresent(x -> {
            selfSchedule.setFirst(new TrainSchedule(TrainListener.data.containsKey(x.id) ? TrainListener.data.get(x.id).getSessionId() : new UUID(0, 0), x));
        });
        
        List<TrainStop> list = TrainListener.data.values().stream()
            .filter(x -> !x.getTrainId().equals(selfTrain) && TrainUtils.isTrainUsable(x.getTrain()))
            .flatMap(x -> x.getPredictions().stream().filter(stationFilter))
            .map(TrainStop::new)
            .filter(x -> {
                if (selfSchedule.getFirst() != null) {
                    return true;
                }
                Optional<Train> train = TrainUtils.getTrain(x.getTrainId());
                if (!train.isPresent()) {
                    return false;
                }              
                TrainSchedule sched = new TrainSchedule(TrainListener.data.containsKey(train.get().id) ? TrainListener.data.get(train.get().id).getSessionId() : new UUID(0, 0), train.get());
                return !sched.isEqual(selfSchedule.getFirst());
            })
            .sorted((a, b) -> Long.compare(a.getScheduledDepartureTime(), b.getScheduledDepartureTime()))
            .toList();

        List<TrainStop> results = new ArrayList<>();
        Set<UUID> usedTrains = new HashSet<>();
        usedTrains.add(selfTrain);
        for (TrainStop stop : list) {
            if (!usedTrains.contains(stop.getTrainId())) {
                usedTrains.add(stop.getTrainId());
                results.add(stop);
            }
        }

        return results;
    }

    public static Set<Train> isSignalOccupied(UUID signalId, Set<UUID> excludedTrains) {
        Optional<SignalBoundary> signal = getAllSignals().stream().filter(x -> x.getId().equals(signalId)).findFirst();
        if (!signal.isPresent()) {
            return Set.of();
        }

        Set<Train> occupyingTrains = getTrains(false).stream().filter(x -> !excludedTrains.contains(x.id) && x.occupiedSignalBlocks.keySet().stream().anyMatch(y -> y.equals(signal.get().groups.getFirst()) || y.equals(signal.get().groups.getSecond()))).collect(Collectors.toSet());
        return occupyingTrains;
    }

    
    public static NearestTrackStationResult getNearestTrackStation(Level level, Vec3i pos) {   
        Optional<GlobalStation> station = getAllStations().stream().filter(x ->
            isStationKnown(x.name) &&
            x.getBlockEntityDimension().equals(level.dimension()) &&
            !GlobalSettings.getInstance().isStationBlacklisted(x.name)
        ).min((a, b) -> Double.compare(a.getBlockEntityPos().distSqr(pos), b.getBlockEntityPos().distSqr(pos)));

        double distance = station.isPresent() ? station.get().getBlockEntityPos().distSqr(pos) : 0;
        return new NearestTrackStationResult(station, distance);
    }

    public static TrainExitSide getTrainStationExit(GlobalStation station, Direction stationDirection, Level level) {
        DoorControlBehaviour dcb = getTrainStationDoorControl(station, level);
        if (dcb == null) {
            return TrainExitSide.UNKNOWN;
        }

        if (dcb.mode.matches(stationDirection.getClockWise())) {
            return TrainExitSide.RIGHT;
        } else if (dcb.mode.matches(stationDirection.getCounterClockWise())) {
            return TrainExitSide.LEFT;
        }
        return TrainExitSide.UNKNOWN;
    }    

    public static DoorControlBehaviour getTrainStationDoorControl(GlobalStation station, Level level) {
		BlockPos stationPos = station.getBlockEntityPos();
		if (level == null || !level.isLoaded(stationPos)) {
			return null;
        }
        if (level.getBlockEntity(stationPos) instanceof StationBlockEntity be) {
            return be.doorControls;
        }
        return null;
	}
    

    public static Optional<TrackEdge> getEdge(GlobalStation station) {
        MutableSingle<TrackEdge> edge = new MutableSingle<TrackEdge>(null);
        Create.RAILWAYS.trackNetworks.forEach((uuid, graph) -> {
            if (edge.getFirst() != null) return;
            TrackEdge e = graph.getConnection(Couple.create(graph.locateNode(station.edgeLocation.getFirst()), graph.locateNode(station.edgeLocation.getSecond())));
            if (e == null) return;
            edge.setFirst(e);
        });        
        return Optional.ofNullable(edge.getFirst());
    }

    public static double angleOn(TrackEdgePoint point, TrackEdge edge) {
        double basePos = point.isPrimary(edge.node1) ? edge.getLength() - point.position : point.position;
        Vec3 vec = edge.getDirectionAt(basePos);
        return point.isPrimary(edge.node1) ? MathUtils.getVectorAngle(vec) : MathUtils.getVectorAngle(vec.reverse());
    }

    public static TrainExitSide getExitSide(GlobalStation station) {
        Level level = ModCommonEvents.getPhysicalLevel();
        if (level == null || station == null || !level.isLoaded(station.getBlockEntityPos())) {
            return TrainExitSide.UNKNOWN;
        }
        final Optional<TrackEdge> edge = level != null ? getEdge(station) : Optional.empty();
        if (!edge.isPresent()) {
            return TrainExitSide.UNKNOWN;
        }
        TrainExitSide side = getTrainStationExit(station, Direction.fromYRot(angleOn(station, edge.get())), level);

        return side;
    }

    
    public static boolean stationMatches(String stationName, String filter) {
        String regex = filter.isBlank() ? filter : "\\Q" + filter.replace("*", "\\E.*\\Q");
        return stationName.matches(regex);
    }

    public static boolean isTrainValid(Train train) {
        return //!train.derailed &&
               !train.invalid &&
               //!train.runtime.paused &&
               train.runtime.getSchedule() != null &&
               train.graph != null
        ;
    }

    public static boolean isTrainUsable(Train train) {
        return isTrainValid(train) &&
               TrainListener.data.containsKey(train.id) &&
               TrainListener.data.get(train.id).isInitialized() && 
               !TrainListener.data.get(train.id).isPreparing()
        ;
    }
}
