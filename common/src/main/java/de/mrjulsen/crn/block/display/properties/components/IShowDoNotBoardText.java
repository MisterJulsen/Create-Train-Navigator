package de.mrjulsen.crn.block.display.properties.components;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.display.properties.IDisplaySettings;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.MutableComponent;

public interface IShowDoNotBoardText {

    public static final String GUI_LINE_SHOW_DO_NOT_BOARD_TEXT_NAME = "show_do_not_board_text";

    public static final String NBT_SHOW_DO_NOT_BOARD_TEXT = "ShowDoNotBoardText";

    public static final MutableComponent textShowDoNotBoardText = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.show_do_not_board_text");
    public static final MutableComponent textShowDoNotBoardTextDescription = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.show_do_not_board_text.description");

    boolean showDoNotBoardText();
    void setShowDoNotBoardText(boolean b);

    @Environment(EnvType.CLIENT)
    default void buildShowDoNotBoardTextGui(GuiBuilderContext context) {
        GuiBuilderWrapper.buildShowDoNotBoardTextGui(this, context);
    }
    
    default void copyShowDoNotBoardTextSetting(IDisplaySettings oldSettings) {
        if (oldSettings instanceof IShowDoNotBoardText o) {
            setShowDoNotBoardText(o.showDoNotBoardText());
        }
    }
}
