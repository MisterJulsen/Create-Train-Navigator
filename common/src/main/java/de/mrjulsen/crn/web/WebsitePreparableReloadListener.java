package de.mrjulsen.crn.web;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public class WebsitePreparableReloadListener extends SimplePreparableReloadListener<Map<String, Supplier<byte[]>>> {

    public static final String RESOURCE_PATH = "crn_website";

    private Map<String, Supplier<byte[]>> data;

    @Override
    protected void apply(Map<String, Supplier<byte[]>> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        data = object;
    }

    @Override
    protected Map<String, Supplier<byte[]>> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {

        Map<String, Supplier<byte[]>> map = new HashMap<>();
        Collection<ResourceLocation> locations = resourceManager.listResources(RESOURCE_PATH, x -> true).keySet();

        for (ResourceLocation loc : locations) {
            final ResourceLocation localLoc = loc;
            String path = localLoc.getPath().replace(RESOURCE_PATH, "");
            map.put(path, () -> {
                try {
                    Resource resource = resourceManager.getResourceOrThrow(localLoc);
                    try (InputStream inputstream = resource.open()) {
                        return inputstream.readAllBytes();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            });            
        }

        return map;
    }

    public Optional<byte[]> getFileBytesFor(String path) {
        return Optional.ofNullable(data.containsKey(path) ? data.get(path).get() : null);
    }
    
}
