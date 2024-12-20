package de.mrjulsen.crn.block.display.properties.components;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.display.properties.IDisplaySettings;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.MutableComponent;

public interface IShowExitDirectionSetting {

    public static final String GUI_LINE_SHOW_ARRIVAL_NAME = "show_exit";

    public static final String NBT_SHOW_EXIT = "ShowExit";

    public static final MutableComponent textShowExit = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.show_exit");

    boolean showExit();
    void setShowExit(boolean b);

    @Environment(EnvType.CLIENT)
    default void buildShowExitGui(GuiBuilderContext context) {
        GuiBuilderWrapper.buildShowExitGui(this, context);
    }
    
    default void copyShowExitSetting(IDisplaySettings oldSettings) {
        if (oldSettings instanceof IShowExitDirectionSetting o) {
            setShowExit(o.showExit());
        }
    }
}
