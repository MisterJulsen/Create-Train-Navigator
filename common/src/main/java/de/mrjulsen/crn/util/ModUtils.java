package de.mrjulsen.crn.util;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.simibubi.create.foundation.utility.CreateLang;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.exceptions.RuntimeSideException;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.config.ECachingPriority;
import de.mrjulsen.mcdragonlib.util.Cache;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.time.ConfiguredTimeSystem;
import de.mrjulsen.mcdragonlib.util.time.DLTime;
import de.mrjulsen.mcdragonlib.util.time.TimeContext;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;

public class ModUtils {

    private static final Cache<DLColor[]> dyeColorsCache = new Cache<>(() -> Arrays.stream(DyeColor.values()).map(x -> DLColor.fromInt(x == DyeColor.ORANGE ? 0xFFFF9900 : (0xFF << 24) | (x.getTextColor() & 0x00FFFFFF))).toArray(DLColor[]::new), ECachingPriority.LOW);
    
    public static float clockHandDegrees(double time, double fac) {
        double relTime = time - (long)time;
        return (float)(360D * (relTime * fac));
    }

    public static double calcSpeed(double metersPerTick, ESpeedUnit unit) {
        return metersPerTick * 20 * unit.getFactor(); // TODO: DragonLib minecraftTps() for the game tick rate
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

        if (values.isEmpty()) {
            return 0;
        }

        Collections.sort(values);
        int median = 0;
        if (values.size() % 2 == 0) {
            median = (int)(((double)values.get(values.size() / 2) + (double)values.get(values.size() / 2 + 1)) / 2D);
        } else if (values.size() == 1) {
            median = (int)(((double)values.get(0) * 2) / 2D);
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
            sb.append(CreateLang.translateDirect("display_source.station_summary.now").getString());

        } else {
            long min = ticks / 1200;
            long sec = (ticks / 20) % 60;
            sec = Mth.ceil(sec / 15f) * 15;
            if (sec == 60) {
                min++;
                sec = 0;
            }
            sb.append(min > 0 ? TextUtils.text(String.valueOf(min)).getString() : "");
            sb.append(min > 0 ? CreateLang.translateDirect("display_source.station_summary.minutes").getString() : CreateLang.translateDirect("display_source.station_summary.seconds", sec).getString());
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
    
    public static DLColor[] getDyeColors() {
        return dyeColorsCache.get();
    }

    public static long convertToTimeTicks(int hours, int minutes) {
        return (long)((double)hours * 1000D + (1000D / 60D * (double)minutes));
    }

    private static Pattern buildPattern(String src) {
        String escaped = "\\Q" + src.replace("*", "\\E(.*)\\Q") + "\\E";
        return Pattern.compile(escaped);
    }

    public static boolean hasWildcards(String text) {
        return text.contains("*");
    }

    public static Collection<String> wildcardMatches(String src, Collection<String> pool) {
        try {
            Pattern p = buildPattern(src);
            List<String> res = new LinkedList<>();
            for (String text : pool) {
                Matcher m = p.matcher(text);
                if (!m.matches()) continue;
                res.add(text);
            }
            return res;
        } catch (Exception e) {
            CreateRailwaysNavigator.LOGGER.warn("Error while checking regex: " + e);
            return List.of();
        }
    }

    public static Map<String, List<String>> mapWildcards(String src, List<String> targets, Collection<String> pool) {
        Pattern p = buildPattern(src);
        Map<String, List<String>> res = new LinkedHashMap<>();
        for (String text : pool) {
            Matcher m = p.matcher(text);
            if (!m.matches()) continue;
            int g = m.groupCount();
            List<String> groups = new ArrayList<>(g);
            for (int i = 1; i <= g; i++) groups.add(m.group(i));

            List<String> out = new ArrayList<>();
            for (String target : targets) {
                String[] part = target.split("\\*", -1);
                int S = part.length - 1;
                StringBuilder sb = new StringBuilder(part[0]);
                if (S == 1) {
                    sb.append(String.join("", groups)).append(part[1]);
                } else if (S > 1) {
                    for (int i = 0; i < S; i++) {
                        String fill;
                        if (i < S - 1) {
                            fill = i < g ? groups.get(i) : "";
                        } else {
                            int start = Math.min(i, g);
                            fill = String.join("", groups.subList(start, g));
                        }
                        sb.append(fill).append(part[i + 1]);
                    }
                }
                out.add(sb.toString().replace("*", ""));
            }
            res.put(text, out);
        }
        return res;
    }

    public static String formatTime(long time, boolean asETA) throws RuntimeSideException {
        if (Platform.getEnvironment() != Env.CLIENT) {
            throw new RuntimeSideException(true);
        }
        if (asETA) {
            return timeRemainingString(time - DragonLib.getCurrentWorldTime());
        }
        return DLTime.fromTicks(time, new ConfiguredTimeSystem()).format(ModClientConfig.TIME_FORMAT.get().getFormat(), TimeContext.INGAME);
    }



    public static <K, V> void putMap(CompoundTag nbt, String key, Map<K, V> map, Function<K, String> keySerializer, Function<V, CompoundTag> valueSerializer) {
        CompoundTag mapNbt = new CompoundTag();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            mapNbt.put(keySerializer.apply(entry.getKey()), valueSerializer.apply(entry.getValue()));
        }
        nbt.put(key, mapNbt);
    }

    public static <K, V> Map<K, V> getMap(CompoundTag nbt, String key, Function<String, K> keyDeserializer, Function<CompoundTag, V> valueDeserializer) {
        CompoundTag mapNbt = nbt.getCompound(key);
        Map<K, V> map = new HashMap<>(mapNbt.size());
        for (String k : mapNbt.getAllKeys()) {
            map.put(keyDeserializer.apply(k), valueDeserializer.apply(mapNbt.getCompound(k)));
        }
        return map;
    }
}
