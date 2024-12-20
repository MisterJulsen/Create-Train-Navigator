package de.mrjulsen.crn.block.display.properties.components;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.display.properties.IDisplaySettings;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.MutableComponent;

public interface IShowNextConnections {

    public static final String GUI_LINE_SHOW_CONNECTIONS_NAME = "show_connections";

    public static final String NBT_SHOW_CONNECTIONS = "ShowConnections";

    public static final MutableComponent textShowConnections = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.show_connections");

    boolean showConnections();
    void setShowConnection(boolean b);

    @Environment(EnvType.CLIENT)
    default void buildShowConnectionGui(GuiBuilderContext context) {
        GuiBuilderWrapper.buildShowConnectionGui(this, context);        
    }
    
    default void copyShowConnectionSetting(IDisplaySettings oldSettings) {
        if (oldSettings instanceof IShowNextConnections o) {
            setShowConnection(o.showConnections());
        }
    }
}
