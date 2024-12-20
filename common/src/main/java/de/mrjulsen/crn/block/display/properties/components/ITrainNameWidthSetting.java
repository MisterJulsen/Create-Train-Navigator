package de.mrjulsen.crn.block.display.properties.components;

import de.mrjulsen.crn.block.display.properties.IDisplaySettings;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * For data conversion: Indicates that this class adopts the original
 * property {@code timeDisplay} from the Advanced Displays.
 * If the class should adopt this property, this interface must be
 * implemented or the value will not be converted!
 */
public interface ITrainNameWidthSetting extends ICustomTextWidthSetting {

    public static final int DEFAULT_TRAIN_NAME_WIDTH = 16;
    public static final int MIN_VALUE = 0;
    public static final int MAX_VALUE = 100;
    public static final String NBT_TRAIN_NAME_WIDTH = "TrainNameWidth";
    
    byte getTrainNameWidth();
    void setTrainNameWidth(byte b);

    @Environment(EnvType.CLIENT)
    default void buildTrainNameGui(GuiBuilderContext context, boolean allowAuto, boolean allowMax) {
        buildBasicTextWidthGui(context);
        GuiBuilderWrapper.buildTrainNameGui(this, context, allowAuto, allowMax);
    }
    
    default void copyTrainNameSetting(IDisplaySettings oldSettings) {
        if (oldSettings instanceof ITrainNameWidthSetting o) {
            setTrainNameWidth(o.getTrainNameWidth());
        }
    }

    default boolean isAutoTrainNameWidth() {
        return getTrainNameWidth() < MIN_VALUE;
    }

    default boolean isFullTrainNameWidth() {
        return getTrainNameWidth() >= MAX_VALUE;
    }
}
