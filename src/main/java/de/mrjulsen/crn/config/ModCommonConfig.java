package de.mrjulsen.crn.config;

import de.mrjulsen.crn.ModMain;
import net.minecraftforge.common.ForgeConfigSpec;

public class ModCommonConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<Integer> GLOBAL_SETTINGS_PERMISSION_LEVEL;
    public static final ForgeConfigSpec.ConfigValue<Integer> TRAIN_LISTENER_INTERVALL;
    public static final ForgeConfigSpec.ConfigValue<Integer> NAVIGATION_ITERATION_DELAY;
    public static final ForgeConfigSpec.ConfigValue<Boolean> PARALLEL_NAVIGATION;

    static {
        BUILDER.push(ModMain.MOD_ID + "_common_config");
        
        TRAIN_LISTENER_INTERVALL = BUILDER.comment("The time interval (in ms) in which the Train Listener collects tick data of all trains. Higher values may result in less precise time information in the route details view, while lower values may result in higher CPU usage. Default: 1000ms (1s)")
            .defineInRange("train_listener_intervall", 1000, 10, 60000);

        GLOBAL_SETTINGS_PERMISSION_LEVEL = BUILDER.comment("Minimum permission level required to edit the global navigator settings. 0 allows everyone to edit these settings.")
            .defineInRange("global_settings_permission_level", 0, 0, 4);
        
        NAVIGATION_ITERATION_DELAY = BUILDER.comment("Delay in milliseconds between each iteration on the navigation thread. Higher delays might improve general server performance (especially on servers with low CPU power), but also increases time needed for finding routes. Default: 0ms (fastest)")
            .defineInRange("navigation_iteration_delay", 0, 0, 100);

        PARALLEL_NAVIGATION = BUILDER.comment("Navigates in parallel for all trains departing from the starting station. Uses all the CPU power available on your system. Default: false")
            .define("parallel_navigation", false);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
