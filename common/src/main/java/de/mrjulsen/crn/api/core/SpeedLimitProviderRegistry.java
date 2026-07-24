package de.mrjulsen.crn.api.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import net.minecraft.resources.ResourceLocation;

/**
 * Where {@link ISpeedLimitProvider} implementations are registered. Register during mod setup;
 * providers may be added or removed at any later time as well. All methods are safe to call from
 * any thread.
 */
public final class SpeedLimitProviderRegistry {

    private static final Map<ResourceLocation, ISpeedLimitProvider> PROVIDERS = new LinkedHashMap<>();

    private SpeedLimitProviderRegistry() {}

    /**
     * Registers a provider under an id, which should live in your own mod's namespace. Registering
     * an id twice replaces the earlier provider and logs a warning.
     *
     * @return The provider passed in, so the call can be used in a field initialiser.
     */
    public static synchronized <T extends ISpeedLimitProvider> T register(ResourceLocation id, T provider) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(provider, "provider");
        if (PROVIDERS.containsKey(id)) {
            CreateRailwaysNavigator.LOGGER.warn("[Backend] Speed limit provider '{}' is already registered and will be overwritten.", id);
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

    public static synchronized Optional<ISpeedLimitProvider> get(ResourceLocation id) {
        return Optional.ofNullable(PROVIDERS.get(id));
    }

    /** The ids of all registered providers, in registration order. */
    public static synchronized List<ResourceLocation> getProviderIds() {
        return List.copyOf(PROVIDERS.keySet());
    }

    public static synchronized boolean isEmpty() {
        return PROVIDERS.isEmpty();
    }

    /**
     * Asks every registered provider for its restrictions and merges the answers, attributing each
     * segment to the provider that supplied it. Segments beyond the query's horizon are dropped,
     * and a provider that throws is logged and skipped rather than failing the whole query.
     * <p>
     * Called by the backend; addons normally do not need this.
     */
    public static List<SpeedLimitSegment> collect(SpeedLimitQuery query) {
        List<Map.Entry<ResourceLocation, ISpeedLimitProvider>> providers;
        synchronized (SpeedLimitProviderRegistry.class) {
            if (PROVIDERS.isEmpty()) {
                return List.of();
            }
            providers = List.copyOf(PROVIDERS.entrySet());
        }

        List<SpeedLimitSegment> collected = new ArrayList<>();
        for (Map.Entry<ResourceLocation, ISpeedLimitProvider> entry : providers) {
            ResourceLocation id = entry.getKey();
            try {
                ISpeedLimitProvider provider = entry.getValue();
                if (!provider.appliesTo(query)) {
                    continue;
                }
                Collection<SpeedLimitSegment> segments = provider.getSpeedLimits(query);
                if (segments == null) {
                    continue;
                }
                for (SpeedLimitSegment segment : segments) {
                    if (segment != null && segment.startDistance() <= query.horizon()) {
                        collected.add(segment.attributedTo(id));
                    }
                }
            } catch (Exception e) {
                CreateRailwaysNavigator.LOGGER.error("[Backend] Speed limit provider '{}' failed and was skipped.", id, e);
            }
        }
        return collected;
    }
}
