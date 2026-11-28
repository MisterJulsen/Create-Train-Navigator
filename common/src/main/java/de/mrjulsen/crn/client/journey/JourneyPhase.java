/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.client.journey;

public enum JourneyPhase {

    BEFORE_DEPARTURE,

    RIDING,

    TRANSFERRING,

    COMPLETED,

    CONNECTION_MISSED,

    TRAIN_CANCELLED;

    public boolean isTravelling() {
        return this == BEFORE_DEPARTURE || this == RIDING || this == TRANSFERRING;
    }

    public boolean isDisrupted() {
        return this == CONNECTION_MISSED || this == TRAIN_CANCELLED;
    }
}
