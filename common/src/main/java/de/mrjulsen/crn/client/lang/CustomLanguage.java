package de.mrjulsen.crn.client.lang;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.network.chat.MutableComponent;

public class CustomLanguage {

    public static final String DEFAULT = "";

    private final String code;
    private final Supplier<LanguageInfo> info;

    public CustomLanguage(String code) {
        this.code = code;
        this.info = Suppliers.memoize(() -> {
            try {
                return Minecraft.getInstance().getLanguageManager().getLanguage(code);
            } catch (Exception e) {
                return null;
            }
        });
    }

    public Optional<LanguageInfo> getLanguageInfo() {
        return Optional.ofNullable(info.get());
    }

    public String getCode() {
        return code;
    }

    public boolean isDefault() {
        return DEFAULT.equals(code);
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

    @Override
    public String toString() {
        return super.toString();
    }
}
