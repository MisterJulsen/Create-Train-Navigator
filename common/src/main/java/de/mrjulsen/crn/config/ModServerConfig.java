package de.mrjulsen.crn.config;

import java.util.List;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import net.minecraftforge.common.ForgeConfigSpec;

public class ModServerConfig {
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
    public static final ForgeConfigSpec.ConfigValue<Integer> TOTAL_DURATION_DEVIATION_THRESHOLD;
    public static final ForgeConfigSpec.ConfigValue<Boolean> SCHEDULE_INCLUDES_WAITING;

    static {
        BUILDER.push(CreateRailwaysNavigator.MOD_ID + "_common_config");

        NEXT_STOP_ANNOUNCEMENT = BUILDER.comment("[in Ticks]", "The next stop or information about the start of the journey is announced in the specified number of ticks before the scheduled arrival at the next station. (Default: 600, 30 real life seconds)")
                .defineInRange("general.next_stop_announcement", 600, 100, 1000);
        REALTIME_PRECISION_THRESHOLD = BUILDER.comment("[in Ticks]", "This value indicates how accurately the real-time data should be displayed. By default, only deviations above 10 in-game minutes (167 ticks, approx. 8 real life seconds) are displayed. The lower the value, the more accurate the real-time data but also the more often deviations from the schedule occur. (Default: 167, 10 in-game minutes)")
                .defineInRange("general.realtime_precision_threshold", 167, 1, 1000);
        DISPLAY_LEAD_TIME = BUILDER.comment("[in Ticks]", "How early a train should be shown on the display. (Default: 1200, 1 real life minute)")
                .defineInRange("general.display_lead_time", 1200, 100, 24000);


        GLOBAL_SETTINGS_PERMISSION_LEVEL = BUILDER.comment("Minimum permission level required to edit the global navigator settings. 0 allows everyone to edit these settings. (Default: 0)")
            .defineInRange("permissions.global_settings_permission_level", 0, 0, 4);
        GLOBAL_SETTINGS_ADMIN_PERMISSION_LEVEL = BUILDER.comment("Minimum permission level required to use admin features in CRN. 0 gives everybody admin permissions in CRN (not recommended), -1 disables admin features. (Default: 3)")
            .defineInRange("permissions.admin_mode_permission_level", 3, -1, 4);
        

        TOTAL_DURATION_BUFFER_SIZE = BUILDER.comment("[in Cycles]", "How often the calculated time for a route section between two stations must deviate from the current reference value before the reference value is updated. (Default: 3)")
            .defineInRange("train_data_calculation.total_duration_deviation_buffer_size", 3, 1, 16);            
        TOTAL_DURATION_DEVIATION_THRESHOLD = BUILDER.comment("[in Ticks]", "Deviations of the calculated time for a route section between two stations from the reference value that are smaller than the threshold value are not taken into account. (Default: 50)")
            .defineInRange("train_data_calculation.total_duration_deviation_threshold", 50, 0, 1000);
        SCHEDULE_DEVIATION_THRESHOLD = BUILDER.comment("[in Ticks]", "How many ticks the real-time can deviate from the scheduled time before the train is considered delayed. (Default: 500)")
            .defineInRange("train_data_calculation.schedule_deviation_threshold", 500, 100, 24000);
        SCHEDULE_INCLUDES_WAITING = BUILDER.comment("If activated, the schedule also stores the typical total waiting time of each route section (signals, other trains) in addition to the pure driving time.", "When off, the schedule is the ideal unobstructed run and any waiting at all shows up as a delay. (Default: ON)")
            .define("train_data_calculation.schedule_includes_waiting", true);
        AUTO_RESET_TIMINGS = BUILDER.comment("[in Cycles]", "(ONLY WORKS FOR TRAINS WITH DYNAMIC DELAYS! Trains without dynamic delays do this every new schedule section by default.)", " ", "Every X cycles the scheduled times are updated to the current real-time data. (Default: 2; Disabled: 0)")
            .defineInRange("train_data_calculation.auto_reset_timings", 2, 0, Integer.MAX_VALUE);

        DISRUPTION_DISPLAY_DURATION = BUILDER.comment("[in Ticks]", "How long a train whose schedule was removed keeps being shown as cancelled (Default: 1200, 1 real life minute; Unlimited: -1; Never shown: 0)")
            .defineInRange("disruptions.display_duration", 1200, -1, Integer.MAX_VALUE);
        DISRUPTION_DISPLAY_DURATION_PAUSED = BUILDER.comment("[in Ticks]", "How long a train whose schedule was paused keeps being shown as cancelled. (Default: 3600, 3 real life minutes; Unlimited: -1; Never shown: 0)")
            .defineInRange("disruptions.display_duration_schedule_paused", 3600, -1, Integer.MAX_VALUE);
        DISRUPTION_DISPLAY_DURATION_DERAILED = BUILDER.comment("[in Ticks]", "How long a derailed train keeps being shown as cancelled. (Default: -1, unlimited; Never shown: 0)")
            .defineInRange("disruptions.display_duration_derailed", -1, -1, Integer.MAX_VALUE);


        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
