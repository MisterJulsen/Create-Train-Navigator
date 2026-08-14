package de.mrjulsen.crn.api.core.snapshot;

import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import net.minecraft.resources.ResourceLocation;

/**
 * A serialisable view of a track node's location in the world.
 *
 * @param x             The node's x coordinate.
 * @param y             The node's y coordinate.
 * @param z             The node's z coordinate.
 * @param yOffsetPixels The node's vertical offset within the block, in pixels.
 * @param dimension     The dimension the node is in.
 */
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