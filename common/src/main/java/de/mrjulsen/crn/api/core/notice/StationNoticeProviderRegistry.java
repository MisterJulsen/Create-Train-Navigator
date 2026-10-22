package de.mrjulsen.crn.api.core.notice;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import net.minecraft.resources.ResourceLocation;

/**
 * Where {@link IStationNoticeProvider} implementations are registered. Register during mod setup; providers may be
 * added or removed at any later time as well. All methods are safe to call from any thread.
 */
public final class StationNoticeProviderRegistry {

    private static final Map<ResourceLocation, IStationNoticeProvider> PROVIDERS = new LinkedHashMap<>();

    private StationNoticeProviderRegistry() {}

    /**
     * Registers a provider under an id, which should live in your own mod's namespace. Registering an id twice
     * replaces the earlier provider and logs a warning.
     *
     * @return The provider passed in, so the call can be used in a field initialiser.
     */
    public static synchronized <T extends IStationNoticeProvider> T register(ResourceLocation id, T provider) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(provider, "provider");
        if (PROVIDERS.containsKey(id)) {
            CreateRailwaysNavigator.LOGGER.warn("[Backend] Station notice provider '{}' is already registered and will be overwritten.", id);
        }
        PROVIDERS.put(id, provider);
        return provider;
    }

    /** Removes a provider, reporting whether one was registered under that id. */
    public static synchronized boolean unregister(ResourceLocation id) {
        return PROVIDERS.remove(id) != null;
    }

    /** Removes every provider registered under the given namespace. */
    public static synchronized void unregisterNamespace(String modid) {
        PROVIDERS.keySet().removeIf(id -> id.getNamespace().equals(modid));
    }

    /** The provider registered under the given id, if any. */
    public static synchronized Optional<IStationNoticeProvider> get(ResourceLocation id) {
        return Optional.ofNullable(PROVIDERS.get(id));
    }

    /** The ids of all registered providers, in registration order. */
    public static synchronized List<ResourceLocation> getProviderIds() {
        return List.copyOf(PROVIDERS.keySet());
    }

    /**
     * Asks every registered provider for its notices about the station and joins the answers in registration order.
     * A provider that throws is logged and skipped rather than failing the whole refresh.
     * <p>
     * Called by the displays; addons normally do not need this.
     */
    public static List<StationNotice> collect(String stationName) {
        List<Map.Entry<ResourceLocation, IStationNoticeProvider>> providers;
        synchronized (StationNoticeProviderRegistry.class) {
            if (PROVIDERS.isEmpty()) {
                return List.of();
            }
            // Detached copies: the map's own entries change when a provider is replaced while the providers run.
            providers = PROVIDERS.entrySet().stream().map(entry -> Map.entry(entry.getKey(), entry.getValue())).toList();
        }

        List<StationNotice> collected = new ArrayList<>();
        for (Map.Entry<ResourceLocation, IStationNoticeProvider> entry : providers) {
            try {
                List<StationNotice> notices = entry.getValue().getNotices(stationName);
                if (notices != null) {
                    notices.stream().filter(Objects::nonNull).forEach(collected::add);
                }
            } catch (Exception e) {
                CreateRailwaysNavigator.LOGGER.error("[Backend] Station notice provider '{}' failed and was skipped.", entry.getKey(), e);
            }
        }
        return List.copyOf(collected);
    }
}
