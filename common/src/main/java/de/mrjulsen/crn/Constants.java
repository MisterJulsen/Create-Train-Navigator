package de.mrjulsen.crn;

import java.util.UUID;

import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class Constants {
    public static final ResourceLocation GUI_WIDGETS = new ResourceLocation(ExampleMod.MOD_ID, "textures/gui/widgets.png");
    public static final Component ELLIPSIS_STRING = TextUtils.text("...");
    public static final Component TOOLTIP_GO_BACK = TextUtils.translate("gui." + ExampleMod.MOD_ID + ".common.go_back");
    public static final Component TOOLTIP_GO_TO_TOP = TextUtils.translate("gui." + ExampleMod.MOD_ID + ".common.go_to_top");
    public static final Component TOOLTIP_RESET_DEFAULTS = TextUtils.translate("gui." + ExampleMod.MOD_ID + ".common.reset_defaults");
    public static final Component TOOLTIP_EXPAND = TextUtils.translate("gui." + ExampleMod.MOD_ID + ".common.expand");
    public static final Component TOOLTIP_COLLAPSE = TextUtils.translate("gui." + ExampleMod.MOD_ID + ".common.collapse");
    public static final Component TEXT_COUNT = TextUtils.translate("gui." + ExampleMod.MOD_ID + ".common.count");
    public static final Component TEXT_TRUE = TextUtils.translate("gui." + ExampleMod.MOD_ID + ".common.true");
    public static final Component TEXT_FALSE = TextUtils.translate("gui." + ExampleMod.MOD_ID + ".common.false");
    public static final Component TEXT_SERVER_ERROR = TextUtils.translate("gui." + ExampleMod.MOD_ID + ".common.server_error");
    public static final Component TEXT_SEARCH = TextUtils.translate("gui." + ExampleMod.MOD_ID + ".common.search");
    public static final UUID ZERO_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
     
    public static final int COLOR_ON_TIME = 0x1AEA5F;
    public static final int COLOR_DELAYED = 0xFF4242;
}
