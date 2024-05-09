package de.mrjulsen.crn.client.lang;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import de.mrjulsen.crn.client.ClientWrapper;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.BaseComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.ContextAwareComponent;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TranslatableFormatException;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("all")
public class ModTranslatableComponent extends BaseComponent implements ContextAwareComponent {
    private static final Object[] NO_ARGS = new Object[0];
    private static final FormattedText TEXT_PERCENT = FormattedText.of("%");
    private static final FormattedText TEXT_NULL = FormattedText.of("null");
    private final String key;
    private final Object[] args;
    @Nullable
    private Language decomposedWith;
    private List<FormattedText> decomposedParts = ImmutableList.of();
    private static final Pattern FORMAT_PATTERN = Pattern.compile("%(?:(\\d+)\\$)?([A-Za-z%]|$)");

    public ModTranslatableComponent(String key) {
        this.key = key;
        this.args = NO_ARGS;
    }

    public ModTranslatableComponent(String key, Object... args) {
        this.key = key;
        this.args = args;
    }

    private void decompose() {
        Language language = ClientWrapper.getCurrentClientLanguage();
        if (language != this.decomposedWith) {
            this.decomposedWith = language;
            String string = language.getOrDefault(this.key);

            try {
                ImmutableList.Builder<FormattedText> builder = ImmutableList.builder();
                Objects.requireNonNull(builder);
                this.decomposeTemplate(string, builder::add);
                this.decomposedParts = builder.build();
            } catch (TranslatableFormatException var4) {
                this.decomposedParts = ImmutableList.of(FormattedText.of(string));
            }

        }
    }

    private void decomposeTemplate(String formatTemplate, Consumer<FormattedText> consumer) {
        Matcher matcher = FORMAT_PATTERN.matcher(formatTemplate);

        int i = 0;

        int j;
        int l;
        for (j = 0; matcher.find(j); j = l) {
            int k = matcher.start();
            l = matcher.end();
            String string;
            if (k > j) {
                string = formatTemplate.substring(j, k);
                if (string.indexOf(37) != -1) {
                    throw new IllegalArgumentException();
                }

                consumer.accept(FormattedText.of(string));
            }

            string = matcher.group(2);
            String string2 = formatTemplate.substring(k, l);
            if ("%".equals(string) && "%%".equals(string2)) {
                consumer.accept(TEXT_PERCENT);
            } else {
                if (!"s".equals(string)) {
                    throw new IllegalArgumentException("Unsupported format: '" + string2 + "'");
                }

                String string3 = matcher.group(1);
                int m = string3 != null ? Integer.parseInt(string3) - 1 : i++;
                if (m < this.args.length) {
                    consumer.accept(this.getArgument(m));
                }
            }
        }

        if (j < formatTemplate.length()) {
            String string4 = formatTemplate.substring(j);
            if (string4.indexOf(37) != -1) {
                throw new IllegalArgumentException();
            }

            consumer.accept(FormattedText.of(string4));
        }
    }

    private FormattedText getArgument(int index) {
        if (index >= this.args.length) {
            throw new IllegalArgumentException(this + ", " + index);
        } else {
            Object object = this.args[index];
            if (object instanceof Component) {
                return (Component) object;
            } else {
                return object == null ? TEXT_NULL : FormattedText.of(object.toString());
            }
        }
    }

    public ModTranslatableComponent plainCopy() {
        return new ModTranslatableComponent(this.key, this.args);
    }

    public <T> Optional<T> visitSelf(FormattedText.StyledContentConsumer<T> consumer, Style style) {
        this.decompose();
        Iterator var3 = this.decomposedParts.iterator();

        Optional optional;
        do {
            if (!var3.hasNext()) {
                return Optional.empty();
            }

            FormattedText formattedText = (FormattedText) var3.next();
            optional = formattedText.visit(consumer, style);
        } while (!optional.isPresent());

        return optional;
    }

    public <T> Optional<T> visitSelf(FormattedText.ContentConsumer<T> consumer) {
        this.decompose();
        Iterator var2 = this.decomposedParts.iterator();

        Optional optional;
        do {
            if (!var2.hasNext()) {
                return Optional.empty();
            }

            FormattedText formattedText = (FormattedText) var2.next();
            optional = formattedText.visit(consumer);
        } while (!optional.isPresent());

        return optional;
    }

    public MutableComponent resolve(@Nullable CommandSourceStack commandSourceStack, @Nullable Entity entity,
            int recursionDepth) throws CommandSyntaxException {
        Object[] objects = new Object[this.args.length];

        for (int i = 0; i < objects.length; ++i) {
            Object object = this.args[i];
            if (object instanceof Component) {
                objects[i] = ComponentUtils.updateForEntity(commandSourceStack, (Component) object, entity,
                        recursionDepth);
            } else {
                objects[i] = object;
            }
        }

        return new ModTranslatableComponent(this.key, objects);
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof ModTranslatableComponent)) {
            return false;
        } else {
            ModTranslatableComponent ModTranslatableComponent = (ModTranslatableComponent) object;
            return Arrays.equals(this.args, ModTranslatableComponent.args) && this.key.equals(ModTranslatableComponent.key)
                    && super.equals(object);
        }
    }

    public int hashCode() {
        int i = super.hashCode();
        i = 31 * i + this.key.hashCode();
        i = 31 * i + Arrays.hashCode(this.args);
        return i;
    }

    public String toString() {
        String var10000 = this.key;
        return "ModTranslatableComponent{key='" + var10000 + "', args=" + Arrays.toString(this.args) + ", siblings="
                + this.siblings + ", style=" + this.getStyle() + "}";
    }

    public String getKey() {
        return this.key;
    }

    public Object[] getArgs() {
        return this.args;
    }
}
