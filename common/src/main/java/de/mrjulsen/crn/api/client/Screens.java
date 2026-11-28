/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.api.client;

import de.mrjulsen.crn.client.gui.windows.NavigatorWindow;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindow;

/** Opens this mod's screens. <b>Client side only.</b> */
public final class Screens {
    private Screens() {}

    /**
     * Opens the navigator.
     *
     * @param stationName Not currently used; the navigator opens without a preset station.
     * @param isPublic    Not currently used.
     */
    public static void showNavigatorScreen(String stationName, boolean isPublic) {
        DLWindow.openWindow((mgr) -> new NavigatorWindow(mgr));
    }
}
