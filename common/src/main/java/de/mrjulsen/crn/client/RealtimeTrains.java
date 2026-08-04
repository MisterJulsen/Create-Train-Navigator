package de.mrjulsen.crn.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import de.mrjulsen.crn.api.core.snapshot.JourneySnapshot;
import de.mrjulsen.crn.api.core.snapshot.StopSnapshot;
import de.mrjulsen.crn.api.core.snapshot.TrainSnapshot;
import de.mrjulsen.crn.network.packets.GetTrainRealtimePacketData;
import de.mrjulsen.crn.registry.ModNetworkManager;
import de.mrjulsen.crn.core.navigator.route.RouteCall;
import de.mrjulsen.crn.core.navigator.route.RouteLeg;
import de.mrjulsen.mcdragonlib.network.NetworkDirection;

public final class RealtimeTrains {

    private RealtimeTrains() {}

    public interface Watch {
        void release();
    }

    private static final Map<UUID, Integer> watchCounts = new ConcurrentHashMap<>();
    private static final Map<UUID, List<Runnable>> updateHooks = new ConcurrentHashMap<>();
    private static final Map<UUID, TrainSnapshot> trains = new ConcurrentHashMap<>();
    private static final Map<UUID, JourneySnapshot> journeys = new ConcurrentHashMap<>();

    public static int debug_watchedTrainCount() {
        return watchCounts.size();
    }

    public static Watch watch(UUID trainId, Runnable onUpdate) {
        return watch(List.of(trainId), onUpdate);
    }

    public static Watch watch(Collection<UUID> trainIds, Runnable onUpdate) {
        List<UUID> watched = new ArrayList<>(trainIds);
        for (UUID trainId : watched) {
            watchCounts.merge(trainId, 1, Integer::sum);
            if (onUpdate != null) {
                updateHooks.computeIfAbsent(trainId, x -> new CopyOnWriteArrayList<>()).add(onUpdate);
            }
        }
        return new Watch() {
            private boolean released;

            @Override
            public void release() {
                if (released) {
                    return;
                }
                released = true;
                for (UUID trainId : watched) {
                    if (onUpdate != null) {
                        List<Runnable> hooks = updateHooks.get(trainId);
                        if (hooks != null) {
                            hooks.remove(onUpdate);
                            if (hooks.isEmpty()) {
                                updateHooks.remove(trainId);
                            }
                        }
                    }
                    watchCounts.computeIfPresent(trainId, (id, count) -> count <= 1 ? null : count - 1);
                    if (!watchCounts.containsKey(trainId)) {
                        trains.remove(trainId);
                        journeys.remove(trainId);
                    }
                }
            }
        };
    }

    public static Optional<TrainSnapshot> train(UUID trainId) {
        return Optional.ofNullable(trains.get(trainId));
    }

    public static Optional<JourneySnapshot> journey(UUID trainId) {
        return Optional.ofNullable(journeys.get(trainId));
    }

    public static RouteCall liveCall(RouteLeg leg, RouteCall call) {
        JourneySnapshot snapshot = journeys.get(leg.trainId());
        if (snapshot == null) {
            return call;
        }
        return snapshot.stops().stream()
            .filter(stop -> stop.entryIndex() == call.entryIndex())
            .findFirst()
            .map(stop -> {
                StopSnapshot projected = stop.advancedBy(call.cycle() - stop.completedVisits(), snapshot.totalDuration());
                return new RouteCall(projected.scheduledStation(), projected.station(),
                    projected.entryIndex(), call.cycle(), projected.scheduled(), projected.realtime());
            })
            .orElse(call);
    }

    public static void tick() {
        for (UUID trainId : List.copyOf(watchCounts.keySet())) {
            ModNetworkManager.GET_TRAIN_REALTIME.send(NetworkDirection.toServer(), new GetTrainRealtimePacketData.Request(trainId, true), (response) -> {
                if (!watchCounts.containsKey(trainId)) {
                    return;
                }
                response.getTrain().ifPresentOrElse(x -> trains.put(trainId, x), () -> trains.remove(trainId));
                response.getJourney().ifPresentOrElse(x -> journeys.put(trainId, x), () -> journeys.remove(trainId));
                List<Runnable> hooks = updateHooks.get(trainId);
                if (hooks != null) {
                    hooks.forEach(Runnable::run);
                }
            }, () -> {});
        }
    }

    public static void clear() {
        watchCounts.clear();
        updateHooks.clear();
        trains.clear();
        journeys.clear();
    }
}
