package de.mrjulsen.crn.core.delay;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.mcdragonlib.util.DLColor;

public enum DelaySeverity {
    INFO,
    DELAY,
    IMPORTANT;

    public boolean isDelay() {
        return this == DELAY;
    }

    public DLColor color() {
        return this == INFO ? DLColor.WHITE : Constants.COLOR_DELAYED;
    }
}
