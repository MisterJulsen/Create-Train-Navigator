package de.mrjulsen.crn.block.display.properties.components;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.display.properties.IDisplaySettings;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.network.chat.MutableComponent;

public interface IShowTrainMultipleTimes {

    public static final String GUI_LINE_SHOW_TRAIN_MULTIPLE_TIMES_NAME = "show_train_multiple_times";

    public static final String NBT_SHOW_TRAIN_MULTIPLE_TIMES = "ShowTrainMultipleTimes";

    public static final MutableComponent textShowTrainMultipleTimes = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.show_train_multiple_times");
    public static final MutableComponent textShowTrainMultipleTimesDescription = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.show_train_multiple_times.description");

    boolean showTrainMultipleTimes();
    void setShowTrainMultipleTimes(boolean b);

    default void buildShowTrainMultipleTimesGui(GuiBuilderContext context) {
        GuiBuilderWrapper.buildShowTrainMultipleTimesGui(this, context);
    }
    
    default void copyShowTrainMultipleTimesSetting(IDisplaySettings oldSettings) {
        if (oldSettings instanceof IShowTrainMultipleTimes o) {
            setShowTrainMultipleTimes(o.showTrainMultipleTimes());
        }
    }
}
