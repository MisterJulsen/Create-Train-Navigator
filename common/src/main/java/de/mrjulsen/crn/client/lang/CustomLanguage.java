package de.mrjulsen.crn.client.lang;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public class CustomLanguage {

    public static final String DEFAULT = "";
    private static final String LANG_PATH_FORMAT = "lang/%s.json";

    private final String code;
    private final Map<String, String> translations;

    private CustomLanguage(String code, Map<String, String> translations) {
        this.code = code;
        this.translations = translations;
    }

    public static CustomLanguage createDefault() {
        return new CustomLanguage(DEFAULT, Map.of());
    }

    public static CustomLanguage load(String code) {
        if (code == null || DEFAULT.equals(code)) {
            return createDefault();
        }

        Map<String, String> map = new HashMap<>();
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        String path = String.format(Locale.ROOT, LANG_PATH_FORMAT, code);

        for (String namespace : resourceManager.getNamespaces()) {
            try {
                ResourceLocation location = new ResourceLocation(namespace, path);
                for (Resource resource : resourceManager.getResourceStack(location)) {
                    try (InputStream stream = resource.open()) {
                        Language.loadFromJson(stream, map::put);
                    } catch (IOException e) {
                        CreateRailwaysNavigator.LOGGER.warn("Failed to load CRN translations for {} from pack {}", code, resource.sourcePackId(), e);
                    }
                }
            } catch (Exception e) {
                CreateRailwaysNavigator.LOGGER.warn("Skipped custom language file: {}:{} ({})", namespace, path, e.toString());
            }
        }
        return new CustomLanguage(code, Map.copyOf(map));
    }

    public String getCode() {
        return code;
    }

    public boolean isDefault() {
        return DEFAULT.equals(code);
    }

    public boolean has(String key) {
        return translations.containsKey(key);
    }

    public String getOrDefault(String key) {
        return translations.getOrDefault(key, key);
    }

    public Optional<LanguageInfo> getLanguageInfo() {
        try {
            return Optional.ofNullable(Minecraft.getInstance().getLanguageManager().getLanguage(code));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static MutableComponent translate(String key) {
        if (Platform.getEnvironment() == Env.CLIENT) {
            return MutableComponent.create(new ModTranslatableComponent(key));
        }
        return TextUtils.translate(key);
    }

    public static MutableComponent translate(String key, Object... args) {
        if (Platform.getEnvironment() == Env.CLIENT) {
            return MutableComponent.create(new ModTranslatableComponent(key, args));
        }
        return TextUtils.translate(key, args);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CustomLanguage o) {
            return code.equals(o.code);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }
}
