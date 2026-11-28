/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.block.display.properties.components;

import de.mrjulsen.crn.block.display.properties.IDisplaySettings;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;

public interface IStaticTextSetting {

    public static final String GUI_STATIC_TEXT_NAME = "static_text";
    public static final String DEFAULT_TEXT = "Hello World";
    public static final String NBT_TEXT = "StaticText";

    String getStaticText();
    void setStaticText(String text);

    default void buildStaticTextGui(GuiBuilderContext context) {
        GuiBuilderWrapper.buildStaticTextGui(this, context);
    }

    default void copyStaticTextSettings(IDisplaySettings oldSettings) {
        if (oldSettings instanceof IStaticTextSetting o) {
            setStaticText(o.getStaticText());
        }
    }
}
