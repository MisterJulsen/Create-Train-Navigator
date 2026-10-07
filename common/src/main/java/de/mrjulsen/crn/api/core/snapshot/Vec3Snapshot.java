package de.mrjulsen.crn.api.core.snapshot;

import net.minecraft.world.phys.Vec3;

/**
 * A serialisable view of a point in the world.
 *
 * @param x The x coordinate.
 * @param y The y coordinate.
 * @param z The z coordinate.
 */
public record Vec3Snapshot(
        double x,
        double y,
        double z
) {

    public static Vec3Snapshot of(Vec3 vec) {
        return vec == null ? null : new Vec3Snapshot(vec.x, vec.y, vec.z);
    }
}
