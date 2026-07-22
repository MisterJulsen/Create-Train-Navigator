package de.mrjulsen.crn.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import de.mrjulsen.crn.client.journey.JourneyTracker;
import de.mrjulsen.crn.navigator.route.RouteJourney;
import de.mrjulsen.crn.network.packets.pain.GetUserSettingsPacketData;
import de.mrjulsen.crn.registry.ModNetworkManager;
import de.mrjulsen.mcdragonlib.network.NetworkDirection;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.Holder.MutableHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

/**
 * The routes the player has bookmarked, held client-side and synchronised with their user settings.
 * <p>
 * A saved route is followed for as long as it is saved, whether or not anybody is looking at it: the
 * point of bookmarking a journey is to be told when it stops working, and that cannot wait for the
 * player to open a window. Each one therefore gets a {@link JourneyTracker} of its own, which keeps
 * its times and its delay reasons current in the background.
 * <p>
 * The tracked instance is the one handed out by {@link #getAllSavedRoutes()}, so a window showing a
 * saved route shows the very same journey object the background tracker is writing into.
 */
public final class SavedRoutesManager {

    private SavedRoutesManager() {}

    private static final LinkedHashSet<RouteJourney> savedRoutes = new LinkedHashSet<>();
    private static final Map<RouteJourney, JourneyTracker> trackers = new HashMap<>();
    private static final MutableHolder<Boolean> isSynchronizing = new MutableHolder<>(false);

    public static void saveRoute(RouteJourney route) {
        savedRoutes.add(route);
        track(route);
    }

    public static void removeRoute(RouteJourney route) {
        savedRoutes.remove(route);
        JourneyTracker tracker = trackers.remove(route);
        if (tracker != null) {
            tracker.close();
        }
    }

    public static void removeAllRoutes() {
        savedRoutes.clear();
        trackers.values().forEach(JourneyTracker::close);
        trackers.clear();
    }

    /** The tracker following the given route in the background, if it is saved. */
    public static Optional<JourneyTracker> trackerOf(RouteJourney route) {
        return Optional.ofNullable(trackers.get(route));
    }

    private static void track(RouteJourney route) {
        trackers.computeIfAbsent(route, x -> {
            JourneyTracker tracker = new JourneyTracker(x);
            tracker.start();
            return tracker;
        });
    }

    public static boolean isSaved(RouteJourney route) {
        return savedRoutes.contains(route);
    }

    public static List<RouteJourney> getAllSavedRoutes() {
        return new ArrayList<>(savedRoutes);
    }

    public static void push(boolean clear, Runnable andThen) {
        isSynchronizing.set(true);
        ModNetworkManager.GET_USER_SETTINGS.send(NetworkDirection.toServer(), new GetUserSettingsPacketData.Request(Minecraft.getInstance().player.getUUID()), (response) -> {
            response.getData().ifPresent(settings -> {
                Set<CompoundTag> currentValue = clear ? new HashSet<>() : settings.savedRoutes.getValue();
                currentValue.addAll(savedRoutes.stream().map(RouteJourney::toNbt).toList());
                settings.savedRoutes.setValue(currentValue);
                settings.clientSave(() -> {
                    isSynchronizing.set(false);
                    DLUtils.doIfNotNull(andThen, Runnable::run);
                });
            });

        }, () -> {});
    }

    public static void pull(boolean clear, Runnable andThen) {
        isSynchronizing.set(true);
        ModNetworkManager.GET_USER_SETTINGS.send(NetworkDirection.toServer(), new GetUserSettingsPacketData.Request(Minecraft.getInstance().player.getUUID()), (response) -> {
            response.getData().ifPresent(settings -> {
                Set<RouteJourney> currentValue = settings.savedRoutes.getValue().stream().map(RouteJourney::fromNbt).collect(Collectors.toSet());
                if (clear) {
                    removeAllRoutes();
                }
                currentValue.forEach(SavedRoutesManager::saveRoute);
                isSynchronizing.set(false);
                DLUtils.doIfNotNull(andThen, Runnable::run);
            });

        }, () -> {});
    }

    public static boolean isSynchronizing() {
        return isSynchronizing.get();
    }
}
