package de.mrjulsen.crn.core.delay;

/** What kind of value a {@link DelayArgument} carries, so it can be formatted for display. */
public enum DelayArgumentType {

    /** Plain text. */
    TEXT,

    /** A train's name. */
    TRAIN_NAME,

    /** A station's name. */
    STATION_NAME,

    /** A line's name. */
    LINE_NAME,

    /** A duration, in ticks. */
    DURATION_TICKS,

    /** A plain number. */
    NUMBER
}
