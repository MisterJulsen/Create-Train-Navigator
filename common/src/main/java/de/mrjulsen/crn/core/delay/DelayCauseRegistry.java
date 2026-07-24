package de.mrjulsen.crn.core.delay;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import net.minecraft.resources.ResourceLocation;

public final class DelayCauseRegistry {

    private static final Map<ResourceLocation, DelayCause> CAUSES = new LinkedHashMap<>();

    private DelayCauseRegistry() {}

    public static <T extends DelayCause> T register(String modid, String name, T cause) {
        ResourceLocation id = new ResourceLocation(modid, name);
        if (CAUSES.containsKey(id)) {
            throw new IllegalStateException("Duplicate delay cause id: " + id);
        }
        cause.assignId(id);
        CAUSES.put(id, cause);
        return cause;
    }

    public static Optional<DelayCause> get(ResourceLocation id) {
        return Optional.ofNullable(CAUSES.get(id));
    }

    public static boolean isRegistered(ResourceLocation id) {
        return CAUSES.containsKey(id);
    }

    public static Collection<DelayCause> all() {
        return Collections.unmodifiableCollection(CAUSES.values());
    }

    public static void unregisterNamespace(String modid) {
        CAUSES.keySet().removeIf(id -> id.getNamespace().equals(modid));
    }
}
