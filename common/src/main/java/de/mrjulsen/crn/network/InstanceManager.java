package de.mrjulsen.crn.network;

public class InstanceManager {
    private static long currentRouteOverlayId;

    public static void setRouteOverlay(long id) {
        removeRouteOverlay();
        currentRouteOverlayId = id;
    }

    public static void removeRouteOverlay() {
        //if (OverlayManager.has(currentRouteOverlayId)) {
        //    OverlayManager.remove(currentRouteOverlayId);
        //}
    }
}
