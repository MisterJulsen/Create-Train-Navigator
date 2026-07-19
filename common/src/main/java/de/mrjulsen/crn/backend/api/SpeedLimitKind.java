package de.mrjulsen.crn.backend.api;

/**
 * Why a speed limit applies. Lets a consumer treat a limit that is part of the line's normal layout
 * differently from one imposed by a temporary situation, e.g. when explaining an estimate to a
 * player or deciding whether a slower run counts as a disruption.
 * <p>
 * New kinds may be added over time. A consumer that does not know a kind should treat it like
 * {@link #OTHER}.
 */
public enum SpeedLimitKind {

    /** A permanent property of the line, e.g. a speed sign or the geometry of the track. */
    PERMANENT,

    /** A limit in force only for now, e.g. roadworks or a temporary restriction. */
    TEMPORARY,

    /** Imposed by signalling, e.g. an approach with a restrictive aspect. */
    SIGNAL,

    /** Imposed by an upcoming stop, e.g. the approach to a station. */
    STATION,

    /** Anything else. */
    OTHER
}
