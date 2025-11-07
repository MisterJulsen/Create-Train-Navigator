package de.mrjulsen.crn.network.packets.pain;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.TagName;
import de.mrjulsen.crn.data.UserSettings;
import de.mrjulsen.crn.data.navigation.ClientRoute;
import de.mrjulsen.crn.data.navigation.Route;
import de.mrjulsen.crn.data.navigation.RoutePart;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.data.train.ScheduleSection;
import de.mrjulsen.crn.data.train.TrainData;
import de.mrjulsen.crn.data.train.TrainListener;
import de.mrjulsen.crn.data.train.TrainPrediction;
import de.mrjulsen.crn.data.train.TrainStop;
import de.mrjulsen.crn.data.train.TrainUtils;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import de.mrjulsen.mcdragonlib.util.Pair;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public class GetDepartureAndArrivalRoutesAtPacketData {

    private static final String NBT_STATION_TAG_NAME = "StationTagName";
    private static final String NBT_ID = "Id";
    private static final String NBT_DATA = "Data";
    private static final String NBT_IS_ARRIVAL = "IsArrival";

    public static class Request extends NetworkPacketData {
        
        private String stationTagName;
        private UUID playerId;

        public Request(DLStatus status) {
            super(status);
        }

        public Request(String stationTagName, UUID playerId) {
            super(DLStatus.OK);
            this.stationTagName = stationTagName;
            this.playerId = playerId;
        }

        @Override
        protected void write(CompoundTag nbt) {
            nbt.putString(NBT_STATION_TAG_NAME, stationTagName);
            nbt.putUUID(NBT_ID, playerId);
        }

        @Override
        protected void read(CompoundTag nbt) {
            this.stationTagName = nbt.getString(NBT_STATION_TAG_NAME);
            this.playerId = nbt.getUUID(NBT_ID);
        }
    }

    public static class Response extends NetworkPacketData {
        private List<Pair<Boolean, Route>> rawData;
        private List<Pair<Boolean, ClientRoute>> data;

        public Response(DLStatus status) {
            super(status);
        }

        public Response(List<Pair<Boolean, Route>> rawData) {
            super(DLStatus.OK);
            this.rawData = rawData;
        }

        @Override
        protected void write(CompoundTag nbt) {
            ListTag list = new ListTag();
            for (Pair<Boolean, Route> pair : rawData) {
                CompoundTag sub = new CompoundTag();
                sub.putBoolean(NBT_IS_ARRIVAL, pair.getFirst());
                sub.put(NBT_DATA, pair.getSecond().toNbt());
                list.add(sub);
            }
            nbt.put(NBT_DATA, list);
        }

        @Override
        protected void read(CompoundTag nbt) {
            this.data = nbt.getList(NBT_DATA, Tag.TAG_COMPOUND).stream().map(x -> {
                CompoundTag tag = (CompoundTag)x;
                return new Pair<>(tag.getBoolean(NBT_IS_ARRIVAL), ClientRoute.fromNbt(tag.getCompound(NBT_DATA), false));
            }).toList();
        }

        public List<Pair<Boolean, ClientRoute>> getData() {
            return data;
        }
        
    }

    public static Response handle(Request packet, NetworkPacketContext context) {
        try {            
            UserSettings settings = UserSettings.getSettingsFor(packet.playerId, true);
            StationTag station = GlobalSettings.getInstance().getOrCreateStationTagFor(TagName.of(packet.stationTagName));
            Set<Train> trains = TrainUtils.getDepartingTrainsAt(station);
            trains.removeIf(x -> 
                !TrainUtils.isTrainUsable(x) ||
                GlobalSettings.getInstance().isTrainBlacklisted(x) ||
                !TrainListener.hasTrainData(x)
            );

            List<Pair<Boolean, Route>> routesL = new LinkedList<>();
            for (Train train : trains) {
                TrainData data = TrainListener.getTrainData(train.id).get();
                List<TrainPrediction> matchingPredictions = data.getPredictionsChronologically();
                
                for (int i = 0; i < matchingPredictions.size(); i++) {
                    TrainPrediction prediction = matchingPredictions.get(i);
                    if (!prediction.getStationTag().equals(station)) {
                        continue;
                    }

                    ScheduleSection section = prediction.getSection();
                    if ((!section.isUsable() && !(section.isFirstStop(prediction) && section.previousSection().isUsable() && section.previousSection().shouldIncludeNextStationOfNextSection())) || (section.getTrainCategory().map(x -> settings.searchExcludedTrainCaegories.getValue().contains(x.getId())).orElse(false))) {
                        continue;
                    }

                    ScheduleSection previousSection = section.previousSection();

                    boolean isStart = section.isFirstStop(prediction); 
                    boolean isLast = section.isFinalStop(prediction); 
                    boolean isStartAndFinal = isStart && previousSection.isUsable() && previousSection.shouldIncludeNextStationOfNextSection() && (previousSection.getTrainCategory().map(x -> !settings.searchExcludedTrainCaegories.getValue().contains(x.getId())).orElse(true)); 
                    
                    TrainStop stop = new TrainStop(prediction);
                    stop.simulateTicks(settings.searchDepartureInTicks.getValue());
                    TrainPrediction fromPrediction = section.getFirstStop().get();
                    TrainStop from = new TrainStop(fromPrediction);

                    Route route = new Route(List.of(new RoutePart(data.getSessionId(), train.id, List.of(stop /* current/target */, from /* from */), section.getAllStops(settings.searchDepartureInTicks.getValue(), prediction.getEntryIndex()))), false);
                    
                    if ((!isStart || isStartAndFinal) && (section.getTrainCategory().map(x -> !settings.searchExcludedTrainCaegories.getValue().contains(x.getId())).orElse(true))) {
                        
                        Route selectedRoute = route;
                        if (isStartAndFinal) {
                            TrainPrediction frPred = previousSection.getFirstStop().get();
                            TrainStop fr = new TrainStop(frPred);
                            selectedRoute = new Route(List.of(new RoutePart(data.getSessionId(), train.id, List.of(stop /* current/target */, fr /* from */), previousSection.getAllStops(settings.searchDepartureInTicks.getValue(), prediction.getEntryIndex()))), false);
                        }
                        routesL.add(Pair.of(true, selectedRoute)); // Arrival
                    }
                    if ((section.isUsable() && (!isLast || section.shouldIncludeNextStationOfNextSection())) && (section.getTrainCategory().map(x -> !settings.searchExcludedTrainCaegories.getValue().contains(x.getId())).orElse(true))) {
                        routesL.add(Pair.of(false, route)); // Departure
                    }
                }
            }
            
            Collections.sort(routesL, (a, b) -> {
                long val1 = a.getFirst() ? a.getSecond().getStart().getScheduledArrivalTime() : a.getSecond().getStart().getScheduledDepartureTime();
                long val2 = b.getFirst() ? b.getSecond().getStart().getScheduledArrivalTime() : b.getSecond().getStart().getScheduledDepartureTime();
                return Long.compare(val1, val2);
            });
            return new Response(routesL);
        } catch (Exception e) {
            CreateRailwaysNavigator.LOGGER.error("Schedule board generation error.", e);
        }
        return new Response(List.of());
    }
    
}
