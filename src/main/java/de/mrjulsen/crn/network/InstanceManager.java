package de.mrjulsen.crn.network;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import de.mrjulsen.crn.data.NearestTrackStationResult;
import de.mrjulsen.crn.data.SimpleRoute;
import de.mrjulsen.crn.data.SimpleTrainConnection;
import de.mrjulsen.crn.data.DeparturePrediction.SimpleDeparturePrediction;
import de.mrjulsen.crn.network.packets.cts.TrainDataRequestPacket.TrainData;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class InstanceManager {

    private static final Map<Long, Runnable> CLIENT_RESPONSE_RECEIVED_ACTION = new HashMap<>();
    private static final Map<Long, Consumer<List<SimpleRoute>>> CLIENT_NAVIGATION_RESPONSE_ACTION = new HashMap<>();
    private static final Map<Long, Consumer<NearestTrackStationResult>> CLIENT_NEAREST_STATION_RESPONSE_ACTION = new HashMap<>();
    private static final Map<Long, BiConsumer<Collection<SimpleDeparturePrediction>, Long>> CLIENT_REALTIME_RESPONSE_ACTION = new HashMap<>();
    private static final Map<Long, BiConsumer<Collection<SimpleTrainConnection>, Long>> CLIENT_NEXT_CONNECTIONS_RESPONSE_ACTION = new HashMap<>();
    private static final Map<Long, BiConsumer<TrainData, Long>> CLIENT_NEXT_TRAIN_DATA_RESPONSE_ACTION = new HashMap<>();
    
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


    public static long registerClientRealtimeResponseAction(BiConsumer<Collection<SimpleDeparturePrediction>, Long> consumer) {
        long id = System.nanoTime();
        CLIENT_REALTIME_RESPONSE_ACTION.put(id, consumer);
        return id;
    }

    public static void runClientRealtimeResponseAction(long id, Collection<SimpleDeparturePrediction> result, long time) {
        if (CLIENT_REALTIME_RESPONSE_ACTION.containsKey(id)) {
            BiConsumer<Collection<SimpleDeparturePrediction>, Long> action = CLIENT_REALTIME_RESPONSE_ACTION.remove(id);
            if (action != null)
                action.accept(result, time);
        }
    }


    public static long registerClientNextConnectionsResponseAction(BiConsumer<Collection<SimpleTrainConnection>, Long> consumer) {
        long id = System.nanoTime();
        CLIENT_NEXT_CONNECTIONS_RESPONSE_ACTION.put(id, consumer);
        return id;
    }

    public static void runClientNextConnectionsResponseAction(long id, Collection<SimpleTrainConnection> result, long time) {
        if (CLIENT_NEXT_CONNECTIONS_RESPONSE_ACTION.containsKey(id)) {
            BiConsumer<Collection<SimpleTrainConnection>, Long> action = CLIENT_NEXT_CONNECTIONS_RESPONSE_ACTION.remove(id);
            if (action != null)
                action.accept(result, time);
        }
    }



    public static long registerClientTrainDataResponseAction(BiConsumer<TrainData, Long> consumer) {
        long id = System.nanoTime();
        CLIENT_NEXT_TRAIN_DATA_RESPONSE_ACTION.put(id, consumer);
        return id;
    }

    public static void runClientTrainDataResponseAction(long id, TrainData data, long time) {
        if (CLIENT_NEXT_TRAIN_DATA_RESPONSE_ACTION.containsKey(id)) {
            BiConsumer<TrainData, Long> action = CLIENT_NEXT_TRAIN_DATA_RESPONSE_ACTION.remove(id);
            if (action != null)
                action.accept(data, time);
        }
    }
}
