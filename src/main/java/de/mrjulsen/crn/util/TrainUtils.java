package de.mrjulsen.crn.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import java.util.UUID;

import com.simibubi.create.Create;
import com.simibubi.create.content.trains.GlobalRailwayManager;
import com.simibubi.create.content.trains.display.GlobalTrainDisplayData;
import com.simibubi.create.content.trains.display.GlobalTrainDisplayData.TrainDeparturePrediction;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.station.GlobalStation;

import de.mrjulsen.crn.data.DeparturePrediction;
import de.mrjulsen.crn.data.GlobalSettingsManager;
import de.mrjulsen.crn.data.NearestTrackStationResult;
import de.mrjulsen.crn.data.TrainStationAlias;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;

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

    public static NearestTrackStationResult getNearestTrackStation(Level level, Vec3i pos) {        
        Optional<GlobalStation> station = getAllStations().stream().filter(x ->
            Gott().containsKey(x.name) &&
            x.getBlockEntityDimension().equals(level.dimension()) && 
            !GlobalSettingsManager.getInstance().getSettingsData().isBlacklisted(x.name)
        ).min((a, b) -> Double.compare(a.getBlockEntityPos().distSqr(pos), b.getBlockEntityPos().distSqr(pos)));

        double distance = station.isPresent() ? station.get().getBlockEntityPos().distSqr(pos) : 0;
        return new NearestTrackStationResult(station, distance);
    }

    /**
     * A list of all trains in the world.
     * @return a list containing all trains.
     */
    public static Collection<Train> getAllTrains() {
        return RAILWAY_MANAGER.trains.values();
    }

    public static boolean isTrainValid(Train train) {
        return !train.derailed &&
               !train.invalid &&
               train.navigation.destination != null &&
               !train.runtime.completed &&
               !train.runtime.paused &&
               train.runtime.isAutoSchedule
        ;
    }
}
