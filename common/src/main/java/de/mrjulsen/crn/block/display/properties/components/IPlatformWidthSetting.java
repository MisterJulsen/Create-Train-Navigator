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
