package de.mrjulsen.crn.config;

import de.mrjulsen.crn.client.gui.overlay.OverlayPosition;
import de.mrjulsen.crn.client.lang.ELanguage;
import de.mrjulsen.crn.util.ESpeedUnit;
import de.mrjulsen.mcdragonlib.util.TimeUtils.TimeFormat;
import net.minecraftforge.common.ForgeConfigSpec;

public class ModClientConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    
    public static final ForgeConfigSpec.ConfigValue<Integer> REALTIME_PRECISION_THRESHOLD;
    public static final ForgeConfigSpec.ConfigValue<Integer> NEXT_STOP_ANNOUNCEMENT;
    public static final ForgeConfigSpec.ConfigValue<Integer> DISPLAY_LEAD_TIME;
    public static final ForgeConfigSpec.ConfigValue<Double> OVERLAY_SCALE;
    public static final ForgeConfigSpec.ConfigValue<Boolean> ROUTE_NOTIFICATIONS;
    public static final ForgeConfigSpec.ConfigValue<OverlayPosition> ROUTE_OVERLAY_POSITION;
    public static final ForgeConfigSpec.ConfigValue<TimeFormat> TIME_FORMAT;
    public static final ForgeConfigSpec.ConfigValue<ELanguage> LANGUAGE;
    public static final ForgeConfigSpec.ConfigValue<ESpeedUnit> SPEED_UNIT;

    public static final double MIN_SCALE = 0.25f;
    public static final double MAX_SCALE = 2.0f;

    static {
        BUILDER.push("Create Railways Navigator Config");

        /* CONFIGS */
        NEXT_STOP_ANNOUNCEMENT = BUILDER.comment(new String[] {"[in Ticks]", "The next stop or information about the start of the journey is announced in the specified number of ticks before the scheduled arrival at the next station. (Default: 500, 30 real life seconds)"})
            .defineInRange("general.next_stop_announcement", 500, 100, 1000);
        REALTIME_PRECISION_THRESHOLD = BUILDER.comment(new String[] {"[in Ticks]", "This value indicates how accurately the real-time data should be displayed. By default, only deviations above 10 in-game minutes (167 ticks, approx. 8 real life seconds) are displayed. The lower the value, the more accurate the real-time data but also the more often deviations from the schedule occur. (Default: 167, 10 in-game minutes)"})
            .defineInRange("general.realtime_precision_threshold", 167, 1, 1000); 
        DISPLAY_LEAD_TIME = BUILDER.comment(new String[] {"[in Ticks]", "How early a train should be shown on the display. (Default: 1000, 1 in-game hour)"})
            .defineInRange("general.display_lead_time", 1000, 100, 24000);
        OVERLAY_SCALE = BUILDER.comment("Scale of the route overlay UI. (Default: 0.75)")
            .defineInRange("route_overlay.scale", 0.75f, MIN_SCALE, MAX_SCALE);
        ROUTE_NOTIFICATIONS = BUILDER.comment("If active, you will receive short toasts about important events on your trip, e.g. delays, changes, ... (Default: ON)")
            .define("route_overlay.notifications", true);
        ROUTE_OVERLAY_POSITION = BUILDER.comment("The position on your screen where you want the overlay to appear. (Default: Top Left)")
            .defineEnum("route_overlay.position", OverlayPosition.TOP_LEFT);
        
        LANGUAGE = BUILDER.comment("The language that should be used for announcements of the navigator. Can be different from the game's language settings. (Default: Default)")
            .defineEnum("language", ELanguage.DEFAULT);
        SPEED_UNIT = BUILDER.comment("The unit to be used to represent speed. (Default: KMH)")
            .defineEnum("speed_unit", ESpeedUnit.KMH);
        TIME_FORMAT = BUILDER.comment("Display Time Format. (Default: Hours 24)")
            .defineEnum("time_format", TimeFormat.HOURS_24);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static void resetSearchSettings() {
    }
}
