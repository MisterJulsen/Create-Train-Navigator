package de.mrjulsen.crn.client.input;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import de.mrjulsen.crn.ModMain;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;

public class ModKeys {
    
    private ModKeys() {}

    public static KeyMapping keyRouteOverlayOptions;

    private static KeyMapping registerKey(String name, String category, IKeyConflictContext context, KeyModifier modifier, int keycode) {
        KeyMapping k = new KeyMapping("key." + ModMain.MOD_ID + "." + name, context, modifier, InputConstants.Type.KEYSYM, keycode, "key." + ModMain.MOD_ID + ".category." + category);
        ClientRegistry.registerKeyBinding(k);
        return k;
    }

    public static void init() {
        keyRouteOverlayOptions = registerKey("route_overlay_options", "crn", KeyConflictContext.IN_GAME, KeyModifier.CONTROL, GLFW.GLFW_KEY_R);
    }
}
