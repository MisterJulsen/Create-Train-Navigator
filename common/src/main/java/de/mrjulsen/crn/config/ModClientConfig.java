package de.mrjulsen.crn.config;

import java.util.ArrayList;
import java.util.List;

import de.mrjulsen.crn.client.gui.overlay.OverlayPosition;
import de.mrjulsen.crn.client.lang.ELanguage;
import de.mrjulsen.crn.util.ESpeedUnit;
import de.mrjulsen.mcdragonlib.util.TimeUtils.TimeFormat;
import net.minecraftforge.common.ForgeConfigSpec;

public class ModClientConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    
    public static final ForgeConfigSpec.ConfigValue<Integer> REALTIME_PRECISION_THRESHOLD;
    public static final ForgeConfigSpec.ConfigValue<Integer> DEVIATION_THRESHOLD;
    public static final ForgeConfigSpec.ConfigValue<Integer> NEXT_STOP_ANNOUNCEMENT;
    public static final ForgeConfigSpec.ConfigValue<Integer> DISPLAY_LEAD_TIME;
    public static final ForgeConfigSpec.ConfigValue<Integer> REALTIME_EARLY_ARRIVAL_THRESHOLD;
    public static final ForgeConfigSpec.ConfigValue<Double> OVERLAY_SCALE;
    public static final ForgeConfigSpec.ConfigValue<Integer> TRANSFER_TIME;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> TRAIN_GROUP_FILTER_BLACKLIST;
    public static final ForgeConfigSpec.ConfigValue<Boolean> ROUTE_NARRATOR;
    public static final ForgeConfigSpec.ConfigValue<Boolean> ROUTE_NOTIFICATIONS;
    public static final ForgeConfigSpec.ConfigValue<OverlayPosition> ROUTE_OVERLAY_POSITION;
    public static final ForgeConfigSpec.ConfigValue<TimeFormat> TIME_FORMAT;
    public static final ForgeConfigSpec.ConfigValue<ELanguage> LANGUAGE;
    public static final ForgeConfigSpec.ConfigValue<ESpeedUnit> SPEED_UNIT;

    public static final int MAX_TRANSFER_TIME = 24000;
    public static final double MIN_SCALE = 0.25f;
    public static final double MAX_SCALE = 2.0f;

    static {
        BUILDER.push("Create Railways Navigator Config");

        /* CONFIGS */
        DEVIATION_THRESHOLD = BUILDER.comment("The value indicates how much deviation from the schedule a train should be considered delayed. Delayed trains are marked red in the real-time display. (Default: 500, 30 in-game minutes)")
            .defineInRange("general.deviation_threshold", 500, 1, MAX_TRANSFER_TIME);
        REALTIME_EARLY_ARRIVAL_THRESHOLD = BUILDER.comment("If a train departs earlier from the scheduled time than specified here, no real-time data will be displayed. Trains that depart earlier than the scheduled departure time minus the minimum transfer time are intentionally \"missed\" in order to continue to ensure the connection. (Default: 500, 30 in-game minutes)")
            .defineInRange("general.realtime_early_arrival_threshold", 500, 1, MAX_TRANSFER_TIME);
        NEXT_STOP_ANNOUNCEMENT = BUILDER.comment("The next stop or information about the start of the journey is announced in the specified number of ticks before the scheduled arrival at the next station. (Default: 500, 30 real life seconds)")
            .defineInRange("general.next_stop_announcement", 500, 100, 1000);
        REALTIME_PRECISION_THRESHOLD = BUILDER.comment("This value (in ticks) indicates how accurately the real-time data should be displayed. By default, only deviations over 10 in-game minutes (167 ticks, approx. 8 real life seconds) are displayed. The lower the value, the more accurate the real-time data but also the more often deviations from the schedule occur. (Default: 167, 10 in-game minutes)")
            .defineInRange("general.realtime_precision_threshold", 167, 1, 1000);
        DISPLAY_LEAD_TIME = BUILDER.comment("This value indicates when departing trains should be displayed on advanced displays. By default 1000 ticks (1 in-game hour, 50 real life seconds) before departure is imminent.")
            .defineInRange("general.display_lead_time", 1000, 200, MAX_TRANSFER_TIME);
        OVERLAY_SCALE = BUILDER.comment("Scale of the route overlay UI. (Default: 0.75)")
            .defineInRange("route_overlay.scale", 0.75f, MIN_SCALE, MAX_SCALE);
        ROUTE_NARRATOR = BUILDER.comment("If active, events during the journey (e.g. the next stop) are announced. (Default: OFF)")
            .define("route_overlay.narrator", false);
        ROUTE_NOTIFICATIONS = BUILDER.comment("If active, you will receive short toasts about important events on your trip, e.g. delays, changes, ... (Default: ON)")
            .define("route_overlay.notifications", true);
        ROUTE_OVERLAY_POSITION = BUILDER.comment("The position on your screen where you want the overlay to appear. (Default: Top Left)")
            .defineEnum("route_overlay.position", OverlayPosition.TOP_LEFT);
        TRANSFER_TIME = BUILDER.comment("Specifies the minimum amount of time (in ticks) that must be available for changing the train. Only trains that depart later than the specified value at the transfer station will be selected. (Default: 1000, 1 in-game hour, approx. 50 real life seconds)")
            .defineInRange("search_settings.transfer_time", 1000, 0, MAX_TRANSFER_TIME);
        TRAIN_GROUP_FILTER_BLACKLIST = BUILDER.comment("List of train groups that should NOT be used in navigation. (Default: <empty>)")
            .defineList("search_settings.train_group_blacklist", new ArrayList<String>(), x -> x instanceof String);
        
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
        TRANSFER_TIME.set(1000);
        TRAIN_GROUP_FILTER_BLACKLIST.set(List.of());
    }
}
