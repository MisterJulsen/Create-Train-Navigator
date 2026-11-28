/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.core.train;

public record LiveUpdateState(
    boolean available,
    ServiceState service,
    int currentEntry,
    boolean atStation,
    int remainingTransitTicks,
    long separationHoldTicks
) {

    public static final LiveUpdateState UNAVAILABLE = new LiveUpdateState(false, ServiceState.IN_SERVICE, -1, false, 0, 0);

    public static LiveUpdateState outOfService(ServiceState service) {
        return new LiveUpdateState(true, service, -1, false, 0, 0);
    }
}
