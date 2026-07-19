package de.mrjulsen.crn.backend.api;

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
 * The registry of all {@link ISpeedLimitProvider}s. See that interface for the problem this solves
 * and how to implement one.
 * <p>
 * Providers are identified so their contributions can be attributed back to them on a display and
 * so one can be replaced or removed again at runtime.
 *
 * <h2>Threading</h2>
 * Registration is expected during mod initialization. Queries run on the server thread.
 */
public final class SpeedLimitProviderRegistry {

    private static final Map<ResourceLocation, ISpeedLimitProvider> PROVIDERS = new LinkedHashMap<>();

    private SpeedLimitProviderRegistry() {}

    /**
     * Registers a provider under the given id, replacing any provider already registered under it.
     * Safe to call from any mod's initialization, in any order.
     *
     * @return The registered provider, for convenient field assignment.
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

    /** Removes the provider registered under the given id. */
    public static synchronized boolean unregister(ResourceLocation id) {
        return PROVIDERS.remove(id) != null;
    }

    /** Removes all providers registered under the given namespace. */
    public static synchronized void unregisterNamespace(String modid) {
        PROVIDERS.keySet().removeIf(id -> id.getNamespace().equals(modid));
    }

    /** The provider registered under the given id. */
    public static synchronized Optional<ISpeedLimitProvider> get(ResourceLocation id) {
        return Optional.ofNullable(PROVIDERS.get(id));
    }

    /** The ids of all registered providers, in registration order. */
    public static synchronized List<ResourceLocation> getProviderIds() {
        return List.copyOf(PROVIDERS.keySet());
    }

    /** Whether any provider is registered at all. */
    public static synchronized boolean isEmpty() {
        return PROVIDERS.isEmpty();
    }

    /**
     * Collects the speed limits every applicable provider reports for the given query, each segment
     * attributed to the provider that reported it.
     * <p>
     * A provider that throws is logged and skipped, so a faulty add-on cannot break the backend's
     * predictions for the whole network.
     *
     * @return All reported segments, unsorted and possibly overlapping. Empty if nothing applies.
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
