package de.mrjulsen.crn.util;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.network.chat.MutableComponent;

public class ModUtils {
    public static float clockHandDegrees(long time, int divisor) {
        return 360.0F / divisor * (time % divisor);
    }

    public static double calcSpeed(double metersPerTick, ESpeedUnit unit) {
        return metersPerTick * DragonLib.TPS * unit.getFactor();
    }

    public static MutableComponent calcSpeedString(double metersPerTick, ESpeedUnit unit) {
        return TextUtils.text((int)Math.abs(Math.round(calcSpeed(metersPerTick, unit))) + " " + unit.getUnit());
    }
}
