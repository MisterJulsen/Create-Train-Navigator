/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.core.train;

/** How far the backend has come in learning a train, and hence how far its data can be trusted. */
public enum TrainLifecycleState {

    /** The train has just been picked up; nothing is known yet. */
    PREPARING,

    /** The backend is learning the train's timetable; its data is not yet dependable. */
    LEARNING,

    /** The train has been learned and its data can be trusted. */
    READY,

    /** The train has been cancelled. */
    CANCELLED,

    /** The train is standing idle with no schedule to learn from. */
    IDLE;

    /** Whether the train has been learned well enough for its data to be dependable. */
    public boolean isUsable() {
        return this == READY;
    }
}
