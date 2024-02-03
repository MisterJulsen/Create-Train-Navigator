package de.mrjulsen.crn.config;

import java.util.ArrayList;
import java.util.List;

import net.minecraftforge.common.ForgeConfigSpec;

public class ModClientConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    
    public static final ForgeConfigSpec.ConfigValue<Integer> TRANSFER_TIME;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> TRAIN_GROUP_FILTER_BLACKLIST;

    public static final int MAX_TRANSFER_TIME = 24000;

    static {
        BUILDER.push("Create Railways Navigator Config");

        /* CONFIGS */     
        TRANSFER_TIME = BUILDER.comment("Minimum transfer duration. (Ticks)'")
            .defineInRange("transfer_time", 1000, 0, MAX_TRANSFER_TIME);
        TRAIN_GROUP_FILTER_BLACKLIST = BUILDER.comment("List of train groups that should not be used in navigation.")
            .defineList("train_group_blacklist", new ArrayList<String>(), x -> x instanceof String);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static void reset() {
        TRANSFER_TIME.set(1000);
        TRAIN_GROUP_FILTER_BLACKLIST.set(List.of());
    }
}
