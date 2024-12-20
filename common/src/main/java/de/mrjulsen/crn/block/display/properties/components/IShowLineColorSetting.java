package de.mrjulsen.crn.block.display.properties.components;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.display.properties.IDisplaySettings;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.MutableComponent;

public interface IShowLineColorSetting {

    public static final String GUI_LINE_SHOW_LINE_COLOR_NAME = "show_line_color";

    public static final String NBT_SHOW_LINE_COLOR = "ShowLineColor";

    public static final MutableComponent textShowLineColor = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.show_line_color");
    public static final MutableComponent textShowLineColorDescription = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.show_line_color.description");

    boolean showLineColor();
    void setShowLineColor(boolean b);

    @Environment(EnvType.CLIENT)
    default void buildShowLineColorGui(GuiBuilderContext context) {
        GuiBuilderWrapper.buildShowLineColorGui(this, context);
    }
    
    default void copyShowLineColorSetting(IDisplaySettings oldSettings) {
        if (oldSettings instanceof IShowLineColorSetting o) {
            setShowLineColor(o.showLineColor());
        }
    }
}
