package de.mrjulsen.crn.block.display.properties.components;

import de.mrjulsen.crn.block.display.properties.IDisplaySettings;
import de.mrjulsen.crn.block.properties.ETimeDisplay;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * For data conversion: Indicates that this class adopts the original
 * property {@code timeDisplay} from the Advanced Displays.
 * If the class should adopt this property, this interface must be
 * implemented or the value will not be converted!
 */
public interface ITimeDisplaySetting extends ICustomTextWidthSetting {

    public static final String GUI_LINE_TIME_NAME = "time";
    public static final String NBT_TIME_DISPLAY = "TimeDisplay";

    ETimeDisplay getTimeDisplay();
    void setTimeDisplay(ETimeDisplay display);

    @Environment(EnvType.CLIENT)
    default void buildTimeDisplayGui(GuiBuilderContext context) {
        GuiBuilderWrapper.buildTimeDisplayGui(this, context);
    }
    
    default void copyTimeDisplaySetting(IDisplaySettings oldSettings) {
        if (oldSettings instanceof ITimeDisplaySetting o) {
            setTimeDisplay(o.getTimeDisplay());
        }
    }
}
