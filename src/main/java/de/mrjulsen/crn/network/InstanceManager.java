package de.mrjulsen.crn.network;

import java.util.Map;
import java.util.function.Consumer;

import de.mrjulsen.crn.data.NearestTrackStationResult;
import de.mrjulsen.crn.data.SimpleRoute;

import java.util.HashMap;
import java.util.List;

public class InstanceManager {

    private static final Map<Long, Runnable> CLIENT_RESPONSE_RECEIVED_ACTION = new HashMap<>();
    private static final Map<Long, Consumer<List<SimpleRoute>>> CLIENT_NAVIGATION_RESPONSE_ACTION = new HashMap<>();
    private static final Map<Long, Consumer<NearestTrackStationResult>> CLIENT_NEAREST_STATION_RESPONSE_ACTION = new HashMap<>();
    
    public static long registerClientResponseReceievedAction(Runnable runnable) {
        long id = System.nanoTime();
        CLIENT_RESPONSE_RECEIVED_ACTION.put(id, runnable);
        return id;
    }

    public static void runClientResponseReceievedAction(long id) {
        if (CLIENT_RESPONSE_RECEIVED_ACTION.containsKey(id)) {
            Runnable run = CLIENT_RESPONSE_RECEIVED_ACTION.remove(id);
            if (run != null)
                run.run();
        }
    }

    public static long registerClientNavigationResponseAction(Consumer<List<SimpleRoute>> consumer) {
        long id = System.nanoTime();
        CLIENT_NAVIGATION_RESPONSE_ACTION.put(id, consumer);
        return id;
    }

    public static void runClientNavigationResponseAction(long id, List<SimpleRoute> routes) {
        if (CLIENT_NAVIGATION_RESPONSE_ACTION.containsKey(id)) {
            Consumer<List<SimpleRoute>> action = CLIENT_NAVIGATION_RESPONSE_ACTION.remove(id);
            if (action != null)
                action.accept(routes);
        }
    }

     public static long registerClientNearestStationResponseAction(Consumer<NearestTrackStationResult> consumer) {
        long id = System.nanoTime();
        CLIENT_NEAREST_STATION_RESPONSE_ACTION.put(id, consumer);
        return id;
    }

    public static void runClientNearestStationResponseAction(long id, NearestTrackStationResult result) {
        if (CLIENT_NEAREST_STATION_RESPONSE_ACTION.containsKey(id)) {
            Consumer<NearestTrackStationResult> action = CLIENT_NEAREST_STATION_RESPONSE_ACTION.remove(id);
            if (action != null)
                action.accept(result);
        }
    }
}
