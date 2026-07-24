package de.mrjulsen.crn.api.core;

/** What kind of restriction a {@link SpeedLimitSegment} describes. */
public enum SpeedLimitKind {

    /** A lasting property of the track, expected to apply on every run. */
    PERMANENT,

    /** A restriction in force for now but not part of the normal state of the track. */
    TEMPORARY,

    /** A restriction imposed by signalling. */
    SIGNAL,

    /** A restriction that applies because of an approach to or passage through a station. */
    STATION,

    /** Anything not covered by the other kinds. */
    OTHER
}
