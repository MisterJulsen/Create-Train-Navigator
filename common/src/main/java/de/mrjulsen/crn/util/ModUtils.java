package de.mrjulsen.crn.util;

import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;

public class ModUtils {
    public static float clockHandDegrees(long time, int divisor) {
        return 360.0F / divisor * (time % divisor);
    }

    public static double calcSpeed(double metersPerTick, ESpeedUnit unit) {
        return metersPerTick * DragonLib.TPS * unit.getFactor();
    }

    public static MutableComponent calcSpeedString(double metersPerTick, ESpeedUnit unit) {
        return TextUtils.text((int) Math.abs(Math.round(calcSpeed(metersPerTick, unit))) + " " + unit.getUnit());
    }

    public static String timeRemainingString(int ticks) {
        StringBuilder sb = new StringBuilder();
        final String unpredictable = " ~ ";
        final String whitespace = " ";

        if (ticks == -1 || ticks >= 12000 - 15 * 20) {
            sb.append(whitespace);
            sb.append(unpredictable);

        } else if (ticks < 200) {
            sb.append(Lang.translateDirect("display_source.station_summary.now").getString());

        } else {
            int min = ticks / 1200;
            int sec = (ticks / 20) % 60;
            sec = Mth.ceil(sec / 15f) * 15;
            if (sec == 60) {
                min++;
                sec = 0;
            }
            sb.append(min > 0 ? Components.literal(String.valueOf(min)).getString() : "");
            sb.append(min > 0 ? Lang.translateDirect("display_source.station_summary.minutes").getString() : Lang.translateDirect("display_source.station_summary.seconds", sec).getString());
        }

        return sb.toString();
    }
}
