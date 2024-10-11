package de.mrjulsen.crn;

import java.util.UUID;

import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class Constants {
    public static final ResourceLocation GUI_WIDGETS = new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "textures/gui/widgets.png");
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
    public static final Component TEXT_HELP = TextUtils.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".common.help");
    public static final UUID ZERO_UUID = new UUID(0, 0);
    public static final int[] DEFAULT_TRAIN_TYPE_COLORS = new int[] { 0xFF393939, 0xFFf0f3f5, 0xFFafb4bb, 0xFF878c96, 0xFF2a7230, 0xFF814997, 0xFF1455c0, 0xFFa9455d, 0xFF55b9e6, 0xFFffd800 };
     
    public static final int COLOR_ON_TIME = 0xFF1AEA5F;
    public static final int COLOR_DELAYED = 0xFFFF4242;
    public static final int COLOR_TRAIN_BACKGROUND = 0xFF393939;

    public static final String GITHUB_WIKI = "https://github.com/MisterJulsen/Create-Train-Navigator/wiki/";
    public static final String HELP_PAGE_ADVANCED_DISPLAYS = GITHUB_WIKI + "Advanced-Displays";
    public static final String HELP_PAGE_DYNAMIC_DELAYS = GITHUB_WIKI + "Dynamic-Delays";
    public static final String HELP_PAGE_GLOBAL_SETTINGS = GITHUB_WIKI + "Global-Settings";
    public static final String HELP_PAGE_NAVIGATION_WARNING = GITHUB_WIKI + "Navigation-Warning";
    public static final String HELP_PAGE_SCHEDULE_SECTIONS = GITHUB_WIKI + "Train-Schedule-Sections";
    public static final String HELP_PAGE_SCHEDULED_TIMES_AND_REAL_TIME = GITHUB_WIKI + "Scheduled-Time-and-Real-Time";
    public static final String HELP_PAGE_STATION_BLACKLIST = GITHUB_WIKI + "Station-Blacklist";
    public static final String HELP_PAGE_STATION_TAGS = GITHUB_WIKI + "Station-Tags";
    public static final String HELP_PAGE_TRAIN_BLACKLIST = GITHUB_WIKI + "Train-Blacklist";
    public static final String HELP_PAGE_TRAIN_GROUPS = GITHUB_WIKI + "Train-Groups";
    public static final String HELP_PAGE_TRAIN_INITIALIZATION = GITHUB_WIKI + "Train-Imnitialization";
    public static final String HELP_PAGE_TRAIN_LINES = GITHUB_WIKI + "Train-Lines";
}
