package de.mrjulsen.crn.data;

import java.util.Arrays;
import de.mrjulsen.mcdragonlib.core.ITranslatableEnum;

public enum ETimeSource implements ITranslatableEnum {
    REAL_LIFE((byte)0, "real_life"),
    IN_GAME((byte)1, "in_game");

    final byte index;
    final String name;

    ETimeSource(byte index, String name) {
        this.index = index;
        this.name = name;
    }

    public byte getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public static ETimeSource getByIndex(int index) {
        return Arrays.stream(values()).filter(x -> x.getIndex() == index).findFirst().orElse(REAL_LIFE);
    }

    @Override
    public String getEnumName() {
        return "time_source";
    }

    @Override
    public String getEnumValueName() {
        return name;
    }
}
