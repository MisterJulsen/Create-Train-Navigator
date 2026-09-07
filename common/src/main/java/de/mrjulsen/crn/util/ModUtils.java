package de.mrjulsen.crn.util;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.simibubi.create.foundation.utility.CreateLang;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.exceptions.RuntimeSideException;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.config.ECachingPriority;
import de.mrjulsen.mcdragonlib.util.Cache;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.time.DLTime;
import de.mrjulsen.mcdragonlib.util.time.TimeContext;
import de.mrjulsen.mcdragonlib.util.time.VanillaTimeSystem;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.createmod.catnip.data.Glob;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;

public class ModUtils {

    private static final Cache<DLColor[]> dyeColorsCache = new Cache<>(() -> Arrays.stream(DyeColor.values()).map(x -> DLColor.fromInt(x == DyeColor.ORANGE ? 0xFFFF9900 : (0xFF << 24) | (x.getTextColor() & 0x00FFFFFF))).toArray(DLColor[]::new), ECachingPriority.LOW);

    public static float clockHandDegrees(double value, double unitsPerRevolution) {
        double normalized = value % unitsPerRevolution;
        return (float) (normalized / unitsPerRevolution * 360.0);
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

    public static Pattern buildPattern(String src) {
        return Pattern.compile(Glob.toRegexPattern(src, "^.*$"));
    }

    public static boolean isGlobPattern(String text) {
        if (text == null) {
            return false;
        }

        final int len = text.length();

        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);

            if (c == '\\') {
                i++;
            } else {
                if (c == '*' || c == '?' || c == '[' || c == '{') {
                    return true;
                }
            }
        }

        return false;
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




    private static final String REGEX_META_CHARS = ".^$+{[]|()";
    private static final String GLOB_META_CHARS = "\\*?[{";

    private static boolean isRegexMeta(char c) {
        return REGEX_META_CHARS.indexOf(c) != -1;
    }
    private static boolean isGlobMeta(char c) {
        return GLOB_META_CHARS.indexOf(c) != -1;
    }
    private static char next(String glob, int i) {
        return i < glob.length() ? glob.charAt(i) : 0;
    }

