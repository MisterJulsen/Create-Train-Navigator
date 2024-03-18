package de.mrjulsen.crn.util;

import net.minecraft.world.phys.Vec3;

public class MathUtils {
    public static double getVectorAngle(Vec3 vec) {
        return Math.round(Math.atan2(vec.x(), -vec.z()) * (180.0 / Math.PI));
    }
}
