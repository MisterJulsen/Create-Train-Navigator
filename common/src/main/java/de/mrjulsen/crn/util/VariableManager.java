package de.mrjulsen.crn.util;

import java.util.Map;
import java.util.function.Supplier;

import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.util.TimeUtils;

public class VariableManager {

    private static final Map<String, Supplier<String>> variables = Map.ofEntries(
        Map.entry("time", () -> TimeUtils.parseTime(DragonLib.getCurrentWorldTime() + DragonLib.daytimeShift(), ModClientConfig.TIME_FORMAT.get()))
    );

    public static String replacePlaceholders(String text) {
        StringBuilder result = new StringBuilder();
        int length = text.length();

        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);

            if (c == '\\' && i + 1 < length && text.charAt(i + 1) == '%') {
                result.append('%');
                i++;
                continue;
            }

            if (c == '%') {
                int end = text.indexOf('%', i + 1);
                if (end > i + 1) {
                    String key = text.substring(i + 1, end);
                    String replacement = variables.get(key).get();
                    if (replacement != null) {
                        result.append(replacement);
                    } else {
                        result.append('%').append(key).append('%');
                    }
                    i = end;
                    continue;
                }
            }
            result.append(c);
        }

        return result.toString();
    }

    public static boolean hasValidPlaceholders(String text) {
        int length = text.length();
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);

            if (c == '\\' && i + 1 < length && text.charAt(i + 1) == '%') {
                i++;
                continue;
            }

            if (c == '%') {
                int end = text.indexOf('%', i + 1);
                if (end == -1) {
                    return false;
                }
                if (end == i + 1) {
                    return false;
                }
                String key = text.substring(i + 1, end);
                if (!variables.containsKey(key)) {
                    return false;
                }
                i = end;
            }
        }
        return true;
    }
}
