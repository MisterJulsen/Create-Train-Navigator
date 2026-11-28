/*
 * Create Railways Navigator 1.21.1
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.network;

public class InstanceManager {
    private static long currentRouteOverlayId;

    public static void setRouteOverlay(long id) {
        removeRouteOverlay();
        currentRouteOverlayId = id;
    }

    public static void removeRouteOverlay() {
    }
}
