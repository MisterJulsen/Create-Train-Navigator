package de.mrjulsen.crn.client.input;

import com.mojang.blaze3d.platform.InputConstants;

import de.mrjulsen.crn.ExampleMod;
import net.minecraft.client.KeyMapping;

public class ModKeys {
    
    private ModKeys() {
    }
    
    public static final KeyMapping KEY_OVERLAY_SETTINGS = new KeyMapping(
        "key." + ExampleMod.MOD_ID + ".route_overlay_options",
        InputConstants.Type.KEYSYM,
        InputConstants.KEY_R | InputConstants.MOD_CONTROL,
        "category." + ExampleMod.MOD_ID + ".category.crn"
    );

    public static void init() {}
}
