/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.core.train;

/** What a train is doing at this moment. */
public enum LiveTrainState {

    /** The train has no schedule to run. */
    NO_SCHEDULE,

    /** The train's schedule is paused. */
    SCHEDULE_PAUSED,

    /** The train has run its schedule to the end. */
    SCHEDULE_COMPLETED,

    /** The train has derailed. */
    DERAILED,

    /** The train is standing at a station. */
    AT_STATION,

    /** The train is running between stations. */
    EN_ROUTE,

    /** The train is held at a signal. */
    WAITING_FOR_SIGNAL,

    /** The train is unable to move. */
    STALLED;

    /** Whether the train is on its way between stations, whether moving, held or stalled. */
    public boolean isEnRoute() {
        return this == EN_ROUTE || this == WAITING_FOR_SIGNAL || this == STALLED;
    }

    /** Whether the train is working its schedule, either running or standing at a station. */
    public boolean isOperating() {
        return isEnRoute() || this == AT_STATION;
    }
}
