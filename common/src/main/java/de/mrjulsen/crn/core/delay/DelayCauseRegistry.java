package de.mrjulsen.crn.core.delay;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import net.minecraft.resources.ResourceLocation;

/**
 * Where {@link DelayCause} implementations are registered, so the backend consults them when it looks
 * for the reasons a train is delayed. Register your causes during mod setup.
 */
public final class DelayCauseRegistry {

    private static final Map<ResourceLocation, DelayCause> CAUSES = new LinkedHashMap<>();

    private DelayCauseRegistry() {}

    /**
     * Registers a cause under an id in your own mod's namespace. Throws if the id is already taken.
     *
     * @return The cause passed in, so the call can be used in a field initialiser.
     */
    public static <T extends DelayCause> T register(String modid, String name, T cause) {
        ResourceLocation id = new ResourceLocation(modid, name);
        if (CAUSES.containsKey(id)) {
            throw new IllegalStateException("Duplicate delay cause id: " + id);
        }
        cause.assignId(id);
        CAUSES.put(id, cause);
        return cause;
    }

    /** The cause registered under the given id, if any. */
    public static Optional<DelayCause> get(ResourceLocation id) {
        return Optional.ofNullable(CAUSES.get(id));
    }

    /** Whether a cause is registered under the given id. */
    public static boolean isRegistered(ResourceLocation id) {
        return CAUSES.containsKey(id);
    }

    /** Every registered cause, in registration order. */
    public static Collection<DelayCause> all() {
        return Collections.unmodifiableCollection(CAUSES.values());
    }

    /** Removes every cause registered under the given namespace. */
    public static void unregisterNamespace(String modid) {
        CAUSES.keySet().removeIf(id -> id.getNamespace().equals(modid));
    }
}
