/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.block.display.properties.components;

import de.mrjulsen.crn.block.display.properties.IDisplaySettings;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;

public interface IPlatformWidthSetting extends ICustomTextWidthSetting {

    public static final String NBT_PLATFORM_WIDTH = "PlatformWidth";

    byte getPlatformWidth();
    void setPlatformWidth(byte b);

    default void buildPlatformWidthGui(GuiBuilderContext context, boolean allowAuto) {
        GuiBuilderWrapper.buildPlatformWidthGui(this, context, allowAuto);
    }

    default void copyPlatformWidthSetting(IDisplaySettings oldSettings) {
        if (oldSettings instanceof IPlatformWidthSetting o) {
            setPlatformWidth(o.getPlatformWidth());
        }
    }

    default boolean isAutoPlatformWidth() {
        return getPlatformWidth() < 0;
    }
}
