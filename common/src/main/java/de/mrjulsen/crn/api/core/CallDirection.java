/*
 * Create Railways Navigator 1.21.1
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.api.core;

/** Which side of a station call is meant, the train coming in or going out. */
public enum CallDirection {

    /** The train arriving at the station. */
    ARRIVAL,

    /** The train leaving the station. */
    DEPARTURE;

    /** Whether this is the arrival side. */
    public boolean isArrival() {
        return this == ARRIVAL;
    }

    /** Whether this is the departure side. */
    public boolean isDeparture() {
        return this == DEPARTURE;
    }
}
