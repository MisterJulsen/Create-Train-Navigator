package de.mrjulsen.crn.block.display.properties.components;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.display.properties.IDisplaySettings;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.MutableComponent;

public interface IShowArrivalSetting {

    public static final String GUI_LINE_SHOW_ARRIVAL_NAME = "show_arrival";

    public static final String NBT_SHOW_ARRIVAL = "ShowArrival";

    public static final MutableComponent textShowArrival = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.show_arrival");
    public static final MutableComponent textShowArrivalDescription = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.show_arrival.description");

    boolean showArrival();
    void setShowArrival(boolean b);

    @Environment(EnvType.CLIENT)
    default void buildShowArrivalGui(GuiBuilderContext context) {
        GuiBuilderWrapper.buildShowArrivalGui(this, context);
    }
    
    default void copyShowArrivalSetting(IDisplaySettings oldSettings) {
        if (oldSettings instanceof IShowArrivalSetting o) {
            setShowArrival(o.showArrival());
        }
    }
}
