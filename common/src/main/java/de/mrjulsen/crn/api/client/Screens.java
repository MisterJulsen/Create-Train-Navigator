package de.mrjulsen.crn.api.client;

import de.mrjulsen.crn.client.gui.windows.NavigatorWindow;
import de.mrjulsen.mcdragonlib.client.gui.widgets.base.DLWindow;

public final class Screens {    
    private Screens() {}

    /**
     * Opens the navigator UI to search for routes.
     * @param stationName The station name that should be entered as the fixed departure station. Pass {@code null} to let the user select any departure station.
     * @param isPublic In public mode, no personal settings and data can be changed or viewed (e.g. saved routes).
     */
    public static void showNavigatorScreen(String stationName, boolean isPublic) {
        //DLWindow.openWindow((mgr) -> new NavigatorScreen(mgr, stationName, isPublic));
        DLWindow.openWindow((mgr) -> new NavigatorWindow(mgr));
    }
}
