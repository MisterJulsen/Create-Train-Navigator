package de.mrjulsen.crn.backend.delay;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import net.minecraft.resources.ResourceLocation;

/**
 * The registry of all {@link DelayCause}s known to the backend.
 * <p>
 * Iteration order is registration order and doubles as priority, so earlier-registered, more
 * specific reasons are considered first. Registration happens once during initialization on both
 * the server and the client, so a cause can be resolved back to its translation on either side.
 */
public final class DelayCauseRegistry {

    private static final Map<ResourceLocation, DelayCause> CAUSES = new LinkedHashMap<>();

    private DelayCauseRegistry() {}

    /**
     * Registers a cause under {@code <modid>:<name>}, replacing any cause already registered under
     * that id. Returns the same instance, for convenient {@code static final} field assignment.
     */
    public static <T extends DelayCause> T register(String modid, String name, T cause) {
        ResourceLocation id = new ResourceLocation(modid, name);
        if (CAUSES.containsKey(id)) {
            CreateRailwaysNavigator.LOGGER.warn("[Backend] Delay cause '{}' is already registered and will be overwritten.", id);
        }
        cause.assignId(id);
        CAUSES.put(id, cause);
        return cause;
    }

    /** The registered cause with the given id. */
    public static Optional<DelayCause> get(ResourceLocation id) {
        return Optional.ofNullable(CAUSES.get(id));
    }

    /** Whether a cause is registered under the given id. */
    public static boolean isRegistered(ResourceLocation id) {
        return CAUSES.containsKey(id);
    }

    /** All registered causes in registration (priority) order. */
    public static Collection<DelayCause> all() {
        return Collections.unmodifiableCollection(CAUSES.values());
    }

    /** Removes all causes registered under the given namespace. */
    public static void unregisterNamespace(String modid) {
        CAUSES.keySet().removeIf(id -> id.getNamespace().equals(modid));
    }
}
