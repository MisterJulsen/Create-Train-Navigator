package de.mrjulsen.crn.api.core.snapshot;

import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import net.minecraft.resources.ResourceLocation;

public record TrackNodeLocationSnapshot(
        int x,
        int y,
        int z,
        int yOffsetPixels,
        ResourceLocation dimension
) {

    public static TrackNodeLocationSnapshot of(TrackNodeLocation location) {
        return new TrackNodeLocationSnapshot(location.getX(), location.getY(), location.getZ(), location.yOffsetPixels, location.dimension.location());
    }
}