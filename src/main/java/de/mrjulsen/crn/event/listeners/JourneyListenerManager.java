package de.mrjulsen.crn.event.listeners;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import de.mrjulsen.crn.data.SimpleRoute;
import de.mrjulsen.crn.event.listeners.JourneyListener.State;

public class JourneyListenerManager {

    private static final int CLEANUP_INTERVALL = 100;
    private static int cleanupTimer = 0;

    private static final Map<UUID, JourneyListener> journeyListenerCache = new HashMap<>();
    private static final Map<UUID, Set<UUID>> dataListeners = new HashMap<>();

    public static UUID register(JourneyListener listener) {
        UUID uuid = UUID.randomUUID();
        while (journeyListenerCache.containsKey(uuid)) {
            uuid = UUID.randomUUID();
        }

        journeyListenerCache.put(uuid, listener);
        return uuid;
    }

    public static UUID create(SimpleRoute route, IJourneyListenerClient initialListener) {
        UUID id = register(new JourneyListener(route));
        dataListeners.put(id, new HashSet<>(Set.of(initialListener.getJourneyListenerClientId())));
        return id;
    }

    public static JourneyListener get(UUID id, IJourneyListenerClient addListener) {
        if (addListener != null) {
            if (dataListeners.containsKey(id)) {
                dataListeners.get(id).add(addListener.getJourneyListenerClientId());
            } else {
                dataListeners.put(id, new HashSet<>(Set.of(addListener.getJourneyListenerClientId())));
            }
        }
        return journeyListenerCache.get(id);
    }

    /*
    public static void remove(UUID id) {
        journeyListenerCache.remove(id);
        dataListeners.remove(id);
    }
    */

    public static void removeClientListener(UUID listenerId, IJourneyListenerClient client) {
        if (dataListeners.containsKey(listenerId)) {
            dataListeners.get(listenerId).removeIf(x -> x.equals(client.getJourneyListenerClientId()));
        }
    }

    public static void removeClientListenerForAll(IJourneyListenerClient client) {
        dataListeners.values().forEach(x -> x.removeIf(a -> a.equals(client.getJourneyListenerClientId())));
    }


    public static void tick() {
        journeyListenerCache.values().forEach(x -> x.tick());

        cleanupTimer++;
        cleanupTimer = cleanupTimer % CLEANUP_INTERVALL;
        if (cleanupTimer == 0) {   
            dataListeners.entrySet().removeIf(x -> x.getValue().isEmpty());
            journeyListenerCache.entrySet().removeIf(e -> !dataListeners.containsKey(e.getKey()) || e.getValue().getCurrentState() == State.AFTER_JOURNEY);
        }
    }

    public static int getCacheSize() {
        return journeyListenerCache.size();
    }

    public static boolean exists(UUID listenerId) {
        return journeyListenerCache.containsKey(listenerId);
    }

}
