package de.mrjulsen.crn.block.display.properties.components;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.display.properties.IDisplaySettings;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;

public interface IColorSetting {

    public static final String GUI_LINE_COLORS_NAME = "color";

    public static final String NBT_FONT_COLOR = "FontColor";
    public static final String NBT_BACK_COLOR = "BackColor";

    public static final MutableComponent textFontColor = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.font_color");
    public static final MutableComponent textBackColor = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.back_color");
    public static final MutableComponent textClickToEdit = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".common.click_to_edit").withStyle(ChatFormatting.DARK_GRAY).withStyle(ChatFormatting.ITALIC);

    int getFontColor();
    int getBackColor();
    void setFontColor(int color);
    void setBackColor(int color);

    @Environment(EnvType.CLIENT)
    default void buildColorGui(GuiBuilderContext context) {
        GuiBuilderWrapper.buildColorGui(this, context);
    }
    
    default void copyColorSetting(IDisplaySettings oldSettings) {
        if (oldSettings instanceof IColorSetting o) {
            setBackColor(o.getBackColor());
            setFontColor(o.getFontColor());
        }
    }
}
