package de.mrjulsen.crn.block.display.properties.components;

import de.mrjulsen.crn.block.display.properties.IDisplaySettings;
import de.mrjulsen.crn.block.properties.ETimeDisplay;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;

public interface ITimeDisplaySetting extends ICustomTextWidthSetting {

    public static final String GUI_LINE_TIME_NAME = "time";
    public static final String NBT_TIME_DISPLAY = "TimeDisplay";

    ETimeDisplay getTimeDisplay();
    void setTimeDisplay(ETimeDisplay display);

    default void buildTimeDisplayGui(GuiBuilderContext context) {
        GuiBuilderWrapper.buildTimeDisplayGui(this, context);
    }

    default void copyTimeDisplaySetting(IDisplaySettings oldSettings) {
        if (oldSettings instanceof ITimeDisplaySetting o) {
            setTimeDisplay(o.getTimeDisplay());
        }
    }
}
