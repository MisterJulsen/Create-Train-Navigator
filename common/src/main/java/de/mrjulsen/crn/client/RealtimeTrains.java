package de.mrjulsen.crn.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import de.mrjulsen.crn.backend.api.JourneySnapshot;
import de.mrjulsen.crn.backend.api.StopSnapshot;
import de.mrjulsen.crn.backend.api.TrainSnapshot;
import de.mrjulsen.crn.network.packets.pain.GetTrainRealtimePacketData;
import de.mrjulsen.crn.registry.ModNetworkManager;
import de.mrjulsen.crn.navigator.route.RouteCall;
import de.mrjulsen.crn.navigator.route.RouteLeg;
import de.mrjulsen.mcdragonlib.network.NetworkDirection;

/**
 * The client's live mirror of the backend's train state.
 * <p>
 * This is the one place that polls, so nothing else has to. A holder
 * {@linkplain #watch(Collection, Runnable) watches} the trains it cares about for as long as it
 * needs them; watching is what drives the polling, and reference counting keeps a train that several
 * holders share polled just once. While a train is watched its latest {@link TrainSnapshot} and
 * {@link JourneySnapshot} are cached here.
 * <p>
 * Views do not read this directly. A route is kept current by a
 * {@link de.mrjulsen.crn.client.journey.JourneyTracker}, which is what watches trains and writes what
 * they report into the route itself; a view then simply reads the route. The projection that makes
 * that possible is {@link #liveCall(RouteLeg, RouteCall)}, which restates a planned call with the
 * times the train is currently running it to.
 */
public final class RealtimeTrains {

    private RealtimeTrains() {}

    /** A view's interest in a set of trains. Releasing it stops them being polled on its behalf. */
    public interface Watch {
        /** Idempotent; the trains stay polled as long as any other watch still holds them. */
        void release();
    }

    private static final Map<UUID, Integer> watchCounts = new ConcurrentHashMap<>();
    private static final Map<UUID, List<Runnable>> updateHooks = new ConcurrentHashMap<>();
    private static final Map<UUID, TrainSnapshot> trains = new ConcurrentHashMap<>();
    private static final Map<UUID, JourneySnapshot> journeys = new ConcurrentHashMap<>();

    public static int debug_watchedTrainCount() {
        return watchCounts.size();
    }

    /** Watches a single train, with an optional callback fired whenever its state refreshes. */
    public static Watch watch(UUID trainId, Runnable onUpdate) {
        return watch(List.of(trainId), onUpdate);
    }

    /**
     * Watches the given trains for as long as the returned handle lives.
     *
     * @param onUpdate Run whenever any of these trains reports fresh state, or {@code null} for a
     *                 view that only pulls at render time and needs no push.
     */
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

    /** The latest state of the given train, if it is watched and has been reached. */
    public static Optional<TrainSnapshot> train(UUID trainId) {
        return Optional.ofNullable(trains.get(trainId));
    }

    /** The latest whole journey of the given train, if it is watched and has been reached. */
    public static Optional<JourneySnapshot> journey(UUID trainId) {
        return Optional.ofNullable(journeys.get(trainId));
    }

    /**
     * The given call as the train is currently running it, rather than as it was projected when the
     * route was searched. This is what a {@link de.mrjulsen.crn.client.journey.JourneyTracker} writes
     * back into the route; the returned call is a throwaway carrier for those two values.
     * <p>
     * The call names a particular visit of its stop ({@link RouteCall#cycle()}); the train's current
     * run is some number of cycles before that, so the live stop is projected forward by the
     * difference, carrying its current delay with it. Falls back to the planned call while the train
     * is not watched, has not been reached, or no longer reports the stop.
     */
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
                return new RouteCall(projected.scheduledStation(), projected.realtimeStation(),
                    projected.entryIndex(), call.cycle(), projected.scheduled(), projected.realtime());
            })
            .orElse(call);
    }

    /** Polls every watched train once and refreshes the cache. Driven from the client tick. */
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