    public static Map<String, List<String>> mapWildcards2(String src, List<String> targets, Collection<String> pool) {
        Pattern p;
        try {
            String regex = createCapturingRegex(src);
            p = Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            e.printStackTrace();
            return new LinkedHashMap<>();
        }

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

                if (S >= 1) {
                    for (int i = 0; i < S; i++) {
                        String fill;
                        if (i < g) {
                            fill = groups.get(i);
                        } else {
                            fill = "";
                        }
                        sb.append(fill).append(part[i + 1]);
                    }
                }
                out.add(sb.toString());
            }
            res.put(text, out);
        }
        return res;
    }

    private static String createCapturingRegex(String globPattern) {
        boolean inGroup = false;
        StringBuilder regex = new StringBuilder("^");
        int i = 0;
        boolean isNegativeLookaround = false;
        boolean isAnchored = true;

        while (i < globPattern.length()) {
            char c = globPattern.charAt(i++);

            switch (c) {
                case '*' -> {
                    regex.append("(.*)");
                    if (!inGroup) isAnchored = false;
                }
                case '?' -> {
                    regex.append("(.)");
                    if (!inGroup) isAnchored = true;
                }
                case ',' -> {
                    if (inGroup) {
                        regex.append("|");
                    } else {
                        regex.append(',');
                        isAnchored = true;
                    }
                }
                case '[' -> {
                    if (next(globPattern, i) == ']' || (next(globPattern, i) == '!' && next(globPattern, i + 1) == ']')) {
                        throw new PatternSyntaxException("Cannot have set with no entries", globPattern, i);
                    }

                    regex.append("([");

                    if (next(globPattern, i) == '^') {
                        regex.append("\\^"); ++i;
                    } else if (next(globPattern, i) == '!') {
                        regex.append('^'); ++i;
                    }
                    if (next(globPattern, i) == '-') {
                        regex.append('-'); ++i;
                    }

                    boolean hasRangeStart = false;
                    char last = 0;
                    while (i < globPattern.length()) {
                        c = globPattern.charAt(i++);
                        if (c == ']') break;
                        if (c == '\\') {
                            if (i == globPattern.length()) throw new PatternSyntaxException("No character to escape", globPattern, i - 1);
                            if (next(globPattern, i) == ']' || next(globPattern, i) == '-' || next(globPattern, i) == '\\') regex.append('\\');
                            regex.append(next(globPattern, i++));
                            continue;
                        }
                        regex.append(c);
                        if (c == '-') {
                            if (!hasRangeStart) throw new PatternSyntaxException("Invalid range", globPattern, i - 1);
                            if ((c = next(globPattern, i++)) == 0 || c == ']') break;
                            if (c < last) throw new PatternSyntaxException("Invalid range", globPattern, i - 3);
                            regex.append(c);
                            hasRangeStart = false;
                        } else {
                            hasRangeStart = true;
                            last = c;
                        }
                    }
                    if (c != ']') throw new PatternSyntaxException("Missing ']'", globPattern, i - 1);

                    regex.append("])");
                    if (!inGroup) isAnchored = true;
                }
                case '\\' -> {
                    if (i == globPattern.length()) throw new PatternSyntaxException("No character to escape", globPattern, i - 1);
                    char next = globPattern.charAt(i++);
                    if (isGlobMeta(next) || isRegexMeta(next)) regex.append('\\');
                    regex.append(next);
                    if (!inGroup) isAnchored = true;
                }
                case '{' -> {
                    if (inGroup) throw new PatternSyntaxException("Cannot nest groups", globPattern, i - 1);
                    if (next(globPattern, i) == '!') {
                        regex.append("(?!");
                        isNegativeLookaround = true;
                        if (!isAnchored) regex.append('<');
                        ++i;
                    } else {
                        regex.append("(");
                        isNegativeLookaround = false;
                    }
                    inGroup = true;
                }
                case '}' -> {
                    if (inGroup) {
                        regex.append(")");
                        if (isAnchored && isNegativeLookaround) {
                            regex.append(".*");
                            isAnchored = false;
                        }
                        inGroup = false;
                    } else {
                        regex.append('}');
                        isAnchored = true;
                    }
                }
                default -> {
                    if (isRegexMeta(c)) regex.append('\\');
                    regex.append(c);
                    if (!inGroup) isAnchored = true;
                }
            }
        }
        if (inGroup) throw new PatternSyntaxException("Missing '}'", globPattern, i - 1);
        return regex.append('$').toString();
    }




    public static String formatTime(long time, boolean asETA) throws RuntimeSideException {
        if (Platform.getEnvironment() != Env.CLIENT) {
            throw new RuntimeSideException(true);
        }
        if (asETA) {
            return timeRemainingString(time - ModUtils.getTransformedWorldTime());
        }
        return DLTime.fromGameTicks(time, VanillaTimeSystem.INSTANCE).format(ModClientConfig.TIME_FORMAT.get().getFormat(), TimeContext.INGAME, DLTime.defaultTimeSystem());
    }

    public static long getTransformedWorldTime() {
        return transformWorldTime(DragonLib.getCurrentWorldTime());
    }

    public static String formatDuration(long durationTicks) {
        if (ModClientConfig.REALTIME_DURATIONS.get()) {
            return new DLTime(durationTicks, VanillaTimeSystem.INSTANCE).format(Constants.DEFAULT_VERBOSE_REAL_DURATION_FORMAT, TimeContext.REAL, VanillaTimeSystem.INSTANCE);
        }
        return new DLTime(durationTicks, VanillaTimeSystem.INSTANCE).format(Constants.DEFAULT_VERBOSE_GAME_DURATION_FORMAT, TimeContext.INGAME, DLTime.defaultTimeSystem());
    }

    public static long transformWorldTime(long rawWorldTime) {
        return Math.round(DLTime.fromGameTicks(rawWorldTime, DLTime.defaultTimeSystem()).toTicks(VanillaTimeSystem.INSTANCE));
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


    public static <T, S> boolean listContainsAny(Collection<T> searchFor, Collection<S> searchIn, BiPredicate<T, S> test) {
        return listContainsAny(searchFor, searchIn, true, false, test);
    }

    public static <T, S> boolean listContainsAny(Collection<T> searchFor, Collection<S> searchIn, boolean ifSearchEmpty, boolean ifTargetEmpty, BiPredicate<T, S> test) {
        if (searchFor.isEmpty()) {
            return ifSearchEmpty;
        }
        if (searchIn.isEmpty()) {
            return ifTargetEmpty;
        }

        for (S s : searchIn) {
            for (T t : searchFor) {
                if (test.test(t, s)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static <T, S> boolean listContainsElement(T searchFor, Collection<S> searchIn, BiPredicate<T, S> test) {
        return listContainsElement(searchFor, searchIn, false, test);
    }

    public static <T, S> boolean listContainsElement(T searchFor, Collection<S> searchIn, boolean ifTargetEmpty, BiPredicate<T, S> test) {
        if (searchIn.isEmpty()) {
            return ifTargetEmpty;
        }

        for (S s : searchIn) {
            if (test.test(searchFor, s)) {
                return true;
            }
        }
        return false;
    }
}
