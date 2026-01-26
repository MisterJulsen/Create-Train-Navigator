package de.mrjulsen.crn.client;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.mcdragonlib.client.render.DefaultGuiTextures;
import de.mrjulsen.mcdragonlib.client.util.DLTexture;
import de.mrjulsen.mcdragonlib.util.DLUtils;

public final class CRNGui {
    public static final DLTexture GUI = new DLTexture(DLUtils.resourceLocation(CreateRailwaysNavigator.MOD_ID, "textures/gui/gui.png"), 64, 64);
    public static final DefaultGuiTextures GUI_SPRITES = new DefaultGuiTextures(DLUtils.resourceLocation(CreateRailwaysNavigator.MOD_ID, "textures/gui/gui.png"));
}
