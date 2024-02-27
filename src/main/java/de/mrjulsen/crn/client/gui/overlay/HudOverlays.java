package de.mrjulsen.crn.client.gui.overlay;

import java.util.HashMap;
import java.util.Map;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent.CharacterTyped;
import net.minecraftforge.client.event.ScreenEvent.KeyPressed;
import net.minecraftforge.client.event.ScreenEvent.KeyReleased;
import net.minecraftforge.client.event.ScreenEvent.MouseButtonPressed;
import net.minecraftforge.client.event.ScreenEvent.MouseDragged;
import net.minecraftforge.client.event.ScreenEvent.MouseScrolled;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(Dist.CLIENT)
public class HudOverlays {

    private static final Map<Integer, IHudOverlay> activeOverlays = new HashMap<>();

    public static final IGuiOverlay HUD_ROUTE_DETAILS = ((gui, graphics, partialTicks, width, height) -> {
        activeOverlays.values().forEach(x -> x.render(gui, graphics, width, height, partialTicks));
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
    public static void keyPressed(KeyPressed event) {
        activeOverlays.values().forEach(x -> x.keyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers()));
    }

    @SubscribeEvent
    public static void keyReleased(KeyReleased event) {
        activeOverlays.values().forEach(x -> x.keyReleased(event.getKeyCode(), event.getScanCode(), event.getModifiers()));
    }

    @SubscribeEvent
    public static void charTyped(CharacterTyped event) {
        activeOverlays.values().forEach(x -> x.charTyped(event.getCodePoint(), event.getModifiers()));
    }

    @SubscribeEvent
    public static void mouseDragged(MouseDragged event) {
        activeOverlays.values().forEach(x -> x.mouseDragged(event.getMouseX(), event.getMouseY(), event.getMouseButton(), event.getDragX(), event.getDragY()));
    }

    @SubscribeEvent
    public static void mouseClicked(MouseButtonPressed event) {
        activeOverlays.values().forEach(x -> x.mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton()));
    }

    @SubscribeEvent
    public static void mouseScrolled(MouseScrolled event) {
        activeOverlays.values().forEach(x -> x.mouseScrolled(event.getMouseX(), event.getMouseY(), event.getScrollDelta()));
    }
}
