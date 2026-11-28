/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.api.core.speed;

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
