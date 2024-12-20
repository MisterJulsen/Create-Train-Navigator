package de.mrjulsen.crn.block.display.properties.components;

import de.mrjulsen.crn.block.display.properties.IDisplaySettings;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.MutableComponent;

public interface ICarriageIndexSetting {

    public static final String GUI_LINE_CARRIAGE_INDEX_NAME = "carriage_index";

    public static final String NBT_CARRIAGE_INDEX = "CarriageIndexOffset";
    public static final String NBT_OVERWRITE_CARRIAGE_INDEX = "OverwriteCarriageIndex";

    public static final MutableComponent textOverwriteCarriageIndex = TextUtils.translate("gui.createrailwaysnavigator.advanced_display_settings.overwrite_carriage_index");
    public static final MutableComponent textOverwriteCarriageIndexDescription = TextUtils.translate("gui.createrailwaysnavigator.advanced_display_settings.overwrite_carriage_index.description");
    
    byte getCarriageIndex();
    boolean shouldOverwriteCarriageIndex();
    void setCarriageIndex(byte b);
    void setOverwriteCarriageIndex(boolean b);

    @Environment(EnvType.CLIENT)
    default void buildCarriageIndexGui(GuiBuilderContext context) {
        GuiBuilderWrapper.buildCarriageIndexGui(this, context);
    }
    
    default void copyCarriageIndexSetting(IDisplaySettings oldSettings) {
        if (oldSettings instanceof ICarriageIndexSetting o) {
            setCarriageIndex(o.getCarriageIndex());
            setOverwriteCarriageIndex(o.shouldOverwriteCarriageIndex());
        }
    }
}
