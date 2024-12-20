package de.mrjulsen.crn.block.display.properties.components;

import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public interface ICustomTextWidthSetting {    
    public static final String GUI_LINE_TEXT_SIZE_NAME = "text_width";
    
    public static final int USED_LINE_SPACE = 18 + 4;

    @Environment(EnvType.CLIENT)
    default void buildBasicTextWidthGui(GuiBuilderContext context) {
        GuiBuilderWrapper.buildBasicTextWidthGui(this, context);
    }
}
