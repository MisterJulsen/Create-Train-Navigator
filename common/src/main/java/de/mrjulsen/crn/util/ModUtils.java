package de.mrjulsen.crn.util;

public class ModUtils {
    public static float clockHandDegrees(long time, int divisor) {
        return 360.0F / divisor * (time % divisor);
    }
}
