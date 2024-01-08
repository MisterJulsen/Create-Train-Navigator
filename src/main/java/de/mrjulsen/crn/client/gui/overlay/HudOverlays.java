package de.mrjulsen.crn.client.gui.overlay;

import java.util.HashMap;
import java.util.Map;

import net.minecraftforge.client.gui.IIngameOverlay;

public class HudOverlays {

    private static final Map<Integer, HudOverlay> activeOverlays = new HashMap<>();

    public static final IIngameOverlay HUD_ROUTE_DETAILS = ((gui, poseStack, partialTicks, width, height) -> {
        activeOverlays.values().forEach(x -> x.render(gui, poseStack, width, height, partialTicks));
    });

    public static <T extends HudOverlay> T setOverlay(T overlay) {
        activeOverlays.remove(overlay.getId());
        activeOverlays.put(overlay.getId(), overlay);
        return overlay;
    }

    public static boolean hasOverlay(HudOverlay overlay) {
        return activeOverlays.containsKey(overlay.getId());
    }

    public static HudOverlay getOverlay(int id) {
        return activeOverlays.get(id);
    }

    public static void tick() {
        activeOverlays.values().forEach(x -> x.tick());
    }
}
