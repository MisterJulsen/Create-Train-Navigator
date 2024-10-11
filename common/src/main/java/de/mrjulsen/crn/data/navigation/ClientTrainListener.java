package de.mrjulsen.crn.data.navigation;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import de.mrjulsen.crn.registry.ModAccessorTypes;
import de.mrjulsen.mcdragonlib.data.Pair;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.accessor.DataAccessor;

public final class ClientTrainListener {
    
    private static final ConcurrentHashMap<UUID /* Train id */, ConcurrentHashMap<UUID /* callback id */, Pair<UUID /* session id */, Consumer<ClientRoutePart.TrainRealTimeData>>>> callbacks = new ConcurrentHashMap<>();

    public static int debug_registeredListenersCount() {
        return callbacks.values().stream().mapToInt(x -> x.size()).sum();
    }

    public static UUID register(UUID sessionId, UUID trainId, Consumer<ClientRoutePart.TrainRealTimeData> callback) {
        Map<UUID, Pair<UUID, Consumer<ClientRoutePart.TrainRealTimeData>>> trainCallbacks = callbacks.computeIfAbsent(trainId, x -> new ConcurrentHashMap<>());

        UUID id = null;
        do {
            id = UUID.randomUUID();
        } while (trainCallbacks.containsKey(id));

        trainCallbacks.put(id, Pair.of(sessionId, callback));
        return id;
    }

    public static void unregister(UUID trainId, UUID callbackId) {
        if (callbacks.containsKey(trainId)) {
            Map<UUID, Pair<UUID, Consumer<ClientRoutePart.TrainRealTimeData>>> trainCallbacks = callbacks.get(trainId);
            if (trainCallbacks.containsKey(callbackId)) {
                trainCallbacks.remove(callbackId);
            }
            
            if (trainCallbacks.isEmpty()) {
                callbacks.remove(trainId);
            }
        }
    }

    public static void tick(Runnable andThen) {
        callbacks.entrySet().stream().forEach(x -> {
            if (x.getValue().isEmpty()) {
                callbacks.remove(x.getKey());
                return;
            }

            final Map<UUID, Pair<UUID, Consumer<ClientRoutePart.TrainRealTimeData>>> listeners = x.getValue();
            DataAccessor.getFromServer(x.getKey(), ModAccessorTypes.UPDATE_REALTIME, res -> {
                if (res != null) {
                    new ArrayList<>(listeners.values()).stream().forEach(a -> a.getSecond().accept(res));
                }
                DLUtils.doIfNotNull(andThen, a -> a.run());
            });
        });
    }

    public static void clear() {
        callbacks.clear();
    }
}
