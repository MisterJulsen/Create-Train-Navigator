package de.mrjulsen.crn.client.input;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import de.mrjulsen.crn.ModMain;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;

public class KeyBinding {
    public static final String KEY_CAT_MOD = "key." + ModMain.MOD_ID + ".category.crn";
    public static final String KEY_OPEN_OVERLAY_SETTINGS = "key." + ModMain.MOD_ID + ".route_overlay_options";

    public static final KeyMapping OPEN_SETTINGS_KEY = new KeyMapping(KEY_OPEN_OVERLAY_SETTINGS, KeyConflictContext.IN_GAME, KeyModifier.CONTROL, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, KEY_CAT_MOD);
}
