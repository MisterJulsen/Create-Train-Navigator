/*
 * Create Railways Navigator 1.21.1
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.client;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.mcdragonlib.client.render.DLTextureSheet;
import de.mrjulsen.mcdragonlib.client.util.DLTexture;
import de.mrjulsen.mcdragonlib.util.DLUtils;

public final class CRNGui {
    public static final DLTexture GUI = new DLTexture(DLUtils.resourceLocation(CreateRailwaysNavigator.MOD_ID, "textures/gui/gui.png"), 64, 64);
    public static final DLTextureSheet GUI_SPRITES = new DLTextureSheet(DLUtils.resourceLocation(CreateRailwaysNavigator.MOD_ID, "textures/gui/gui.png"));
}
