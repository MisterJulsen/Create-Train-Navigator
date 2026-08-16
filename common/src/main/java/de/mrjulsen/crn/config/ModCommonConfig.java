package de.mrjulsen.crn.config;

import java.util.List;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import net.minecraftforge.common.ForgeConfigSpec;

public class ModCommonConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<Integer> REALTIME_PRECISION_THRESHOLD;
    public static final ForgeConfigSpec.ConfigValue<Integer> NEXT_STOP_ANNOUNCEMENT;
    public static final ForgeConfigSpec.ConfigValue<Integer> DISPLAY_LEAD_TIME;
    public static final ForgeConfigSpec.ConfigValue<Integer> GLOBAL_SETTINGS_PERMISSION_LEVEL;
    public static final ForgeConfigSpec.ConfigValue<Integer> GLOBAL_SETTINGS_ADMIN_PERMISSION_LEVEL;
    public static final ForgeConfigSpec.ConfigValue<Integer> TOTAL_DURATION_BUFFER_SIZE;
    public static final ForgeConfigSpec.ConfigValue<Integer> SCHEDULE_DEVIATION_THRESHOLD;
    public static final ForgeConfigSpec.ConfigValue<Integer> AUTO_RESET_TIMINGS;
    public static final ForgeConfigSpec.ConfigValue<Integer> DISRUPTION_DISPLAY_DURATION;
    public static final ForgeConfigSpec.ConfigValue<Integer> DISRUPTION_DISPLAY_DURATION_PAUSED;
    public static final ForgeConfigSpec.ConfigValue<Integer> DISRUPTION_DISPLAY_DURATION_DERAILED;
    public static final ForgeConfigSpec.ConfigValue<Integer> TRANSFER_COST;
    public static final ForgeConfigSpec.ConfigValue<Integer> TOTAL_DURATION_DEVIATION_THRESHOLD;
    public static final ForgeConfigSpec.ConfigValue<Boolean> SCHEDULE_INCLUDES_WAITING;
    public static final ForgeConfigSpec.ConfigValue<Boolean> CUSTOM_TRANSIT_TIME_CALCULATION;
    public static final ForgeConfigSpec.ConfigValue<Boolean> USE_CREATE_TRANSIT_TIMES_ON_INIT;
    public static final ForgeConfigSpec.ConfigValue<Boolean> EXCLUDE_TRAINS;
    public static final ForgeConfigSpec.ConfigValue<Boolean> AUTO_UPDATE_DISPLAY_TYPE;
    public static final ForgeConfigSpec.ConfigValue<Boolean> ADVANCED_LOGGING;

    public static final ForgeConfigSpec.ConfigValue<Boolean> EXPERIMENT_SIMULATION_ALGORITHM;

    public static final ForgeConfigSpec.ConfigValue<Boolean> WEB_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> WEB_BIND_ADDRESS;
    public static final ForgeConfigSpec.ConfigValue<Boolean> WEB_HTTP_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<Integer> WEB_HTTP_PORT;
    public static final ForgeConfigSpec.ConfigValue<Boolean> WEB_HTTPS_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<Integer> WEB_HTTPS_PORT;
    public static final ForgeConfigSpec.ConfigValue<String> WEB_KEYSTORE_PATH;
    public static final ForgeConfigSpec.ConfigValue<String> WEB_KEYSTORE_PASSWORD;
    public static final ForgeConfigSpec.ConfigValue<Integer> WEB_THREADS;
    public static final ForgeConfigSpec.ConfigValue<Integer> WEB_MAX_REQUEST_BYTES;
    public static final ForgeConfigSpec.ConfigValue<Boolean> WEB_GZIP_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> WEB_CORS_ORIGINS;

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
        SCHEDULE_INCLUDES_WAITING = BUILDER.comment(new String[] {"When on, the schedule budgets the typical total waiting each route section incurs (signals, congestion) in addition to the pure driving time. A train that only waits as much as usual then counts as on time, and only unusual waiting shows up as a delay - this matches how the schedule behaved before the backend rewrite and is usually what players expect on busy networks.", "When off, the schedule is the ideal unobstructed run and any waiting at all shows up as a delay (more honest, but stricter).", "Either way the live arrival prediction stays based on the physical free-flow time; this only affects what counts as 'on time'. (Default: ON)"})
            .define("train_data_calculation.schedule_includes_waiting", true);
        AUTO_RESET_TIMINGS = BUILDER.comment(new String[] {"[In Cycles]", "(ONLY WORKS FOR TRAINS WITH DYNAMIC DELAYS! Trains without dynamic delays do this every new schedule section by default.)", " ", "Every X cycles the scheduled times are updated to the current real-time data. (Default: 2; Disabled: 0)"})
            .defineInRange("train_data_calculation.auto_reset_timings", 2, 0, Integer.MAX_VALUE);

        DISRUPTION_DISPLAY_DURATION = BUILDER.comment(new String[] {"[in Ticks]", "How long a train whose schedule was removed keeps being shown as cancelled, so travellers learn that it is not running.", "Removing a schedule means a player parked the train on purpose, so afterwards it is dropped from the backend entirely and its recorded data is discarded. It is picked up again from scratch as soon as it runs.", "This is also the default for disruption reasons that do not define a duration of their own. (Default: 1200, 1 real life minute; Unlimited: -1; Never shown: 0)"})
            .defineInRange("disruptions.display_duration", 1200, -1, Integer.MAX_VALUE);
        DISRUPTION_DISPLAY_DURATION_PAUSED = BUILDER.comment(new String[] {"[in Ticks]", "How long a train whose schedule was paused keeps being shown as cancelled. Like a removed schedule this is a deliberate action, so it is short by default and the train is forgotten afterwards.", "Pausing also overrules any other reason: a paused train that has additionally derailed disappears on this schedule, not on the derailment's. (Default: 3600, 3 real life minutes; Unlimited: -1; Never shown: 0)"})
            .defineInRange("disruptions.display_duration_schedule_paused", 3600, -1, Integer.MAX_VALUE);
        DISRUPTION_DISPLAY_DURATION_DERAILED = BUILDER.comment(new String[] {"[in Ticks]", "How long a derailed train keeps being shown as cancelled. A derailment is an accident rather than an intentional shutdown, so by default it stays visible until the train is recovered or removed.", "A derailed train's data is never discarded, even after it stops being displayed - it is what you need to work out what happened. (Default: -1, unlimited; Never shown: 0)"})
            .defineInRange("disruptions.display_duration_derailed", -1, -1, Integer.MAX_VALUE);

        ADVANCED_LOGGING = BUILDER.comment(new String[] {"Prints more details to the console to better observe the behavior of CRN. Only relevant for debugging."})
            .define("debug.advanced_logging", false);

        EXPERIMENT_SIMULATION_ALGORITHM = BUILDER.comment("The new simulation algorithm calculates the times step by step for the respective stations and pays attention to the schedule conditions instead of adding up the total journey time until the desired time is reached. This should lead to more precise results, especially when using time based departure times. (Default: OFF)")
            .define("experimental.use_new_simulation_algorithm", false);

        AUTO_UPDATE_DISPLAY_TYPE = BUILDER.comment("Automatically changes the display type when a display link is connected, depending on what should be displayed. (Default: ON)")
                .define("advanced_display.auto_change_display_type", true);

        WEB_ENABLED = BUILDER.comment(new String[] {"Enables the CRN web API, which opens a separate port for external websites and tools to read train data over HTTP.", "This is a network service, so it stays off unless you deliberately want it. (Default: OFF)"})
            .define("web.enabled", false);
        WEB_BIND_ADDRESS = BUILDER.comment(new String[] {"The network interface the web API listens on.", "'0.0.0.0' accepts connections from anywhere, '127.0.0.1' only from the machine the server runs on, which is what you want in singleplayer or behind a reverse proxy. (Default: 0.0.0.0)"})
            .define("web.bind_address", "0.0.0.0");
        WEB_HTTP_ENABLED = BUILDER.comment("Serves the API over plain HTTP. Traffic is not encrypted, so do not expose this to the internet without a reverse proxy in front of it. (Default: ON)")
            .define("web.http.enabled", true);
        WEB_HTTP_PORT = BUILDER.comment("The port of the plain HTTP listener. It must not be the Minecraft port and must not be used by another program. (Default: 25580)")
            .defineInRange("web.http.port", 25580, 1, 65535);
        WEB_HTTPS_ENABLED = BUILDER.comment("Serves the API over HTTPS. Requires a keystore; see web.https.keystore_path. (Default: OFF)")
            .define("web.https.enabled", false);
        WEB_HTTPS_PORT = BUILDER.comment("The port of the HTTPS listener. (Default: 25585)")
            .defineInRange("web.https.port", 25585, 1, 65535);
        WEB_KEYSTORE_PATH = BUILDER.comment(new String[] {"Path to a PKCS#12 keystore holding the certificate and private key for HTTPS, relative to the game directory.", "Empty means HTTPS stays off even when it is enabled above."})
            .define("web.https.keystore_path", "");
        WEB_KEYSTORE_PASSWORD = BUILDER.comment("Password of the keystore above. Stored in plain text, so keep this config file readable only by the server operator.")
            .define("web.https.keystore_password", "");
        WEB_THREADS = BUILDER.comment("How many requests the web API can process at the same time. Higher values do not make the server faster, they only allow more clients to wait in parallel. (Default: 4)")
            .defineInRange("web.threads", 4, 1, 64);
        WEB_MAX_REQUEST_BYTES = BUILDER.comment("[in Bytes] The largest request body the web API accepts. Anything above is rejected with 413 before it is read into memory. (Default: 65536, 64 KiB)")
            .defineInRange("web.max_request_bytes", 65536, 1024, 8388608);
        WEB_GZIP_ENABLED = BUILDER.comment("Compresses responses with gzip when the client accepts it. JSON compresses very well, so this cuts the size of large responses a lot at a small CPU cost. (Default: ON)")
            .define("web.gzip_enabled", true);
        WEB_CORS_ORIGINS = BUILDER.comment(new String[] {"Which websites hosted elsewhere may read responses from this API in a browser.", "Use the exact origin including the scheme, for example 'https://trains.example.com'. A single '*' allows every website. (Default: none)"})
            .defineList("web.cors_allowed_origins", List.of(), o -> o instanceof String);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
