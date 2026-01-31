package de.mrjulsen.crn.data.train;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.joml.Vector3f;

import com.simibubi.create.Create;
import com.simibubi.create.content.decoration.slidingDoor.DoorControlBehaviour;
import com.simibubi.create.content.trains.GlobalRailwayManager;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.signal.SignalBoundary;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.content.trains.station.StationBlockEntity;

import de.mrjulsen.crn.data.NearestTrackStationResult;
import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.TrainExitSide;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.event.ModCommonEvents;
import de.mrjulsen.crn.data.navigation.TrainSchedule;
import de.mrjulsen.mcdragonlib.config.ECachingPriority;
import de.mrjulsen.mcdragonlib.util.Cache;
import de.mrjulsen.mcdragonlib.util.MapCache;
import de.mrjulsen.mcdragonlib.util.Holder.MutableHolder;
import de.mrjulsen.mcdragonlib.util.math.MathUtils;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;

public final class TrainUtils {

    private static final Cache<Collection<GlobalStation>> allStationsCache = new Cache<>(() -> {
        final Collection<GlobalStation> stations = new ArrayList<>();
        getRailwayManager().trackNetworks.forEach((uuid, graph) -> {
            Collection<GlobalStation> foundStations = graph.getPoints(EdgePointType.STATION);
            stations.addAll(foundStations);
        });
        return stations;
    }, ECachingPriority.LOWEST);

    private static final Cache<Set<String>> allStationNamesCache = new Cache<>(() -> {
        return getAllStations().stream().map(x -> x.name).collect(Collectors.toSet());
    }, ECachingPriority.LOWEST);    

    private static final Cache<Set<String>> allTrainNames = new Cache<>(() -> {
        return getTrains(false).stream().map(x -> x.name.getString()).collect(Collectors.toSet());
    }, ECachingPriority.LOWEST);

    private static final Cache<Set<SignalBoundary>> allSignalsCache = new Cache<>(() -> {
        Set<SignalBoundary> signals = new HashSet<>();
        for (TrackGraph graph : getRailwayManager().trackNetworks.values()) {
            signals.addAll(graph.getPoints(EdgePointType.SIGNAL));
        }
        return signals;
    }, ECachingPriority.LOWEST);

    private static final MapCache<Set<Train>, StationTag, StationTag> departingTrainsAtTagCache = new MapCache<>((station) -> {
        Set<Train> trains = new HashSet<>();
        for (Map.Entry<String, Collection<TrainPrediction>> e : TrainListener.statusByDestination.entrySet()) {
            if (!station.contains(e.getKey())) {
                continue;
            }

            for (TrainPrediction pred : e.getValue()) {
                trains.add(pred.getData().getTrain());
            }
        }
        return trains;
    }, StationTag::hashCode, ECachingPriority.LOWEST);

    private static final MapCache<Set<Train>, String, String> departingTrainsAtStationCache = new MapCache<>((station) -> {
        Set<Train> trains = new HashSet<>();
        
        for (Map.Entry<String, Collection<TrainPrediction>> e : TrainListener.statusByDestination.entrySet()) {
            if (!station.equals(e.getKey())) {
                continue;
            }

            for (TrainPrediction pred : e.getValue()) {
                trains.add(pred.getData().getTrain());
            }
        }
        return trains;
    }, String::hashCode, ECachingPriority.LOWEST);
    
    private static record DeparturesFromTagContext(StationTag station, UUID selfTrain, boolean realTimeOnly, boolean allowDuplicates) {}

    private static final MapCache<List<TrainStop>, DeparturesFromTagContext, DeparturesFromTagContext> departuresAtTagCache = new MapCache<>((context) -> {
        return getDeparturesAt(x -> 
            !GlobalSettings.getInstance().isStationBlacklisted(x.getStationFilter()) && (
            x.getStationTag().equals(context.station()) ||
            (!context.realTimeOnly() && x.getEstimatedStationTag().equals(context.station())
        )), context.selfTrain(), context.allowDuplicates());
    }, DeparturesFromTagContext::hashCode, ECachingPriority.LOWEST);
    
    private static record DeparturesFromStationContext(String station, UUID selfTrain, boolean realTimeOnly, boolean allowDuplicates) {}

