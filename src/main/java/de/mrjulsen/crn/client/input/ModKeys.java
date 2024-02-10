package de.mrjulsen.crn.client.input;

import org.lwjgl.glfw.GLFW;

import de.mrjulsen.crn.ModMain;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.ClientRegistry;

public class ModKeys {
    
    private ModKeys() {}

    public static KeyMapping keyRouteOverlayOptions;

    private static KeyMapping registerKey(String name, String category, int keycode) {
        KeyMapping key = new KeyMapping("key." + ModMain.MOD_ID + "." + name, keycode, "key." + ModMain.MOD_ID + ".category." + category);
        ClientRegistry.registerKeyBinding(key);
        return key;
    }

    public static void init() {
        keyRouteOverlayOptions = registerKey("route_overlay_options", "crn", GLFW.GLFW_KEY_N);
    }
}
