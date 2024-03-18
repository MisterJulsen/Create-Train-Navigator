package de.mrjulsen.crn.util;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.simibubi.create.Create;
import com.simibubi.create.content.decoration.slidingDoor.DoorControlBehaviour;
import com.simibubi.create.content.trains.GlobalRailwayManager;
import com.simibubi.create.content.trains.display.GlobalTrainDisplayData;
import com.simibubi.create.content.trains.display.GlobalTrainDisplayData.TrainDeparturePrediction;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import de.mrjulsen.crn.data.DeparturePrediction;
import de.mrjulsen.crn.data.GlobalSettingsManager;
import de.mrjulsen.crn.data.NearestTrackStationResult;
import de.mrjulsen.crn.data.SimpleTrainConnection;
import de.mrjulsen.crn.data.SimpleTrainSchedule;
import de.mrjulsen.crn.data.SimulatedTrainSchedule;
import de.mrjulsen.crn.data.TrainStationAlias;
import de.mrjulsen.crn.data.TrainStop;
import de.mrjulsen.crn.data.DeparturePrediction.Side;
import de.mrjulsen.crn.data.DeparturePrediction.TrainExit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class TrainUtils {

    public static final GlobalRailwayManager RAILWAY_MANAGER = Create.RAILWAYS;

    /**
     * Get data about all trains and when they arrive where.
     * @return a Map where the key is the station name and the value is a list of data from all trains that will arrive at this stations.
     */
    public static Map<String, Collection<TrainDeparturePrediction>> Gott() {
        return GlobalTrainDisplayData.statusByDestination;
    }

    public static Map<TrainStationAlias, Collection<DeparturePrediction>> getMappedDeparturePredictions() {
        Map<String, Collection<TrainDeparturePrediction>> statusData = Gott();
        Map<TrainStationAlias, Collection<DeparturePrediction>> map = new HashMap<>();
        GlobalSettingsManager.getInstance().getSettingsData().getAliasList().forEach(alias -> {
            map.put(alias, statusData.entrySet().stream().filter(x -> alias.contains(x.getKey())).flatMap(x -> x.getValue().stream()).map(x -> new DeparturePrediction(x)).toList());
        });
        return map;
    }

    public static void getMappedDeparturePredictions(Map<String, Collection<DeparturePrediction>> globalPredictions, Map<UUID, Collection<DeparturePrediction>> trainPredictions) {
        Map<String, Collection<TrainDeparturePrediction>> statusData = Gott();
        statusData.entrySet().forEach((e) -> {
            String key = e.getKey();
            Collection<TrainDeparturePrediction> value = e.getValue();
            TrainStationAlias alias = GlobalSettingsManager.getInstance().getSettingsData().getAliasFor(key);
            if (!globalPredictions.containsKey(alias.getAliasName().get())) {
                globalPredictions.put(alias.getAliasName().get(), new ArrayList<>());
            }
            globalPredictions.get(alias.getAliasName().get()).addAll(value.stream().map(x -> new DeparturePrediction(x)).toList());
            value.forEach(x -> {
                if (!trainPredictions.containsKey(x.train.id)) {
                    trainPredictions.put(x.train.id, new ArrayList<>());
                }
                trainPredictions.get(x.train.id).add(new DeparturePrediction(x));
            });
        });
    }

    public static Collection<DeparturePrediction> getTrainDeparturePredictions(UUID trainId, Level level) {
        final Map<UUID, Set<TrackEdge>> edges = level != null ? getAllEdgesMapped() : new HashMap<>();
        final Map<String, Set<GlobalStation>> stationsByName = level != null ? getAllStations().stream().collect(Collectors.groupingBy(x -> x.name, Collectors.toSet())) : new HashMap<>();

        Collection<DeparturePrediction> preds = Gott().values().stream().flatMap(x -> x.stream()).filter(x -> x.train.id.equals(trainId)).map(x -> {
            DeparturePrediction prediction = new DeparturePrediction(x);
            if (stationsByName.containsKey(prediction.getStationName())) {
                Set<Side> exitSides = stationsByName.get(prediction.getStationName()).stream().filter(a -> edges.containsKey(a.id)).map(a -> getTrainStationExit(a, Direction.fromYRot(angleOn(a, edges.get(a.id).stream().findFirst().get())), level)).collect(Collectors.toSet());
                if (exitSides.size() == 1) {
                    prediction.setExit(new TrainExit(exitSides.stream().findFirst().get(), Direction.NORTH));
                }
            }
            return prediction;
        }).toList();
        return preds;
    }

    public static Collection<TrainStop> getTrainStopsSorted(UUID trainId, Level level) {
        return getTrainDeparturePredictions(trainId, level).stream().map(x -> new TrainStop(x.getNextStop(), x)).sorted(Comparator.comparingInt(x -> x.getPrediction().getTicks())).toList();
    }

    public static Set<SimpleTrainConnection> getConnectionsAt(String stationName, UUID currentTrainId, int ticksToNextStop) {
        TrainStationAlias alias = GlobalSettingsManager.getInstance().getSettingsData().getAliasFor(stationName);
        SimpleTrainSchedule ownSchedule = SimpleTrainSchedule.of(getTrainStopsSorted(currentTrainId, null));
        GlobalTrainDisplayData.refresh();

        List<SimulatedTrainSchedule> excludedSchedules = new ArrayList<>();
        Map<DeparturePrediction, SimpleTrainSchedule> scheduleByPrediction = new HashMap<>();
        Map<DeparturePrediction, SimulatedTrainSchedule> simulatedScheduleByPrediction = new HashMap<>();

        return Gott().entrySet().stream().filter(x -> alias.contains(x.getKey())).map(x -> x.getValue())
                .flatMap(x -> x.parallelStream().map(y -> new DeparturePrediction(y)))
                .peek(x -> {
                    SimpleTrainSchedule schedule = SimpleTrainSchedule.of(getTrainStopsSorted(x.getTrain().id, null));
                    scheduleByPrediction.put(x, schedule);
                    simulatedScheduleByPrediction.put(x, schedule.simulate(x.getTrain(), ticksToNextStop, alias));
                })
                .sorted(Comparator.comparingInt(x -> simulatedScheduleByPrediction.get(x).getSimulationData().simulationCorrection()))
                .filter(x -> {
                    SimpleTrainSchedule schedule = scheduleByPrediction.get(x);
                    SimulatedTrainSchedule directionalSchedule = simulatedScheduleByPrediction.get(x);

                    if (excludedSchedules.stream().anyMatch(y -> y.exactEquals(directionalSchedule))) {
                        return false;
                    }

                    boolean b = !x.getTrain().id.equals(currentTrainId) &&
                            !schedule.equals(ownSchedule) &&
                            TrainUtils.isTrainValid(x.getTrain()) &&
                            !GlobalSettingsManager.getInstance().getSettingsData().isTrainBlacklisted(x.getTrain());

                    if (b) {
                        excludedSchedules.add(directionalSchedule);
                    }

                    return b;
                }).map(x -> {
                    SimulatedTrainSchedule sched = simulatedScheduleByPrediction.get(x);
                    Optional<TrainStop> firstStop = sched.getFirstStopOf(x.getNextStop());
                    return new SimpleTrainConnection(
                        x.getTrain().name.getString(),
                        x.getTrain().id,
                        x.getTrain().icon.getId(),
                        sched.getSimulationData().simulationTime() + sched.getSimulationData().simulationCorrection(),
                        firstStop.isPresent() ? firstStop.get().getPrediction().getScheduleTitle() : x.getScheduleTitle(),
                        firstStop.isPresent() ? firstStop.get().getStationAlias().getInfoForStation(x.getStationName()) : x.getInfo()
                    );
                }).sorted(Comparator.comparingInt(x -> x.ticks())).collect(Collectors.toSet());
                
    }

    public static boolean GottKnows(String station) {
        return Gott().keySet().stream().anyMatch(x -> {
            String regex = x.isBlank() ? x : "\\Q" + x.replace("*", "\\E.*\\Q") + "\\E";
            return station.matches(regex);
        });
    }

    /**
     * A list of all stations in the world.
     * @return a list containing all track stations.
     */
    public static Collection<GlobalStation> getAllStations() {        
        final Collection<GlobalStation> stations = new ArrayList<>();
        RAILWAY_MANAGER.trackNetworks.forEach((uuid, graph) -> {
            Collection<GlobalStation> foundStations = graph.getPoints(EdgePointType.STATION);
            stations.addAll(foundStations);
        });
        return stations;
    }

    public static Collection<TrackEdge> getAllEdges() {
        final Set<TrackEdge> edges = new HashSet<>();
        RAILWAY_MANAGER.trackNetworks.forEach((uuid, graph) -> {
            edges.addAll(graph.getNodes().stream().map(x -> graph.locateNode(x)).flatMap(x -> graph.getConnectionsFrom(x).values().stream()).collect(Collectors.toSet()));
        });
        return edges;
    }

    public static Map<UUID, Set<TrackEdge>> getAllEdgesMapped() {
        final Map<UUID, Set<TrackEdge>> edges = new HashMap<>();
        RAILWAY_MANAGER.trackNetworks.forEach((uuid, graph) -> {
            edges.putAll(graph.getNodes().stream()
                .map(x -> graph.locateNode(x))
                .flatMap(x -> graph.getConnectionsFrom(x).values().stream())
                .distinct()
                .flatMap(edge -> edge.getEdgeData().getPoints().stream().map(point -> new AbstractMap.SimpleEntry<>(point.id, edge)))
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toSet())))
            );
        });
        return edges;
    }

    public static Optional<TrackEdge> getEdgeForStation(GlobalStation station) {
        return getAllEdges().stream().filter(x -> x.getEdgeData().getPoints().stream().anyMatch(y -> y.equals(station))).findFirst();
    }

    public static NearestTrackStationResult getNearestTrackStation(Level level, Vec3i pos) {        
        Optional<GlobalStation> station = getAllStations().stream().filter(x ->
            GottKnows(x.name) &&
            x.getBlockEntityDimension().equals(level.dimension()) &&
            !GlobalSettingsManager.getInstance().getSettingsData().isBlacklisted(x.name)
        ).min((a, b) -> Double.compare(a.getBlockEntityPos().distSqr(pos), b.getBlockEntityPos().distSqr(pos)));

        double distance = station.isPresent() ? station.get().getBlockEntityPos().distSqr(pos) : 0;
        return new NearestTrackStationResult(station, distance);
    }

    public static double getStationAngle(GlobalStation station) {
        return angleOn(station, getEdgeForStation(station).get());
    }

    public static Direction getStationDirection(GlobalStation station) {
        return Direction.fromYRot(getStationAngle(station));
    }

    public static double angleOn(TrackEdgePoint point, TrackEdge edge) {
        double basePos = point.isPrimary(edge.node1) ? edge.getLength() - point.position : point.position;
        Vec3 vec = edge.getDirectionAt(basePos);
        return point.isPrimary(edge.node1) ? MathUtils.getVectorAngle(vec) : MathUtils.getVectorAngle(vec.reverse());
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

    /**
     * 
     * @param station
     * @param server
     * @return 1 = right, -1 = left, 0 = unknown
     */
    public static TrainExit getTrainStationExitDirection(GlobalStation station, Level level) {
        DoorControlBehaviour dcb = getTrainStationDoorControl(station, level);
        if (dcb == null) {
            return TrainExit.def();
        }
        Direction stationDirection = getStationDirection(station);

        if (dcb.mode.matches(stationDirection.getClockWise())) {
            return new TrainExit(Side.RIGHT, stationDirection.getClockWise());
        } else if (dcb.mode.matches(stationDirection.getCounterClockWise())) {
            return new TrainExit(Side.LEFT, stationDirection.getCounterClockWise());
        }
        return TrainExit.def();
    }

    public static Side getTrainStationExit(GlobalStation station, Direction stationDirection, Level level) {
        DoorControlBehaviour dcb = getTrainStationDoorControl(station, level);
        if (dcb == null) {
            return Side.UNKNOWN;
        }

        if (dcb.mode.matches(stationDirection.getClockWise())) {
            return Side.RIGHT;
        } else if (dcb.mode.matches(stationDirection.getCounterClockWise())) {
            return Side.LEFT;
        }
        return Side.UNKNOWN;
    }



    /**
     * A list of all trains in the world.
     * @return a list containing all trains.
     */
    public static Collection<Train> getAllTrains() {
        return RAILWAY_MANAGER.trains.values();
    }

    public static Train getTrain(UUID trainId) {
        return RAILWAY_MANAGER.trains.get(trainId);
    }

    public static boolean isTrainValid(Train train) {
        return !train.derailed &&
               !train.invalid &&
               !train.runtime.paused &&
               train.runtime.getSchedule() != null &&
               train.graph != null
        ;
    }

    public static boolean isTrainIdValid(UUID trainId) {
        return isTrainValid(getTrain(trainId));
    }
}
