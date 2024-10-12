package de.mrjulsen.crn.config;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import net.minecraftforge.common.ForgeConfigSpec;

public class ModCommonConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<Integer> GLOBAL_SETTINGS_PERMISSION_LEVEL;
    public static final ForgeConfigSpec.ConfigValue<Integer> TOTAL_DURATION_BUFFER_SIZE;
    public static final ForgeConfigSpec.ConfigValue<Integer> SCHEDULE_DEVIATION_THRESHOLD;
    public static final ForgeConfigSpec.ConfigValue<Integer> AUTO_RESET_TIMINGS;
    public static final ForgeConfigSpec.ConfigValue<Integer> TRANSFER_COST;
    public static final ForgeConfigSpec.ConfigValue<Integer> TOTAL_DURATION_DEVIATION_THRESHOLD;
    public static final ForgeConfigSpec.ConfigValue<Boolean> CUSTOM_TRANSIT_TIME_CALCULATION;
    public static final ForgeConfigSpec.ConfigValue<Boolean> EXCLUDE_TRAINS;

    static {
        BUILDER.push(CreateRailwaysNavigator.MOD_ID + "_common_config");

        GLOBAL_SETTINGS_PERMISSION_LEVEL = BUILDER.comment("Minimum permission level required to edit the global navigator settings. 0 allows everyone to edit these settings. (Default: 0)")
            .defineInRange("permissions.global_settings_permission_level", 0, 0, 4);
        


        EXCLUDE_TRAINS = BUILDER.comment("If activated, used trains are excluded from the route search for all following route parts. This prevents the same train from being suggested multiple times in the same route and forces the navigator to use other trains instead. Normally, however, there are no problems, so this option can be left off if in doubt. (Default: OFF)")
            .define("navigation.exclude_trains", false);
        TRANSFER_COST = BUILDER.comment("How much transfers should be avoided. Higher values try to use fewer transfers, even if this increases the travel time. (Default: 5000)")
            .defineInRange("navigation.transfer_cost", 10000, 1000, Integer.MAX_VALUE);


        CUSTOM_TRANSIT_TIME_CALCULATION = BUILDER.comment("When activated, CRN calculates the transit times of the trains and does not use the calculations from Create. CRN is much more accurate, while Create calculates an average. (Default: ON)")
            .define("train_data_calculation.custom_transit_time_calculation", true);
        TOTAL_DURATION_BUFFER_SIZE = BUILDER.comment(new String[] {"[in Cycles]", "How often the calculated time for a route section between two stations must deviate from the current reference value before the reference value is updated. (Default: 3)"})
            .defineInRange("train_data_calculation.total_duration_deviation_buffer_size", 3, 1, 16);            
        TOTAL_DURATION_DEVIATION_THRESHOLD = BUILDER.comment(new String[] {"[in Ticks]", "Deviations of the calculated time for a route section between two stations from the reference value that are smaller than the threshold value are not taken into account. (Default: 50)"})
            .defineInRange("train_data_calculation.total_duration_deviation_threshold", 50, 0, 1000);
        SCHEDULE_DEVIATION_THRESHOLD = BUILDER.comment(new String[] {"[in Ticks]", "How many ticks the real-time can deviate from the scheduled time before the train is considered delayed. (Default: 500)"})
            .defineInRange("train_data_calculation.schedule_deviation_threshold", 500, 100, 24000);
        AUTO_RESET_TIMINGS = BUILDER.comment(new String[] {"[In Cycles]", "(ONLY WORKS FOR TRAINS WITH DYNAMIC DELAYS! Trains without dynamic delays do this every new schedule section by default.)", " ", "Every X cycles the scheduled times are updated to the current real-time data. (Default: 2; Disabled: 0)"})
            .defineInRange("train_data_calculation.auto_reset_timings", 2, 0, Integer.MAX_VALUE);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
