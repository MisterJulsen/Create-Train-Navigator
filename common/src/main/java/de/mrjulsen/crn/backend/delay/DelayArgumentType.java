package de.mrjulsen.crn.backend.delay;

/**
 * The kind of value a {@link DelayArgument} carries, so a consumer can format it appropriately
 * instead of printing every argument as raw text.
 * <p>
 * New types may be added over time. A consumer that does not know a type should fall back to
 * {@link DelayArgument#value()}, which is always a printable representation.
 */
public enum DelayArgumentType {

    /** Free text that needs no special treatment. */
    TEXT,

    /** The name of a train, e.g. the one blocking the way. */
    TRAIN_NAME,

    /** The name of a station. */
    STATION_NAME,

    /** The name of a train line. */
    LINE_NAME,

    /** A duration in game ticks, to be formatted as a time span. */
    DURATION_TICKS,

    /** A plain number. */
    NUMBER
}
