package de.mrjulsen.crn.block.display.properties.components;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.display.properties.IDisplaySettings;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.MutableComponent;

public interface IShowTrainStatsSetting {

    public static final String GUI_LINE_SHOW_ARRIVAL_NAME = "show_stats";

    public static final String NBT_SHOW_STATS = "ShowStats";

    public static final MutableComponent textShowStats = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.show_stats");

    boolean showStats();
    void setShowStats(boolean b);

    @Environment(EnvType.CLIENT)
    default void buildShowStatsGui(GuiBuilderContext context) {
        GuiBuilderWrapper.buildShowStatsGui(this, context);
    }
    
    default void copyShowStatsSetting(IDisplaySettings oldSettings) {
        if (oldSettings instanceof IShowTrainStatsSetting o) {
            setShowStats(o.showStats());
        }
    }
}
