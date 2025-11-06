package de.mrjulsen.crn.util;

import java.util.Map;
import java.util.function.Supplier;

import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.util.time.ConfiguredTimeSystem;
import de.mrjulsen.mcdragonlib.util.time.DLTime;
import de.mrjulsen.mcdragonlib.util.time.TimeContext;

public class VariableManager {

    private static final Map<String, Supplier<String>> variables = Map.ofEntries(
        Map.entry("time", () -> DLTime.fromTicks(DragonLib.getCurrentWorldTime(), new ConfiguredTimeSystem()).format(ModClientConfig.TIME_FORMAT.get().getFormat(), TimeContext.INGAME))
    );

    public static String replacePlaceholders(String text) {
        
        StringBuilder result = new StringBuilder();
        int length = text.length();

        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);

            // escaped placeholder \%
            if (c == '\\' && i + 1 < length && text.charAt(i + 1) == '%') {
                result.append('%');
                i++;
                continue;
            }

            if (c == '%') {
                int end = text.indexOf('%', i + 1);
                if (end > i + 1) {
                    String key = text.substring(i + 1, end);
                    Supplier<String> supplier = variables.get(key);
                    if (supplier != null) {
                        String replacement = supplier.get();
                        result.append(replacement);
                    } else {
                        // Key nicht gefunden: lasse Platzhalter unverändert
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
