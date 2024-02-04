package de.mrjulsen.crn.util;

import de.mrjulsen.mcdragonlib.DragonLibConstants;

public class ModTimeUtils {
    public static String parseTimeWithoutCorrection(int time) {
        if (time < 0) {
            return "--:--";
        }

        time = time % DragonLibConstants.TICKS_PER_DAY;
        int hours = time / 1000;
        int minutes = time % 1000;
        minutes = (int)(minutes / (1000.0D / 60.0D));
        return String.format("%02d:%02d", hours, minutes);
    }
}
