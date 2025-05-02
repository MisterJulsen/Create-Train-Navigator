package de.mrjulsen.crn.util;

import java.util.Arrays;

import de.mrjulsen.mcdragonlib.core.IIterableEnum;
import de.mrjulsen.mcdragonlib.core.ITranslatableEnum;

public enum EDepartureBoardTrainFilter implements ITranslatableEnum, IIterableEnum<EDepartureBoardTrainFilter> {
    ARRIVAL_AND_DEPARTURE((byte)0, "arrival_and_departure"),
    ARRIVAL_ONLY((byte)1, "arrival_only"),
    DEPARTURE_ONLY((byte)2, "departure_only");

    private final byte index;
    private final String name;

    private EDepartureBoardTrainFilter(byte index, String name) {
        this.index = index;
        this.name = name;
    }

    public byte getIndex() {
        return index;
    }
    
    public String getName() {
        return name;
    }

    public static EDepartureBoardTrainFilter getByIndex(int index) {
        return Arrays.stream(values()).filter(x -> x.getIndex() == index).findFirst().orElse(ARRIVAL_AND_DEPARTURE);
    }

    @Override
    public String getEnumName() {
        return "departure_board_train_filter";
    }

    @Override
    public String getEnumValueName() {
        return getName();
    }

    @Override
    public EDepartureBoardTrainFilter[] getValues() {
        return values();
    }
}
