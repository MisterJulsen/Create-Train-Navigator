package de.mrjulsen.crn.block.display.properties.components;

import de.mrjulsen.crn.block.display.properties.IDisplaySettings;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * For data conversion: Indicates that this class adopts the original
 * property {@code platformWidth} from the Advanced Displays.
 * If the class should adopt this property, this interface must be
 * implemented or the value will not be converted!
 */
public interface IPlatformWidthSetting extends ICustomTextWidthSetting {

    public static final String NBT_PLATFORM_WIDTH = "PlatformWidth";

    byte getPlatformWidth();
    void setPlatformWidth(byte b);

    @Environment(EnvType.CLIENT)
    default void buildPlatformWidthGui(GuiBuilderContext context, boolean allowAuto) {
        buildBasicTextWidthGui(context);
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
