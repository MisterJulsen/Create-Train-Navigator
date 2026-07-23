package de.mrjulsen.crn.data.schedule.condition;

import java.util.Arrays;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.mcdragonlib.data.ITranslatableEnum;

/**
 * Which trains a {@link TrainSeparationCondition} counts when it looks up the last departure from a
 * station: any train, or only one sharing the current train's line, category or name.
 */
public enum ETrainFilter implements ITranslatableEnum {
    ANY((byte)0, "any"),
    SAME_LINE((byte)1, "same_line"),
    SAME_CATEGORY((byte)2, "same_category"),
    SAME_NAME((byte)3, "same_name");

    private final byte index;
    private final String name;

    private ETrainFilter(byte index, String name) {
        this.index = index;
        this.name = name;
    }

    public byte getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public static ETrainFilter getByIndex(byte i) {
        return Arrays.stream(values()).filter(x -> x.getIndex() == i).findFirst().orElse(ANY);
    }

    @Override
    public Data getTranslationData() {
        return new Data(CreateRailwaysNavigator.MOD_ID, "train_filter", name);
    }
}
