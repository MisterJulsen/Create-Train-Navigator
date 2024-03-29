package de.mrjulsen.crn.client.gui.overlay;

import java.util.HashMap;
import java.util.Map;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent.KeyboardCharTypedEvent;
import net.minecraftforge.client.event.ScreenEvent.KeyboardKeyPressedEvent;
import net.minecraftforge.client.event.ScreenEvent.KeyboardKeyReleasedEvent;
import net.minecraftforge.client.event.ScreenEvent.MouseClickedEvent;
import net.minecraftforge.client.event.ScreenEvent.MouseDragEvent;
import net.minecraftforge.client.event.ScreenEvent.MouseScrollEvent;
import net.minecraftforge.client.gui.IIngameOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(Dist.CLIENT)
public class HudOverlays {

    private static final Map<Integer, IHudOverlay> activeOverlays = new HashMap<>();

    public static final IIngameOverlay HUD_ROUTE_DETAILS = ((gui, poseStack, partialTicks, width, height) -> {
        activeOverlays.values().forEach(x -> x.render(gui, poseStack, width, height, partialTicks));
    });

    public static <T extends IHudOverlay> T setOverlay(T overlay) {
        if (activeOverlays.containsKey(overlay.getId())) {
            activeOverlays.get(overlay.getId()).onClose();
        }
        activeOverlays.remove(overlay.getId());
        activeOverlays.put(overlay.getId(), overlay);
        return overlay;
    }

    public static boolean hasOverlay(IHudOverlay overlay) {
        return activeOverlays.containsKey(overlay.getId());
    }

    public static int overlayCount() {
        return activeOverlays.size();
    }

    public static IHudOverlay getOverlay(int id) {
        return activeOverlays.get(id);
    }

    public static void tick() {
        activeOverlays.values().forEach(x -> x.tick());
    }

    public static void remove(int id) {
        if (activeOverlays.containsKey(id)) {
            activeOverlays.get(id).onClose();
            activeOverlays.remove(id);
        }
    }

    public static int removeAll() {
        int count = activeOverlays.size();
        activeOverlays.values().forEach(x -> x.onClose());
        activeOverlays.clear();
        return count;
    }


    @SubscribeEvent
    public static void keyPressed(KeyboardKeyPressedEvent event) {
        activeOverlays.values().forEach(x -> x.keyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers()));
    }

    @SubscribeEvent
    public static void keyReleased(KeyboardKeyReleasedEvent event) {
        activeOverlays.values().forEach(x -> x.keyReleased(event.getKeyCode(), event.getScanCode(), event.getModifiers()));
    }

    @SubscribeEvent
    public static void charTyped(KeyboardCharTypedEvent event) {
        activeOverlays.values().forEach(x -> x.charTyped(event.getCodePoint(), event.getModifiers()));
    }

    @SubscribeEvent
    public static void mouseDragged(MouseDragEvent event) {
        activeOverlays.values().forEach(x -> x.mouseDragged(event.getMouseX(), event.getMouseY(), event.getMouseButton(), event.getDragX(), event.getDragY()));
    }

    @SubscribeEvent
    public static void mouseClicked(MouseClickedEvent event) {
        activeOverlays.values().forEach(x -> x.mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton()));
    }

    @SubscribeEvent
    public static void mouseScrolled(MouseScrollEvent event) {
        activeOverlays.values().forEach(x -> x.mouseScrolled(event.getMouseX(), event.getMouseY(), event.getScrollDelta()));
    }
}
