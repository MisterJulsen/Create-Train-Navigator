package de.mrjulsen.crn.block.display.properties.components;

import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;

public interface ICustomTextWidthSetting {
    public static final String GUI_LINE_TEXT_SIZE_NAME = "text_width";
    
    public static final int USED_LINE_SPACE = 18 + 4;

    default void buildBasicTextWidthGui(GuiBuilderContext context) {
        GuiBuilderWrapper.buildBasicTextWidthGui(context);
    }
}
