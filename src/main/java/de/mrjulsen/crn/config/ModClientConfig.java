package de.mrjulsen.crn.config;

import java.util.ArrayList;
import java.util.List;

import net.minecraftforge.common.ForgeConfigSpec;

public class ModClientConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    
    public static final ForgeConfigSpec.ConfigValue<Integer> REALTIME_PRECISION_THRESHOLD;
    public static final ForgeConfigSpec.ConfigValue<Integer> DEVIATION_THRESHOLD;
    public static final ForgeConfigSpec.ConfigValue<Double> OVERLAY_SCALE;
    public static final ForgeConfigSpec.ConfigValue<Integer> TRANSFER_TIME;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> TRAIN_GROUP_FILTER_BLACKLIST;

    public static final int MAX_TRANSFER_TIME = 24000;

    static {
        BUILDER.push("Create Railways Navigator Config");

        /* CONFIGS */
        DEVIATION_THRESHOLD = BUILDER.comment("The value indicates how much deviation from the schedule a train should be considered delayed. Delayed trains are marked red in the real-time display. (Default: 500, ~30 in-game minutes)")
            .defineInRange("general.deviation_threshold", 500, 1, MAX_TRANSFER_TIME);
        REALTIME_PRECISION_THRESHOLD = BUILDER.comment("This value (in ticks) indicates how accurately the real-time data should be displayed. By default, only deviations over 10 in-game minutes (167 ticks, approx. 8 real life seconds) are displayed. The lower the value, the more accurate the real-time data but also the more often deviations from the schedule occur. (Default: 167, ~10 in-game minutes)")
            .defineInRange("general.realtime_precision_threshold", 167, 1, 1000);
        OVERLAY_SCALE = BUILDER.comment("Scale of the route overlay. (Default: 0.75")
            .defineInRange("ui.overlay_scale", 0.75f, 0.25f, 2.0f);
        TRANSFER_TIME = BUILDER.comment("Specifies the minimum amount of time (in ticks) that must be available for changing the train. Only trains that depart later than the specified value at the transfer station will be selected. (Default: 1000, ~1 in-game hour)")
            .defineInRange("search_settings.transfer_time", 1000, 0, MAX_TRANSFER_TIME);
        TRAIN_GROUP_FILTER_BLACKLIST = BUILDER.comment("List of train groups that should NOT be used in navigation. (Default: <empty>)")
            .defineList("search_settings.train_group_blacklist", new ArrayList<String>(), x -> x instanceof String);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static void resetSearchSettings() {
        TRANSFER_TIME.set(1000);
        TRAIN_GROUP_FILTER_BLACKLIST.set(List.of());
    }
}
