package de.mrjulsen.crn.config;

import de.mrjulsen.crn.client.gui.overlay.OverlayPosition;
import de.mrjulsen.crn.client.lang.CustomLanguage;
import de.mrjulsen.crn.util.ESpeedUnit;
import de.mrjulsen.crn.util.ETimeFormat;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ModClientConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.ConfigValue<Integer> DISPLAY_REFRESH_RATE;
    public static final ModConfigSpec.ConfigValue<Double> OVERLAY_SCALE;
    public static final ModConfigSpec.ConfigValue<Boolean> ROUTE_NOTIFICATIONS;
    public static final ModConfigSpec.ConfigValue<OverlayPosition> ROUTE_OVERLAY_POSITION;
    public static final ModConfigSpec.ConfigValue<ETimeFormat> TIME_FORMAT;
    public static final ModConfigSpec.ConfigValue<CustomLanguage> LANGUAGE;
    public static final ModConfigSpec.ConfigValue<ESpeedUnit> SPEED_UNIT;

    public static final double MIN_SCALE = 0.25f;
    public static final double MAX_SCALE = 2.0f;

    static {
        BUILDER.push("Create Railways Navigator Config");

        /* CONFIGS */
        DISPLAY_REFRESH_RATE = BUILDER.comment(new String[] {"[in Ticks]", "The interval in which the displays are updated with new information. Lower values can decrease performance and increase the network traffic, while larger values reduce the reaction speed of the displays. (Default: 50, Recommended: 50 - 100)"})
            .defineInRange("general.display_refresh_rate", 50, 20, 200);
        
        OVERLAY_SCALE = BUILDER.comment("Scale of the route overlay UI. (Default: 0.75)")
            .defineInRange("route_overlay.scale", 0.75f, MIN_SCALE, MAX_SCALE);
        ROUTE_NOTIFICATIONS = BUILDER.comment("If active, you will receive short toasts about important events on your trip, e.g. delays, changes, ... (Default: ON)")
            .define("route_overlay.notifications", true);
        ROUTE_OVERLAY_POSITION = BUILDER.comment("The position on your screen where you want the overlay to appear. (Default: Top Left)")
            .defineEnum("route_overlay.position", OverlayPosition.TOP_LEFT);
        
        LANGUAGE = BUILDER.comment("The language that should be used for announcements of the navigator. Can be different from the game's language settings. (Default: Default)")
            .defineEnum("language", CustomLanguage.DEFAULT);
        SPEED_UNIT = BUILDER.comment("The unit to be used to represent speed. (Default: KMH)")
            .defineEnum("speed_unit", ESpeedUnit.KMH);
        TIME_FORMAT = BUILDER.comment("Display Time Format. (Default: Hours 24)")
            .defineEnum("time_format", ETimeFormat.HOURS_24);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static void resetSearchSettings() {
    }
}