    private static final MapCache<List<TrainStop>, DeparturesFromStationContext, DeparturesFromStationContext> departuresAtStationCache = new MapCache<>((context) -> {
        return getDeparturesAt(x ->
            !GlobalSettings.getInstance().isStationBlacklisted(x.getStationFilter()) && (
            TrainUtils.stationMatches(x.getTargetedStationName(), context.station()) ||
            (!context.realTimeOnly() && TrainUtils.stationMatches(x.getScheduledStationName(), context.station())
        )), context.selfTrain(), context.allowDuplicates());
    }, DeparturesFromStationContext::hashCode, ECachingPriority.LOWEST);

    public static void refreshCache() {
        allStationsCache.clear();
        allSignalsCache.clear();
        departingTrainsAtTagCache.clearAll();
        departingTrainsAtStationCache.clearAll();
        departuresAtTagCache.clearAll();
        departuresAtStationCache.clearAll();
        allStationNamesCache.clear();
        allTrainNames.clear();
    }

    private TrainUtils() {}
    
    public static GlobalRailwayManager getRailwayManager() {
        return Create.RAILWAYS;
    }

    /**
     * Get data about all trains and when they arrive where.
     * @return a Map where the key is the station name and the value is a list of data from all trains that will arrive at this stations.
     */
    public static Map<String, Collection<TrainPrediction>> allPredictionsRaw() {
        return new HashMap<>(TrainListener.statusByDestination);
    }    

    public static boolean isStationKnown(String station) {
        for (String stationKey : allPredictionsRaw().keySet()) {
            if (TrainUtils.stationMatches(station, stationKey)) {
                return true;
            }
        }
        return false;
    }

