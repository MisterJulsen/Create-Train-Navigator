package de.mrjulsen.crn.block.display.properties.components;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.display.properties.IDisplaySettings;
import de.mrjulsen.crn.client.gui.widgets.modular.GuiBuilderContext;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.network.chat.MutableComponent;

public interface IRetainScaleAndPosSetting {
  String GUI_LINE_RETAIN_NAME = "retain_scale_pos";

  boolean DEFAULT_RETAIN_SCALE_POS = false;
  String NBT_RETAIN_SCALE_POS = "RetainScalePos";

  MutableComponent txtRetain = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.retain_scale_pos");
  MutableComponent txtRetainDescription = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".advanced_display_settings.retain_scale_pos.description");

  boolean shouldRetainScaleAndPos();
  void setShouldRetainScaleAndPos(boolean v);

  default void buildRetainGui(GuiBuilderContext context) {
    GuiBuilderWrapper.buildRetainGui(this, context);
  }

  default void copyRetainSettings(IDisplaySettings oldSettings) {
    if (oldSettings instanceof IRetainScaleAndPosSetting o) {
      setShouldRetainScaleAndPos(o.shouldRetainScaleAndPos());
    }
  }
}
