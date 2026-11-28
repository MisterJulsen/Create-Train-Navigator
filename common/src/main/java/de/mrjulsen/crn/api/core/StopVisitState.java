/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.api.core;

/** Where a stop stands in relation to the train's progress through its run. */
public enum StopVisitState {

    /** The train has already departed from this stop on the current run. */
    PASSED,

    /** The train is standing at this stop now. */
    CURRENT,

    /** The train has yet to reach this stop. */
    UPCOMING;

    /** Whether the train has still to depart from this stop, which covers both of the latter two. */
    public boolean isPending() {
        return this != PASSED;
    }
}
