package de.mrjulsen.crn.data;

import de.mrjulsen.crn.CreateRailwaysNavigator;

public interface ITranslatableEnum {
    String getNameOfEnum();
    String getValue();

    default String getTranslationKey() {
        return String.format("gui.%s.%s.%s", CreateRailwaysNavigator.MOD_ID, this.getNameOfEnum(), this.getValue());
    }
    default String getDescriptionTranslationKey() {
        return String.format("gui.%s.%s.description", CreateRailwaysNavigator.MOD_ID, this.getNameOfEnum());
    }
    default String getInfoTranslationKey() {
        return String.format("gui.%s.%s.info.%s", CreateRailwaysNavigator.MOD_ID, this.getNameOfEnum(), this.getValue());
    }
}
