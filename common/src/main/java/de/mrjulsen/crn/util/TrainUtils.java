package de.mrjulsen.crn.util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
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
import de.mrjulsen.crn.data.TrainExitSide;
import de.mrjulsen.crn.data.settings.GlobalSettings;
import de.mrjulsen.crn.event.ModCommonEvents;
import de.mrjulsen.mcdragonlib.config.ECachingPriority;
import de.mrjulsen.mcdragonlib.util.Cache;
import de.mrjulsen.mcdragonlib.util.Holder.MutableHolder;
import de.mrjulsen.mcdragonlib.util.math.MathUtils;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class TrainUtils {

    private static final int MAX_CACHED_PATTERNS = 1024;

    private static final Map<String, Pattern> patternCache = new ConcurrentHashMap<>();

    private static final Cache<Map<UUID, SignalBoundary>> signalsByIdCache = new Cache<>(() -> {
        Map<UUID, SignalBoundary> byId = new HashMap<>();
        for (SignalBoundary signal : getAllSignals()) {
            byId.put(signal.getId(), signal);
        }
        return byId;
    }, ECachingPriority.LOWEST);

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

    public static void refreshCache() {
        allStationsCache.clear();
        allSignalsCache.clear();
        signalsByIdCache.clear();
        allStationNamesCache.clear();
        allTrainNames.clear();
    }

    private TrainUtils() {}

    public static GlobalRailwayManager getRailwayManager() {
        return Create.RAILWAYS;
    }

    public static boolean stationExists(String stationName) {
        for (GlobalStation station : getAllStations()) {
            if (station.name.equals(stationName)) {
                return true;
            }
        }
        return false;
    }

    public static Collection<GlobalStation> getAllStations() {
        return allStationsCache.get();
    }

    public static Set<String> getAllStationNames() {
        return allStationNamesCache.get();
    }

    public static Optional<Train> getTrain(UUID trainId) {
        if (trainId == null) {
            return Optional.empty();
        }
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

    public static Set<Train> isSignalOccupied(UUID signalId, Set<UUID> excludedTrains) {
        SignalBoundary signal = signalsByIdCache.get().get(signalId);
        if (signal == null) {
            return Set.of();
        }

        UUID firstGroup = signal.groups.getFirst();
        UUID secondGroup = signal.groups.getSecond();

        Set<Train> occupyingTrains = new HashSet<>();
        for (Train train : getTrains(false)) {
            if (excludedTrains.contains(train.id)) {
                continue;
            }

            boolean isOccupyingSignal = false;
            for (UUID occupiedSignal : train.occupiedSignalBlocks.keySet()) {
                if (occupiedSignal.equals(firstGroup) || occupiedSignal.equals(secondGroup)) {
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
        if (station == null) {
            return TrainExitSide.UNKNOWN;
        }
        Level level = levelOf(station);
        if (level == null || !level.isLoaded(station.getBlockEntityPos())) {
            return TrainExitSide.UNKNOWN;
        }
        Optional<TrackEdge> edge = getEdge(station);
        if (edge.isEmpty()) {
            return TrainExitSide.UNKNOWN;
        }
        return getTrainStationExit(station, Direction.fromYRot(angleOn(station, edge.get())), level);
    }

    private static Level levelOf(GlobalStation station) {
        ResourceKey<Level> dimension = station.getBlockEntityDimension();
        if (dimension == null) {
            return ModCommonEvents.getPhysicalLevel();
        }
        return ModCommonEvents.getCurrentServer()
            .<Level>map(server -> server.getLevel(dimension))
            .orElseGet(ModCommonEvents::getPhysicalLevel);
    }


    public static boolean stationMatches(String stationName, String filter) {
        if (stationName == null || filter == null) {
            return false;
        }
        if (!ModUtils.isGlobPattern(filter)) {
            return stationName.equals(filter);
        }
        return patternOf(filter).matcher(stationName).matches();
    }

    private static Pattern patternOf(String filter) {
        Pattern cached = patternCache.get(filter);
        if (cached != null) {
            return cached;
        }
        if (patternCache.size() >= MAX_CACHED_PATTERNS) {
            patternCache.clear();
        }
        Pattern compiled = ModUtils.buildPattern(filter);
        patternCache.put(filter, compiled);
        return compiled;
    }

    public static boolean isTrainValid(Train train) {
        return
               !train.invalid &&
               train.runtime.getSchedule() != null &&
               train.graph != null
        ;
    }

    public static boolean isTrainUsable(Train train) {
        return isTrainValid(train);
    }
}
