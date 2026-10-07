package de.mrjulsen.crn.client.lang;

import java.util.Arrays;
import java.util.Optional;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import de.mrjulsen.crn.client.ClientWrapper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.Entity;

public class ModTranslatableComponent implements ComponentContents {

    private static final Object[] NO_ARGS = new Object[0];

    private final String key;
    private final Object[] args;

    private CustomLanguage resolvedWith;
    private ComponentContents delegate;

    public ModTranslatableComponent(String key) {
        this(key, NO_ARGS);
    }

    public ModTranslatableComponent(String key, Object... args) {
        this.key = key;
        this.args = args;
    }

    private ComponentContents resolve() {
        CustomLanguage language = ClientWrapper.getCurrentLanguage();
        if (language != this.resolvedWith || this.delegate == null) {
            this.resolvedWith = language;
            if (!language.isDefault() && language.has(this.key)) {
                String template = language.getOrDefault(this.key);
                this.delegate = new TranslatableContents(template, template, this.args);
            } else {
                this.delegate = new TranslatableContents(this.key, null, this.args);
            }
        }
        return this.delegate;
    }

    @Override
    public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> consumer, Style style) {
        return resolve().visit(consumer, style);
    }

    @Override
    public <T> Optional<T> visit(FormattedText.ContentConsumer<T> consumer) {
        return resolve().visit(consumer);
    }

    @Override
    public MutableComponent resolve(CommandSourceStack source, Entity entity, int depth) throws CommandSyntaxException {
        Object[] resolved = new Object[this.args.length];
        for (int i = 0; i < resolved.length; i++) {
            Object arg = this.args[i];
            resolved[i] = arg instanceof Component c ? ComponentUtils.updateForEntity(source, c, entity, depth) : arg;
        }
        return MutableComponent.create(new ModTranslatableComponent(this.key, resolved));
    }

    public String getKey() {
        return this.key;
    }

    public Object[] getArgs() {
        return this.args;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof ModTranslatableComponent other) {
            return this.key.equals(other.key) && Arrays.equals(this.args, other.args);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 31 * this.key.hashCode() + Arrays.hashCode(this.args);
    }

    @Override
    public String toString() {
        return "crn_translation{key='" + this.key + "', args=" + Arrays.toString(this.args) + "}";
    }
}
