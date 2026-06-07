package de.mrjulsen.crn.config;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ModCommonConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.ConfigValue<Integer> REALTIME_PRECISION_THRESHOLD;
    public static final ModConfigSpec.ConfigValue<Integer> NEXT_STOP_ANNOUNCEMENT;
    public static final ModConfigSpec.ConfigValue<Integer> DISPLAY_LEAD_TIME;
    public static final ModConfigSpec.ConfigValue<Integer> GLOBAL_SETTINGS_PERMISSION_LEVEL;
    public static final ModConfigSpec.ConfigValue<Integer> GLOBAL_SETTINGS_ADMIN_PERMISSION_LEVEL;
    public static final ModConfigSpec.ConfigValue<Integer> TOTAL_DURATION_BUFFER_SIZE;
    public static final ModConfigSpec.ConfigValue<Integer> SCHEDULE_DEVIATION_THRESHOLD;
    public static final ModConfigSpec.ConfigValue<Integer> AUTO_RESET_TIMINGS;
    public static final ModConfigSpec.ConfigValue<Integer> TRANSFER_COST;
    public static final ModConfigSpec.ConfigValue<Integer> TOTAL_DURATION_DEVIATION_THRESHOLD;
    public static final ModConfigSpec.ConfigValue<Boolean> CUSTOM_TRANSIT_TIME_CALCULATION;
    public static final ModConfigSpec.ConfigValue<Boolean> USE_CREATE_TRANSIT_TIMES_ON_INIT;
    public static final ModConfigSpec.ConfigValue<Boolean> EXCLUDE_TRAINS;
    public static final ModConfigSpec.ConfigValue<Boolean> AUTO_UPDATE_DISPLAY_TYPE;
    public static final ModConfigSpec.ConfigValue<Boolean> ADVANCED_LOGGING;

    public static final ModConfigSpec.ConfigValue<Boolean> EXPERIMENT_SIMULATION_ALGORITHM;

    public static final ModConfigSpec.ConfigValue<Boolean> WEB_API_ENABLED;
    public static final ModConfigSpec.ConfigValue<Integer> WEB_API_PORT;
    public static final ModConfigSpec.ConfigValue<String> WEB_API_BIND_ADDRESS;
    public static final ModConfigSpec.ConfigValue<Integer> WEB_API_MAX_EVENTS;

    static {
        BUILDER.push(CreateRailwaysNavigator.MOD_ID + "_common_config");

        NEXT_STOP_ANNOUNCEMENT = BUILDER.comment(new String[] {"[in Ticks]", "The next stop or information about the start of the journey is announced in the specified number of ticks before the scheduled arrival at the next station. (Default: 600, 30 real life seconds)"})
                .defineInRange("general.next_stop_announcement", 600, 100, 1000);
        REALTIME_PRECISION_THRESHOLD = BUILDER.comment(new String[] {"[in Ticks]", "This value indicates how accurately the real-time data should be displayed. By default, only deviations above 10 in-game minutes (167 ticks, approx. 8 real life seconds) are displayed. The lower the value, the more accurate the real-time data but also the more often deviations from the schedule occur. (Default: 167, 10 in-game minutes)"})
                .defineInRange("general.realtime_precision_threshold", 167, 1, 1000);
        DISPLAY_LEAD_TIME = BUILDER.comment(new String[] {"[in Ticks]", "How early a train should be shown on the display. (Default: 1200, 1 real life minute)"})
                .defineInRange("general.display_lead_time", 1200, 100, 24000);


        GLOBAL_SETTINGS_PERMISSION_LEVEL = BUILDER.comment("Minimum permission level required to edit the global navigator settings. 0 allows everyone to edit these settings. (Default: 0)")
            .defineInRange("permissions.global_settings_permission_level", 0, 0, 4);
        GLOBAL_SETTINGS_ADMIN_PERMISSION_LEVEL = BUILDER.comment("Minimum permission level required to use admin features in CRN. 0 gives everybody admin permissions in CRN (not recommended), -1 disables admin features. (Default: 3)")
            .defineInRange("permissions.admin_mode_permission_level", 3, -1, 4);
        


        EXCLUDE_TRAINS = BUILDER.comment("If activated, used trains are excluded from the route search for all following route parts. This prevents the same train from being suggested multiple times in the same route and forces the navigator to use other trains instead. Normally, however, there are no problems, so this option can be left off if in doubt. (Default: OFF)")
            .define("navigation.exclude_trains", false);
        TRANSFER_COST = BUILDER.comment("How much transfers should be avoided. Higher values try to use fewer transfers, even if this increases the travel time. (Default: 10000)")
            .defineInRange("navigation.transfer_cost", 10000, 1000, Integer.MAX_VALUE);


        CUSTOM_TRANSIT_TIME_CALCULATION = BUILDER.comment("When activated, CRN calculates the transit times of the trains and does not use the calculations from Create. CRN is much more accurate, while Create calculates an average. (Default: ON)")
            .define("train_data_calculation.custom_transit_time_calculation", true);
        USE_CREATE_TRANSIT_TIMES_ON_INIT = BUILDER.comment("When activated, CRN uses the transit times provided by Create (if available) when initializing. When turned off, the initialization may take longer. (Default: ON)")
            .define("train_data_calculation.use_create_transit_times_on_init", true);
        TOTAL_DURATION_BUFFER_SIZE = BUILDER.comment(new String[] {"[in Cycles]", "How often the calculated time for a route section between two stations must deviate from the current reference value before the reference value is updated. (Default: 3)"})
            .defineInRange("train_data_calculation.total_duration_deviation_buffer_size", 3, 1, 16);            
        TOTAL_DURATION_DEVIATION_THRESHOLD = BUILDER.comment(new String[] {"[in Ticks]", "Deviations of the calculated time for a route section between two stations from the reference value that are smaller than the threshold value are not taken into account. (Default: 50)"})
            .defineInRange("train_data_calculation.total_duration_deviation_threshold", 50, 0, 1000);
        SCHEDULE_DEVIATION_THRESHOLD = BUILDER.comment(new String[] {"[in Ticks]", "How many ticks the real-time can deviate from the scheduled time before the train is considered delayed. (Default: 500)"})
            .defineInRange("train_data_calculation.schedule_deviation_threshold", 500, 100, 24000);
        AUTO_RESET_TIMINGS = BUILDER.comment(new String[] {"[In Cycles]", "(ONLY WORKS FOR TRAINS WITH DYNAMIC DELAYS! Trains without dynamic delays do this every new schedule section by default.)", " ", "Every X cycles the scheduled times are updated to the current real-time data. (Default: 2; Disabled: 0)"})
            .defineInRange("train_data_calculation.auto_reset_timings", 2, 0, Integer.MAX_VALUE);

        ADVANCED_LOGGING = BUILDER.comment(new String[] {"Prints more details to the console to better observe the behavior of CRN. Only relevant for debugging."})
            .define("debug.advanced_logging", false);

        EXPERIMENT_SIMULATION_ALGORITHM = BUILDER.comment("The new simulation algorithm calculates the times step by step for the respective stations and pays attention to the schedule conditions instead of adding up the total journey time until the desired time is reached. This should lead to more precise results, especially when using time based departure times. (Default: OFF)")
            .define("experimental.use_new_simulation_algorithm", false);

        AUTO_UPDATE_DISPLAY_TYPE = BUILDER.comment("Automatically changes the display type when a display link is connected, depending on what should be displayed. (Default: ON)")
                .define("advanced_display.auto_change_display_type", true);

        BUILDER.push("web_api");
        WEB_API_ENABLED = BUILDER.comment("Expose a JSON HTTP API that updates when trains arrive at or leave stations. Bind address defaults to localhost for safety. (Default: OFF)")
            .define("enabled", false);
        WEB_API_PORT = BUILDER.comment("TCP port for the station-stops web API. (Default: 8765)")
            .defineInRange("port", 8765, 1024, 65535);
        WEB_API_BIND_ADDRESS = BUILDER.comment("Address to bind the web API to. Use 0.0.0.0 only if you trust your network. (Default: 127.0.0.1)")
            .define("bind_address", "127.0.0.1");
        WEB_API_MAX_EVENTS = BUILDER.comment("Maximum number of recent arrival/departure events kept in memory for polling and SSE replay. (Default: 256)")
            .defineInRange("max_events", 256, 32, 4096);
        BUILDER.pop();

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
