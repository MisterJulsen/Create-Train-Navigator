package de.mrjulsen.crn.data;

import de.mrjulsen.crn.ModMain;

public interface ITranslatableEnum {
    String getNameOfEnum();
    String getValue();

    default String getTranslationKey() {
        return String.format("gui.%s.%s.%s", ModMain.MOD_ID, this.getNameOfEnum(), this.getValue());
    }
    default String getDescriptionTranslationKey() {
        return String.format("gui.%s.%s.description", ModMain.MOD_ID, this.getNameOfEnum());
    }
    default String getInfoTranslationKey() {
        return String.format("gui.%s.%s.info.%s", ModMain.MOD_ID, this.getNameOfEnum(), this.getValue());
    }
}
