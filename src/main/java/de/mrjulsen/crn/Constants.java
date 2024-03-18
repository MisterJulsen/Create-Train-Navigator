package de.mrjulsen.crn;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.UUID;

import com.google.gson.Gson;

import de.mrjulsen.mcdragonlib.utils.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class Constants {
    public static final ResourceLocation GUI_WIDGETS = new ResourceLocation(ModMain.MOD_ID, "textures/gui/widgets.png");
    public static final Component ELLIPSIS_STRING = Utils.text("...");
    public static final Component TOOLTIP_GO_BACK = Utils.translate("gui." + ModMain.MOD_ID + ".common.go_back");
    public static final Component TOOLTIP_GO_TO_TOP = Utils.translate("gui." + ModMain.MOD_ID + ".common.go_to_top");
    public static final Component TOOLTIP_RESET_DEFAULTS = Utils.translate("gui." + ModMain.MOD_ID + ".common.reset_defaults");
    public static final Component TOOLTIP_EXPAND = Utils.translate("gui." + ModMain.MOD_ID + ".common.expand");
    public static final Component TOOLTIP_COLLAPSE = Utils.translate("gui." + ModMain.MOD_ID + ".common.collapse");
    public static final Component TEXT_COUNT = Utils.translate("gui." + ModMain.MOD_ID + ".common.count");
    public static final Component TEXT_TRUE = Utils.translate("gui." + ModMain.MOD_ID + ".common.true");
    public static final Component TEXT_FALSE = Utils.translate("gui." + ModMain.MOD_ID + ".common.false");
    public static final Component TEXT_SERVER_ERROR = Utils.translate("gui." + ModMain.MOD_ID + ".common.server_error");
    public static final String TEXT_SEARCH = Utils.translate("common." + ModMain.MOD_ID + ".search").getString();
    public static final Gson GSON = new Gson();
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat();
    public static final int TIME_SHIFT = 6000;
    public static final UUID ZERO_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
     
    public static final int COLOR_ON_TIME = 0x1AEA5F;
    public static final int COLOR_DELAYED = 0xFF4242;
}
