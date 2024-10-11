package de.mrjulsen.crn.data.navigation;

import java.util.Set;
import java.util.UUID;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.UserSettings;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.data.train.TrainStop;
import de.mrjulsen.crn.data.train.TrainStatus.CompiledTrainStatus;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.data.Pair;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public class RoutePart implements Comparable<RoutePart> {

    protected static final String NBT_SESSION_ID = "SessionId";
    protected static final String NBT_TRAIN_ID = "TrainId";
    protected static final String NBT_STOPS = "Stops";
    protected static final String NBT_JOURNEY = "EntireJourney";

    protected final UUID sessionId;
    protected final UUID trainId;
    protected final List<TrainStop> routeStops;
    protected final List<TrainStop> allStops;
    protected boolean cancelled;
    protected final Set<CompiledTrainStatus> status = new HashSet<>();

    public static RoutePart get(UUID sessionId, TrainSchedule schedule, StationTag from, StationTag to, UserSettings settings) {
        List<TrainStop> stops = getBetween(schedule, from, to, settings).stream().findFirst().orElse(List.of());
        List<TrainStop> entireJourney = List.of();
        if (stops.isEmpty()) {
            return null;
        } else {
            TrainStop firstStop = stops.get(0);
            TrainStop lastStop = stops.get(stops.size() - 1);
            entireJourney = TrainSchedule.ofSectionForIndex(sessionId, schedule.getTrain(), lastStop.getScheduleIndex(), firstStop.getScheduleIndex(), firstStop.getSimulationTime()).getAllStops();
        }
        return new RoutePart(sessionId, schedule.getTrain().id, stops, entireJourney);
    }

    public RoutePart(UUID sessionId, TrainSchedule schedule) {
        this(
            sessionId,
            schedule.getTrain().id,
            schedule.getAllStops(),
            TrainSchedule.ofSectionForIndex(
                sessionId,
                schedule.getTrain(),
                schedule.getAllStops().isEmpty() ? 0 : schedule.getAllStops().get(schedule.getAllStops().size() - 1).getScheduleIndex(),
                schedule.getAllStops().isEmpty() ? 0 : schedule.getAllStops().get(0).getScheduleIndex(),
                schedule.getAllStops().isEmpty() ? 0 : schedule.getAllStops().get(0).getSimulationTime()
            ).getAllStops()
        );
    }

    public RoutePart(UUID sessionId, UUID trainId, List<TrainStop> routeStops, List<TrainStop> allStops) {
        this.sessionId = sessionId;
        this.routeStops = routeStops;
        this.trainId = trainId;
        this.allStops = allStops;
    }

    public boolean isEmpty() {
        return routeStops.isEmpty();
    }

    public TrainStop getFirstStop() {
        return routeStops.get(0);
    }

    public TrainStop getLastStop() {
        return routeStops.get(routeStops.size() - 1);
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getTrainId() {
        return trainId;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public List<TrainStop> getStopovers() {
        return routeStops.size() <= 2 ? List.of() : ImmutableList.copyOf(routeStops.subList(1, routeStops.size() - 1));
    }

    public ImmutableList<TrainStop> getAllStops() {
        return ImmutableList.copyOf(routeStops);
    }

    public ImmutableList<TrainStop> getAllJourneyStops() {
        return ImmutableList.copyOf(allStops);
    }

    public long departureIn() {
        return getFirstStop().getScheduledDepartureTime() - DragonLib.getCurrentWorldTime();
    }

    public long arrivalIn() {
        return getFirstStop().getScheduledArrivalTime() - DragonLib.getCurrentWorldTime();
    }

    public long travelTime() {
        return getLastStop().getScheduledArrivalTime() - getFirstStop().getScheduledDepartureTime();
    }

    public long timeUntilEnd() {
        return departureIn() + travelTime();
    }

    public Set<CompiledTrainStatus> getStatus() {
        return ImmutableSet.copyOf(status);
    }

    private static Set<List<TrainStop>> getBetween(TrainSchedule schedule, StationTag start, StationTag end, UserSettings settings) {
        List<TrainStop> stops = schedule.getAllStops().stream().sorted((a, b) -> Long.compare(a.getScheduledDepartureTime(), b.getScheduledDepartureTime())).toList();
        
        if (stops.stream().noneMatch(x -> x.getTag().equals(start)) || stops.stream().noneMatch(x -> x.getTag().equals(end))) {
            return Set.of();
        }

        // Step 1: Select ALL possible pairs of start and end indices
        Set<Pair<Integer /* Start Index */, Integer /* End Index */>> sections = new HashSet<>();
        int firstStartIndex = -1;
        int startIndex = -1;

        for (int i = 0, index = 0; i < stops.size() + (firstStartIndex < 0 ? stops.size() : firstStartIndex); i++, index = i % stops.size()) {
            TrainStop stop = stops.get(index);
            if (stop.getTag().equals(start)) {
                startIndex = index;
                if (firstStartIndex < 0) firstStartIndex = index;
            }
            if (startIndex >= 0 && stop.getTag().equals(end)) {
                sections.add(new Pair<>(startIndex, index));
                startIndex = -1;
            }
        }

        if (sections.isEmpty()) {
            return Set.of();
        }

        // Step 2: Gather all stations between those indices.
        Set<List<TrainStop>> routeParts = new HashSet<>();
        for (Pair<Integer, Integer> startStopPair : sections) {
            if (GlobalSettings.getInstance().isTrainStationExcludedByUser(schedule.getTrain(), stops.get(startStopPair.getFirst()), settings) || GlobalSettings.getInstance().isTrainStationExcludedByUser(schedule.getTrain(), stops.get(startStopPair.getSecond()), settings)) {
                continue;
            }

            List<TrainStop> stopovers = new ArrayList<>();

            int index = startStopPair.getFirst() - 1;
            do {
                index++;
                TrainStop stop = stops.get(index % stops.size());
                stop.simulateCycles(index / stops.size());
                stopovers.add(stop);
            } while (index % stops.size() != startStopPair.getSecond());
            
            routeParts.add(stopovers);
        }
        return routeParts;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putUUID(NBT_SESSION_ID, sessionId);
        nbt.putUUID(NBT_TRAIN_ID, trainId);
        ListTag stopsList = new ListTag();
        stopsList.addAll(routeStops.stream().map(x -> x.toNbt(true)).toList());
        nbt.put(NBT_STOPS, stopsList);
        ListTag journeyList = new ListTag();
        journeyList.addAll(allStops.stream().map(x -> x.toNbt(true)).toList());
        nbt.put(NBT_JOURNEY, journeyList);
        return nbt;
    }

    public static RoutePart fromNbt(CompoundTag nbt) {
        return new RoutePart(
            nbt.contains(NBT_SESSION_ID) ? nbt.getUUID(NBT_SESSION_ID) : new UUID(0, 0),
            nbt.getUUID(NBT_TRAIN_ID),
            nbt.getList(NBT_STOPS, Tag.TAG_COMPOUND).stream().map(x -> TrainStop.fromNbt((CompoundTag)x)).toList(),
            nbt.getList(NBT_JOURNEY, Tag.TAG_COMPOUND).stream().map(x -> TrainStop.fromNbt((CompoundTag)x)).toList()
        );
    }

    @Override
    public int compareTo(RoutePart o) {
        return Long.compare(departureIn(), o.departureIn());
    }
}
