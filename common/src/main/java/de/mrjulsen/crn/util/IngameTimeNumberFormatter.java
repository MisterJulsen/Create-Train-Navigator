package de.mrjulsen.crn.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.mrjulsen.mcdragonlib.client.gui.widgets.util.INumberFormatAdapter;

public class IngameTimeNumberFormatter implements INumberFormatAdapter {

    public static final IngameTimeNumberFormatter INSTANCE = new IngameTimeNumberFormatter();

    @Override
    public String format(double value) {
        return ModUtils.formatDuration((long) value);
    }

    @Override
    public double parse(String input) throws NumberFormatException {
        final double ticksPerDay = 24000;
        final double ticksPerHour = ticksPerDay / 24;
        final double ticksPerMinute = ticksPerHour / 60;
        Pattern pattern = Pattern.compile("(\\d+)d|(\\d+)h|(\\d+)m");
        Matcher matcher = pattern.matcher(input.replaceAll("\\s+", ""));
        double totalTicks = 0;

        while (matcher.find()) {
            if (matcher.group(1) != null) {
                int days = Integer.parseInt(matcher.group(1));
                totalTicks += days * ticksPerDay;
            } else if (matcher.group(2) != null) {
                int hours = Integer.parseInt(matcher.group(2));
                totalTicks += hours * ticksPerHour;
            } else if (matcher.group(3) != null) {
                int minutes = Integer.parseInt(matcher.group(3));
                totalTicks += minutes * ticksPerMinute;
            }
        }

        return totalTicks;
    }
}
