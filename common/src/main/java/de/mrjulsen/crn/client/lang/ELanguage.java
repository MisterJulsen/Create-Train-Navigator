package de.mrjulsen.crn.client.lang;

import java.util.Arrays;
import java.util.List;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringRepresentable;

public enum ELanguage implements StringRepresentable {
    DEFAULT("defaut", "def"),
    ENGLISH("english", "en_us"),
    GERMAN("german", "de_de"),
    DUTCH("dutch", "nl_nl"),
    POLISH("polish", "pl_pl");

    private String name;
    private String code;

    private static ELanguage currentLanguage;
    private static ClientLanguage currentClientLanguage;

    private ELanguage(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public static ELanguage getByCode(String code) {
        return Arrays.stream(values()).filter(x -> x.getCode().equals(code)).findFirst().orElse(DEFAULT);
    }

    public static void updateLanguage(ELanguage lang) {
        if (currentLanguage == lang) {
            return;
        }

        LanguageInfo info = lang == DEFAULT ? null : Minecraft.getInstance().getLanguageManager().getLanguage(lang.getCode());
        if (info == null) {
            info = Minecraft.getInstance().getLanguageManager().getSelected();
        }
        currentLanguage = lang;
        currentClientLanguage = ClientLanguage.loadFrom(Minecraft.getInstance().getResourceManager(), List.of(info));
        CreateRailwaysNavigator.LOGGER.info("Updated custom language to: " + (info == null ? null : info.getName()));
    }

    public static ClientLanguage getCurrentClientLanguage() {
        return currentClientLanguage == null ? (ClientLanguage)Language.getInstance() : currentClientLanguage;
    }

    public static MutableComponent translate(String key) {
        return new ModTranslatableComponent(key);
    }

    public static MutableComponent translate(String key, Object... args) {
        return new ModTranslatableComponent(key, args);
    }


    
    @Override
    public String getSerializedName() {
        return code;
    }
    
}
