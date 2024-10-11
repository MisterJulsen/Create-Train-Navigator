package de.mrjulsen.crn.util;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Predicate;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;

import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.exceptions.RuntimeSideException;
import de.mrjulsen.crn.web.WebsitePreparableReloadListener;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.TimeUtils;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;

public class ModUtils {

    private static WebsitePreparableReloadListener websitemanager;
    
    public static float clockHandDegrees(long time, int divisor) {
        return 360.0F / divisor * (time % divisor);
    }

    public static double calcSpeed(double metersPerTick, ESpeedUnit unit) {
        return metersPerTick * DragonLib.TPS * unit.getFactor();
    }

    public static MutableComponent calcSpeedString(double metersPerTick, ESpeedUnit unit) {
        return TextUtils.text((int) Math.abs(Math.round(calcSpeed(metersPerTick, unit))) + " " + unit.getUnit());
    }    

    public static int calculateMedian(Queue<Integer> history, int smoothingThreshold, Predicate<Integer> filter) {
        if (history.isEmpty()) {
            return 0;
        }

        List<Integer> values = new LinkedList<>();
        for (int i : history) {
            if (!filter.test(i)) 
                continue;

            values.add(i);
        }

        Collections.sort(values);
        int median = 0;
        if (values.size() % 2 == 0) {
            median = (int)(((double)values.get(values.size() / 2) + (double)values.get(values.size() / 2 + 1)) / 2D);
        }
        median = values.get(values.size() / 2);

        final int med = median;
        return (int)history.stream().mapToInt(x -> x).filter(x -> Math.abs(med - x) <= smoothingThreshold).average().orElse(0);
    }

    public static String timeRemainingString(long ticks) {
        StringBuilder sb = new StringBuilder();
        final String unpredictable = " ~ ";
        final String whitespace = " ";

        if (ticks == -1 || ticks >= 120000 - 15 * 20) {
            sb.append(whitespace);
            sb.append(unpredictable);

        } else if (ticks < 200) {
            sb.append(Lang.translateDirect("display_source.station_summary.now").getString());

        } else {
            long min = ticks / 1200;
            long sec = (ticks / 20) % 60;
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

    public static long generateId(Predicate<Long> exists) {
        long id;
        do {
            id = DragonLib.RANDOM.nextLong();
        } while (exists.test(id));
        return id;
    }

    public static void setWebsiteResourceManager(WebsitePreparableReloadListener manager) {
        websitemanager = manager;
    }

    public static WebsitePreparableReloadListener getWebsiteResourceManager() {
        return websitemanager;
    }

    /** Client-side only! */
    public static String formatTime(long time, boolean asETA) throws RuntimeSideException {
        if (Platform.getEnvironment() != Env.CLIENT) {
            throw new RuntimeSideException(true);
        }
        if (asETA) {
            return timeRemainingString(time - DragonLib.getCurrentWorldTime());
        }
        return TimeUtils.parseTime((time + DragonLib.DAYTIME_SHIFT) % DragonLib.TICKS_PER_DAY, ModClientConfig.TIME_FORMAT.get());
    }
}
