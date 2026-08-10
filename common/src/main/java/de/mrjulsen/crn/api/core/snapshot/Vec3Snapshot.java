package de.mrjulsen.crn.api.core.snapshot;

import net.minecraft.world.phys.Vec3;

public record Vec3Snapshot(
        double x,
        double y,
        double z
) {

    public static Vec3Snapshot of(Vec3 vec) {
        return vec == null ? null : new Vec3Snapshot(vec.x, vec.y, vec.z);
    }
}
