package de.mrjulsen.crn;

import java.util.UUID;

import de.mrjulsen.mcdragonlib.client.util.DLTexture;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.time.DLTime;
import de.mrjulsen.mcdragonlib.util.time.DLTimeUnit;
import de.mrjulsen.mcdragonlib.util.time.VanillaTimeSystem;
import de.mrjulsen.mcdragonlib.util.time.format.ITimeFormatter;
import de.mrjulsen.mcdragonlib.util.time.format.TimeFormatDigitalDuration;
import de.mrjulsen.mcdragonlib.util.time.format.TimeFormatVerboseDuration;
import net.minecraft.Util;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class Constants {
    public static final DLTexture GUI_WIDGETS = new DLTexture(DLUtils.resourceLocation(CreateRailwaysNavigator.MOD_ID, "textures/gui/widgets.png"), 256, 256);
    public static final Component ELLIPSIS_STRING = TextUtils.text("...");
    public static final Component TOOLTIP_GO_BACK = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".common.go_back");
    public static final Component TOOLTIP_GO_TO_TOP = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".common.go_to_top");
    public static final Component TOOLTIP_RESET_DEFAULTS = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".common.reset_defaults");
    public static final Component TOOLTIP_EXPAND = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".common.expand");
    public static final Component TOOLTIP_COLLAPSE = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".common.collapse");
    public static final Component TEXT_COUNT = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".common.count");
    public static final Component TEXT_TRUE = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".common.true");
    public static final Component TEXT_FALSE = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".common.false");
    public static final Component TEXT_SERVER_ERROR = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".common.server_error");
    public static final Component TEXT_SEARCH = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".common.search");
    public static final Component TEXT_PIN = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".common.pin");
    public static final Component TEXT_UNPIN = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".common.unpin");
    public static final Component TEXT_MORE_OPTIONS = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".common.more_options");
    public static final Component TEXT_HELP = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".common.help");
    public static final Component TEXT_COPY = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".common.copy");
    public static final Component TEXT_PASTE = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".common.paste");
    public static final Component TEXT_RESET = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".common.reset_defaults");
    public static final Component TEXT_REMOVE = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".common.remove");
    public static final Component TEXT_DELETE = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".common.delete");
    public static final Component TEXT_ADD = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".common.add");
    public static final Component TEXT_NEW = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".common.new");
    public static final Component TEXT_CLEAR_ALL = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".common.clear_all");
    public static final Component TEXT_CLEAR = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".common.clear");
    public static final Component TEXT_LOADING = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".common.loading");
    public static final Component TEXT_READ_ONLY = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".common.read_only");
    public static final Component TEXT_SERVER = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".common.server");
    public static final UUID ZERO_UUID = new UUID(0, 0);
    public static final DLColor[] DEFAULT_TRAIN_TYPE_COLORS = new DLColor[] { DLColor.fromInt(0xFF393939), DLColor.fromInt(0xFFf0f3f5), DLColor.fromInt(0xFFafb4bb), DLColor.fromInt(0xFF878c96), DLColor.fromInt(0xFF2a7230), DLColor.fromInt(0xFF814997), DLColor.fromInt(0xFF1455c0), DLColor.fromInt(0xFFa9455d), DLColor.fromInt(0xFF55b9e6), DLColor.fromInt(0xFFffd800) };
     
    public static final DLColor COLOR_ON_TIME = DLColor.fromInt(0xFF1AEA5F);
    public static final DLColor COLOR_DELAYED = DLColor.fromInt(0xFFFF4242);
    public static final DLColor COLOR_TRAIN_BACKGROUND = DLColor.fromInt(0xFF393939);

    public static final DLTime NULL_TIME = new DLTime(0, VanillaTimeSystem.INSTANCE);
    public static final ITimeFormatter DEFAULT_REAL_DURATION_FORMAT = new TimeFormatDigitalDuration(DLTimeUnit.SECONDS, false);
    public static final ITimeFormatter DEFAULT_GAME_DURATION_FORMAT = new TimeFormatDigitalDuration(DLTimeUnit.MINUTES, false);
    public static final ITimeFormatter DEFAULT_VERBOSE_REAL_DURATION_FORMAT = new TimeFormatVerboseDuration(new TimeFormatVerboseDuration.Config().showDays(true).showHours(true).showMinutes(true).showSeconds(true));
    public static final ITimeFormatter DEFAULT_VERBOSE_GAME_DURATION_FORMAT = new TimeFormatVerboseDuration(new TimeFormatVerboseDuration.Config().showDays(true).showHours(true).showMinutes(true).showSeconds(false));

    public static final String WIKI = "https://wiki.mrjulsen.net/createrailwaysnavigator/";
    public static final String HELP_PAGE_ADVANCED_DISPLAYS = WIKI + "blocks/advanced_displays/";
    public static final String HELP_PAGE_DYNAMIC_DELAYS = WIKI + "schedule_entries/dynamic_delay/";
    public static final String HELP_PAGE_TRAIN_SEPARATION = WIKI + "schedule_entries/train_separation/";
    public static final String HELP_PAGE_PRIORITIZED_DESTINATION_INSTRUCTION = WIKI + "schedule_entries/prioritized_destination_instruction/";
    public static final String HELP_PAGE_GLOBAL_SETTINGS = WIKI + "features/global_settings/";
    public static final String HELP_PAGE_NAVIGATION_WARNING = WIKI + "other_topics/navigation_warning/";
    public static final String HELP_PAGE_SCHEDULE_SECTIONS = WIKI + "features/schedule_section/";
    public static final String HELP_PAGE_SCHEDULED_TIMES_AND_REAL_TIME = WIKI + "other_topics/scheduled_and_real_time/";
    public static final String HELP_PAGE_STATION_BLACKLIST = WIKI + "features/blacklists/#station-blacklist";
    public static final String HELP_PAGE_STATION_TAGS = WIKI + "features/station_tag/";
    public static final String HELP_PAGE_TRAIN_BLACKLIST = WIKI + "features/blacklists/#train-blacklist";
    public static final String HELP_PAGE_TRAIN_CATEGORIES = WIKI + "features/train_category/";
    public static final String HELP_PAGE_TRAIN_INITIALIZATION = WIKI + "other_topics/train_initialization/";
    public static final String HELP_PAGE_TRAIN_LINES = WIKI + "features/train_lines/";
}
