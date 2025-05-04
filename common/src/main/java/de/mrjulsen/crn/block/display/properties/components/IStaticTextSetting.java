package de.mrjulsen.crn.block.display.properties.components;

import de.mrjulsen.crn.block.display.properties.IDisplaySettings;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;

/**
 * For data conversion: Indicates that this class adopts the original
 * property {@code timeDisplay} from the Advanced Displays.
 * If the class should adopt this property, this interface must be
 * implemented or the value will not be converted!
 */
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
