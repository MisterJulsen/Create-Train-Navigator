package de.mrjulsen.crn.config;

import de.mrjulsen.crn.data.EFilterCriteria;
import de.mrjulsen.crn.data.EResultCount;
import net.minecraftforge.common.ForgeConfigSpec;

public class ModClientConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    
    public static final ForgeConfigSpec.ConfigValue<EFilterCriteria> FILTER_CRITERIA;
    public static final ForgeConfigSpec.ConfigValue<EResultCount> RESULT_RANGE;
    public static final ForgeConfigSpec.ConfigValue<Integer> RESULT_AMOUNT;
    public static final ForgeConfigSpec.ConfigValue<Boolean> TAKE_NEXT_DEPARTING_TRAIN;
    public static final ForgeConfigSpec.ConfigValue<Integer> TRANSFER_TIME;

    public static final int MAX_TRANSFER_TIME = 24000;

    static {
        BUILDER.push("Create Railways Navigator Config");

        /* CONFIGS */     
        FILTER_CRITERIA = BUILDER.comment("The criterion by which the results are sorted.")
            .defineEnum("filter_criteria", EFilterCriteria.TRANSFER_COUNT);
        RESULT_RANGE = BUILDER.comment("The set of results that will be displayed. Can be a fixed number or automatic.")
            .defineEnum("result_range", EResultCount.BEST);
        RESULT_AMOUNT = BUILDER.comment("The amount of results being displayed. Takes only effect when 'Result Range' is set to 'Fixed Amount'")
            .defineInRange("result_amount", 10, 1, 999);
        TAKE_NEXT_DEPARTING_TRAIN = BUILDER.comment("If true, the navigator will take the next departing train at the starting station. The route may be inefficient.")
            .define("take_next_departing_train", false);
        TRANSFER_TIME = BUILDER.comment("Minimum transfer duration. (Ticks)'")
            .defineInRange("transfer_time", 1000, 0, MAX_TRANSFER_TIME);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static void reset() {
        FILTER_CRITERIA.set(EFilterCriteria.TRANSFER_COUNT);
        RESULT_RANGE.set(EResultCount.BEST);
        RESULT_AMOUNT.set(10);
        TAKE_NEXT_DEPARTING_TRAIN.set(false);
    }
}
