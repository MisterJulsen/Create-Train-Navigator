package de.mrjulsen.crn.util;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.config.ModCommonConfig;
import net.minecraft.world.level.Level;

public class ExtraTimeUtils {
    public static long getDayTimeScaled(Level level) {
        return (long)Math.ceil(level.getDayTime() * ModCommonConfig.TIME_MULTIPLIER.get());
    }
    
    public static long getCurrentWorldTimeScaled() {
        return (long)Math.ceil(DragonLib.getCurrentWorldTime() * ModCommonConfig.TIME_MULTIPLIER.get());
    }
}