    public static boolean stationExists(String stationName) {
        for (GlobalStation station : getAllStations()) {
            if (station.name.equals(stationName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A list of all stations in the world.
     * @return a list containing all track stations.
     */
    public static Collection<GlobalStation> getAllStations() {        
        return allStationsCache.get();
    }
    
    public static Set<String> getAllStationNames() {        
        return allStationNamesCache.get();
    }

    public static Optional<Train> getTrain(UUID trainId) { 
        return Optional.ofNullable(getRailwayManager().trains.get(trainId));
    }

    public static Set<UUID> getTrainIds() {
        return new HashSet<>(getRailwayManager().trains.keySet());
    }

    public static Set<String> getTrainNames() {
        return allTrainNames.get();
    }

    public static Set<Train> getTrains(boolean onlyValid) {
        Set<Train> trains = new HashSet<>();
        for (Train train : getRailwayManager().trains.values()) {
            if (onlyValid && !isTrainValid(train)) {
                continue;
            }
            trains.add(train);
        }
        return trains;
    }

    public static Set<SignalBoundary> getAllSignals() {        
        return allSignalsCache.get();
    }

    public static Set<Train> getDepartingTrainsAt(StationTag station) {
        return departingTrainsAtTagCache.get(station, station);
    }

    public static Set<Train> getDepartingTrainsAt(String station) {
        return departingTrainsAtStationCache.get(station, station);
    }

    /**
     * A list of all trains that will stop at the specified station.
     * @param station The station tag of the station
     * @param selfTrain The train that is asking for this information (to exclude it) or {@code null} for all departures.
     * @param realTimeOnly Whether only currently valid departures at this station should be returned or also trains that were intended to stop here but the track has changed.
     * @return A list of stops at this stations.
     */
    public static List<TrainStop> getDeparturesAt(StationTag station, UUID selfTrain, boolean realTimeOnly, boolean allowDuplicates) {
        DeparturesFromTagContext context = new DeparturesFromTagContext(station, selfTrain, realTimeOnly, allowDuplicates);
        return departuresAtTagCache.get(context, context);
    }
    /**
     * A list of all trains that will stop at the specified station.
     * @param stationName The station name of the station
     * @param selfTrain The train that is asking for this information (to exclude it) or {@code null} for all departures.
     * @param realTimeOnly Whether only currently valid departures at this station should be returned or also trains that were intended to stop here but the track has changed.
     * @return A list of stops at this stations.
     */
    public static List<TrainStop> getDeparturesAtStationName(String stationName, UUID selfTrain, boolean realTimeOnly, boolean allowDuplicates) {
        DeparturesFromStationContext context = new DeparturesFromStationContext(stationName, selfTrain, realTimeOnly, allowDuplicates);
        return departuresAtStationCache.get(context, context);
    }

    public static List<TrainStop> getDeparturesAt(Predicate<TrainPrediction> stationFilter, UUID selfTrain, boolean allowDuplicates) {
        MutableHolder<TrainSchedule> selfSchedule = new MutableHolder<TrainSchedule>(null);
        TrainUtils.getTrain(selfTrain).ifPresent(x -> {
            selfSchedule.set(new TrainSchedule(TrainListener.getTrainData(x.id).map(TrainData::getSessionId).orElse(new UUID(0, 0)), x));
        });
        
        List<TrainStop> stops = new ArrayList<>();
        for (TrainData data : TrainListener.getAllTrainData()) {

            if (data.getTrainId().equals(selfTrain) || !TrainUtils.isTrainUsable(data.getTrain())) {
                continue;
            }            

            for (TrainPrediction pred : data.getPredictions()) {
                if (!stationFilter.test(pred)) {
                    continue;
                }

                TrainStop stop = new TrainStop(pred);
                if (selfSchedule.get() == null) {
                    Optional<Train> train = TrainUtils.getTrain(stop.getTrainId());
                    if (!train.isPresent()) {
                        continue;
                    }
                    TrainSchedule sched = new TrainSchedule(TrainListener.getTrainData(train.get().id).map(TrainData::getSessionId).orElse(new UUID(0, 0)), train.get());
                    if (sched.isEqual(selfSchedule.get())) {
                        continue;
                    }
                }
                stops.add(stop);
            }

            Collections.sort(stops, (a, b) -> Long.compare(a.getScheduledDepartureTime(), b.getScheduledDepartureTime()));
        }

        List<TrainStop> results = new ArrayList<>();
        Set<UUID> usedTrains = new HashSet<>();
        usedTrains.add(selfTrain);
        for (TrainStop stop : stops) {
            if (!TrainListener.hasTrainData(stop.getTrainId())) continue;
            TrainData data = TrainListener.getTrainData(stop.getTrainId()).get();
            ScheduleSection section = data.getSectionByIndex(stop.getSectionIndex());
            if (!section.isUsable() && !(section.isFirstStop(stop.getScheduleIndex()) && section.previousSection().isUsable() && section.previousSection().shouldIncludeNextStationOfNextSection())) {
                continue;
            }
            
            if (!usedTrains.contains(stop.getTrainId()) || allowDuplicates) {
                usedTrains.add(stop.getTrainId());
                results.add(stop);
            }
        }

        return results;
    }

    public static Set<Train> isSignalOccupied(UUID signalId, Set<UUID> excludedTrains) {
        Optional<SignalBoundary> signal = Optional.empty();
        for (SignalBoundary s : getAllSignals()) {
            if (s.getId().equals(signalId)) {
                signal = Optional.of(s);
                break;
            }
        }
        if (!signal.isPresent()) {
            return Set.of();
        }

        Set<Train> occupyingTrains = new HashSet<>();
        for (Train train : getTrains(false)) {
            if (excludedTrains.contains(train.id)) {
                continue;
            }

            boolean isOccupyingSignal = false;
            for (UUID occupiedSignal : train.occupiedSignalBlocks.keySet()) {
                if (occupiedSignal.equals(signal.get().groups.getFirst()) || occupiedSignal.equals(signal.get().groups.getSecond())) {
                    isOccupyingSignal = true;
                    break;
                }
            }

            if (!isOccupyingSignal) {
                continue;
            }

            occupyingTrains.add(train);
        }

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
        MutableHolder<TrackEdge> edge = new MutableHolder<TrackEdge>(null);
        Create.RAILWAYS.trackNetworks.forEach((uuid, graph) -> {
            if (edge.get() != null) return;
            TrackEdge e = graph.getConnection(Couple.create(graph.locateNode(station.edgeLocation.getFirst()), graph.locateNode(station.edgeLocation.getSecond())));
            if (e == null) return;
            edge.set(e);
        });        
        return Optional.ofNullable(edge.get());
    }

    public static double angleOn(TrackEdgePoint point, TrackEdge edge) {
        double basePos = point.isPrimary(edge.node1) ? edge.getLength() - point.position : point.position;
        Vector3f vec = edge.getDirectionAt(basePos).toVector3f();
        return point.isPrimary(edge.node1) ? MathUtils.getVectorAngle(vec) : MathUtils.getVectorAngle(vec.negate());
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
               TrainListener.hasTrainData(train.id) &&
               TrainListener.getTrainData(train.id).get().isInitialized() && 
               !TrainListener.getTrainData(train.id).get().isPreInitializationPhase()
        ;
    }

    public static boolean canReadTrainSchedule(Train train) {
        return train != null && train.runtime != null && train.runtime.getSchedule() != null;
    }

    public static boolean canReadTrainNavigation(Train train) {
        return train != null && train.navigation != null;
    }
}
