package de.mrjulsen.crn.config;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import net.minecraftforge.common.ForgeConfigSpec;

public class ModCommonConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<Integer> GLOBAL_SETTINGS_PERMISSION_LEVEL;
    public static final ForgeConfigSpec.ConfigValue<Integer> TRAIN_WATCHER_INTERVALL;

    static {
        BUILDER.push(CreateRailwaysNavigator.MOD_ID + "_common_config");
        
        TRAIN_WATCHER_INTERVALL = BUILDER.comment("The time interval (in ticks) in which the Train Listener collects tick data of all trains. Higher values may result in less precise time information in the route details view, while lower values may result in higher CPU usage. Default: 100 (5s)")
            .defineInRange("train_watcher_intervall", 100, 100, 1000);

        GLOBAL_SETTINGS_PERMISSION_LEVEL = BUILDER.comment("Minimum permission level required to edit the global navigator settings. 0 allows everyone to edit these settings.")
            .defineInRange("global_settings_permission_level", 0, 0, 4);
        
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
