package de.mrjulsen.crn.core.delay;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.mcdragonlib.util.DLColor;

/** How much weight a delay reason carries, and how it should be shown. */
public enum DelaySeverity {

    /** A note worth showing that does not itself mean lost time. */
    INFO,

    /** A reason that accounts for lost time. */
    DELAY,

    /** A reason worth showing whether or not it costs time, such as a disruption. */
    IMPORTANT;

    /** Whether this severity marks lost time. */
    public boolean isDelay() {
        return this == DELAY;
    }

    /** The colour to show a reason of this severity in. */
    public DLColor color() {
        return this == INFO ? DLColor.WHITE : Constants.COLOR_DELAYED;
    }
}
