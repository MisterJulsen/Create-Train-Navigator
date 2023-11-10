package de.mrjulsen.crn.data;

import de.mrjulsen.crn.config.ModClientConfig;
import net.minecraft.nbt.CompoundTag;

public class UserSettings {

    private static final String NBT_FILTER_CRITERIA = "FilterCriteria";
    private static final String NBT_RESULT_TYPE = "ResultType";
    private static final String NBT_RESULT_COUNT = "ResultCount";
    private static final String NBT_NEXT_TRAIN = "TakeNextTrain";

    private final EFilterCriteria filterCriteria;
    private final EResultCount resultType;
    private final int resultCount;
    private final boolean takeNextDepartingTrain;

    public UserSettings() {
        this(ModClientConfig.FILTER_CRITERIA.get(), ModClientConfig.RESULT_RANGE.get(), ModClientConfig.RESULT_AMOUNT.get(), ModClientConfig.TAKE_NEXT_DEPARTING_TRAIN.get());
    }

    private UserSettings(EFilterCriteria filterCriteria, EResultCount resultType, int resultCount, boolean takeNextDepartingTrain) {
        this.filterCriteria = filterCriteria;
        this.resultType = resultType;
        this.resultCount = resultCount;
        this.takeNextDepartingTrain = takeNextDepartingTrain;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt(NBT_FILTER_CRITERIA, getFilterCriteria().getId());
        nbt.putInt(NBT_RESULT_TYPE, getResultType().getId());
        nbt.putInt(NBT_RESULT_COUNT, getResultCount());
        nbt.putBoolean(NBT_NEXT_TRAIN, shouldOnlyTakeNextDepartingTrain());
        return nbt;
    }

    public static UserSettings fromNbt(CompoundTag nbt) {
        return new UserSettings(
            EFilterCriteria.getCriteriaById(nbt.getInt(NBT_FILTER_CRITERIA)),
            EResultCount.getCriteriaById(nbt.getInt(NBT_RESULT_TYPE)),
            nbt.getInt(NBT_RESULT_COUNT),
            nbt.getBoolean(NBT_NEXT_TRAIN)
        );
    }

    public EFilterCriteria getFilterCriteria() {
        return filterCriteria;
    }

    public EResultCount getResultType() {
        return resultType;
    }

    public int getResultCount() {
        return resultCount;
    }

    public boolean shouldOnlyTakeNextDepartingTrain() {
        return takeNextDepartingTrain;
    }
}
