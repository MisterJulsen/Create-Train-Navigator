package de.mrjulsen.crn.data;

import java.util.Arrays;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.mcdragonlib.data.ITranslatableEnum;

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
		for (ETimeSource source : values())
			if (source.getIndex() == index) return source;
		return REAL_LIFE;
    }

    @Override
    public Data getTranslationData() {
        return new Data(CreateRailwaysNavigator.MOD_ID, "time_source", name);
    }
}
