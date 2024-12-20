package de.mrjulsen.crn.network;

import de.mrjulsen.mcdragonlib.client.OverlayManager;

public class InstanceManager {
    private static long currentRouteOverlayId;

    public static void setRouteOverlay(long id) {
        removeRouteOverlay();
        currentRouteOverlayId = id;
    }

    public static void removeRouteOverlay() {
        if (OverlayManager.has(currentRouteOverlayId)) {
            OverlayManager.remove(currentRouteOverlayId);
        }
    }
}
