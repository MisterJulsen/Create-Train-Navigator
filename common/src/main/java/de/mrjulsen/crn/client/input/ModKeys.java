package de.mrjulsen.crn.client.input;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import de.mrjulsen.crn.ExampleMod;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;

public class ModKeys {
    
    private ModKeys() {
    }
    
    public static final KeyMapping KEY_OVERLAY_SETTINGS = new KeyMapping(
        "key." + ExampleMod.MOD_ID + ".route_overlay_options",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_R,
        "category." + ExampleMod.MOD_ID + ".category.crn"
    );

    public static void init() {
        KeyMappingRegistry.register(KEY_OVERLAY_SETTINGS);
    }
}
