package de.mrjulsen.crn.event.listeners;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.data.SimpleRoute;
import de.mrjulsen.crn.event.listeners.JourneyListener.State;
import net.minecraft.client.Minecraft;

public class JourneyListenerManager {

    private static final int CLEANUP_INTERVALL = 100;
    private static JourneyListenerManager instance;

    private int cleanupTimer = 0;

    private final Map<UUID, JourneyListener> journeyListenerCache = new HashMap<>();
    private final Map<UUID, Set<UUID>> dataListeners = new HashMap<>();

    public UUID register(JourneyListener listener) {
        UUID uuid = UUID.randomUUID();
        while (journeyListenerCache.containsKey(uuid)) {
            uuid = UUID.randomUUID();
        }

        journeyListenerCache.put(uuid, listener);
        return uuid;
    }

    public UUID create(SimpleRoute route, IJourneyListenerClient initialListener) {
        UUID id = register(new JourneyListener(route));
        dataListeners.put(id, new HashSet<>(Set.of(initialListener.getJourneyListenerClientId())));
        return id;
    }

    public JourneyListener get(UUID id, IJourneyListenerClient addListener) {
        if (addListener != null) {
            if (dataListeners.containsKey(id)) {
                dataListeners.get(id).add(addListener.getJourneyListenerClientId());
            } else {
                dataListeners.put(id, new HashSet<>(Set.of(addListener.getJourneyListenerClientId())));
            }
        }
        return journeyListenerCache.get(id);
    }

    public void removeClientListener(UUID listenerId, IJourneyListenerClient client) {
        if (dataListeners.containsKey(listenerId)) {
            dataListeners.get(listenerId).removeIf(x -> x.equals(client.getJourneyListenerClientId()));
        }
    }

    public void removeClientListenerForAll(IJourneyListenerClient client) {
        dataListeners.values().forEach(x -> x.removeIf(a -> a.equals(client.getJourneyListenerClientId())));
    }


    public static boolean hasInstance() {
        return instance != null;
    }

    @SuppressWarnings("resource")
    public static void tick() {
        if (!hasInstance() || Minecraft.getInstance().level == null) {
            return;
        }

        instance.tickInstance();        
    }

    private void tickInstance() {
        journeyListenerCache.values().forEach(x -> x.tick());

        cleanupTimer++;
        cleanupTimer = cleanupTimer % CLEANUP_INTERVALL;
        if (cleanupTimer == 0) {   
            dataListeners.entrySet().removeIf(x -> x.getValue().isEmpty());
            journeyListenerCache.entrySet().removeIf(e -> !dataListeners.containsKey(e.getKey()) || e.getValue().getCurrentState() == State.AFTER_JOURNEY);
        }
    }

    public int getCacheSize() {
        return journeyListenerCache.size();
    }

    public boolean exists(UUID listenerId) {
        return journeyListenerCache.containsKey(listenerId);
    }

    public Collection<JourneyListener> getAllListeners() {
        return journeyListenerCache.values();
    }


    public static JourneyListenerManager getInstance() {
        return instance;
    }

    public static JourneyListenerManager start() {
        stop();
        instance = new JourneyListenerManager();
        CreateRailwaysNavigator.LOGGER.info("Journey Listener started.");
        return instance;
    }

    public static void stop() {
        if (hasInstance()) {
            instance.stopInstance();
        }

        instance = null;
    }

    private void stopInstance() {
        dataListeners.clear();
        journeyListenerCache.clear();
        CreateRailwaysNavigator.LOGGER.info("Journey Listener stopped.");
    }

}
