package de.mrjulsen.crn.block.display.properties.components;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.display.properties.IDisplaySettings;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.network.chat.MutableComponent;

/**
 * For data conversion: Indicates that this class adopts the original
 * property {@code timeDisplay} from the Advanced Displays.
 * If the class should adopt this property, this interface must be
 * implemented or the value will not be converted!
 */
public interface ITextBackgroundColorSetting {

    public static final String GUI_BG_COLOR_NAME = "text_bg_color";

    public static final DLColor DEFAULT_BG_COLOR = DLColor.TRANSPARENT;
    public static final boolean DEFAULT_FULL_LABEL_COLOR = false;

    public static final String NBT_TEXT_BG_COLOR = "TextBackgroundColor";
    public static final String NBT_FULL_LABEL_COLOR = "FullLabelBackgroundColor";

    public static final MutableComponent txtLabelBackgroundColor = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.label_background_color");
    public static final MutableComponent txtFullSize = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.background_color_full_size");
    public static final MutableComponent txtFullSizeDescription = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.background_color_full_size.description");
    
    DLColor getTextBackgroundColor();
    void setTextBackgroundColor(DLColor color);
    boolean isFullLabelBackgroundColor();
    void setFullLabelBackgroundColor(boolean b);

    default void buildTextBackgroundColorGui(GuiBuilderContext context) {
        GuiBuilderWrapper.buildTextBackgroundColorGui(this, context);
    }
    
    default void copyTextBackgroundColorSettings(IDisplaySettings oldSettings) {
        if (oldSettings instanceof ITextBackgroundColorSetting o) {
            setTextBackgroundColor(o.getTextBackgroundColor());
            setFullLabelBackgroundColor(o.isFullLabelBackgroundColor());
        }
    }
}
