package de.mrjulsen.crn;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.google.gson.Gson;

import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

public class Constants {
    public static final ResourceLocation GUI_WIDGETS = new ResourceLocation(ModMain.MOD_ID, "textures/gui/widgets.png");
    public static final int TICKS_PER_DAY = 24000;
    public static final double ONE_HOUR_TICKS = Constants.TICKS_PER_DAY / 24.0D;
    public static final double ONE_MINUTE_TICKS = Constants.ONE_HOUR_TICKS / 60.0D;
    public static final TranslatableComponent TOOLTIP_GO_BACK = new TranslatableComponent("gui." + ModMain.MOD_ID + ".common.go_back");
    public static final TranslatableComponent TOOLTIP_GO_TO_TOP = new TranslatableComponent("gui." + ModMain.MOD_ID + ".common.go_to_top");
    public static final TranslatableComponent TOOLTIP_RESET_DEFAULTS = new TranslatableComponent("gui." + ModMain.MOD_ID + ".common.reset_defaults");
    public static final TranslatableComponent TOOLTIP_EXPAND = new TranslatableComponent("gui." + ModMain.MOD_ID + ".common.expand");
    public static final TranslatableComponent TOOLTIP_COLLAPSE = new TranslatableComponent("gui." + ModMain.MOD_ID + ".common.collapse");
    public static final TranslatableComponent TEXT_COUNT = new TranslatableComponent("gui." + ModMain.MOD_ID + ".common.count");
    public static final TranslatableComponent TEXT_TRUE = new TranslatableComponent("gui." + ModMain.MOD_ID + ".common.true");
    public static final TranslatableComponent TEXT_FALSE = new TranslatableComponent("gui." + ModMain.MOD_ID + ".common.false");
    public static final TranslatableComponent TEXT_SERVER_ERROR = new TranslatableComponent("gui." + ModMain.MOD_ID + ".common.server_error");
    public static final String DEFAULT_SETTINGS_PATH = "./config/" + ModMain.MOD_ID + "_settings.json";
    public static final Gson GSON = new Gson();
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat();
}
