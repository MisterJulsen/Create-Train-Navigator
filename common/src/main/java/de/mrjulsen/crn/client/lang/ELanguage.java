package de.mrjulsen.crn.client.lang;

import java.util.Arrays;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringRepresentable;

public enum ELanguage implements StringRepresentable {
    DEFAULT("defaut", "def"),
    ENGLISH("english", "en_us"),
    GERMAN("german", "de_de"),
    DUTCH("dutch", "nl_nl"),
    POLISH("polish", "pl_pl"),
    CHINESE_SIMPLIFIED("chinese_simplified", "zh_cn"),
    SAXON("saxon", "sxu"),
    BAVARIAN("bavarian", "bar"),
    SPANISH("spanish", "es_es"),
    RUSSIAN("russian", "ru_ru"),
    FRENCH("french", "fr_fr"),
    KOREAN("korean", "ko_kr"),
    SWEDISH("swedish", "sv_se"),
    PORTUGUESE("portuguese", "pt_pt");

    private String name;
    private String code;


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

    public static MutableComponent translate(String key) {
        if (Platform.getEnvironment() == Env.CLIENT) {
            MutableComponent comp = MutableComponent.create(new ModTranslatableComponent(key));
            if (comp.getString().equals(key)) {
                return TextUtils.translate(key);
            }
            return comp;
        } else {
            return TextUtils.translate(key);
        }
    }

    public static MutableComponent translate(String key, Object... args) {
        if (Platform.getEnvironment() == Env.CLIENT) {
            MutableComponent comp = MutableComponent.create(new ModTranslatableComponent(key, args));
            if (comp.getString().equals(key)) {
                return TextUtils.translate(key, args);
            }
            return comp;
        } else {
            return TextUtils.translate(key, args);
        }
    }
    
    @Override
    public String getSerializedName() {
        return code;
    }
    
}
