package de.mrjulsen.crn.client.lang;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.TranslatableFormatException;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.util.InsertingContents;

import javax.annotation.Nullable;

@SuppressWarnings("all")
public class ModTranslatableComponent implements ComponentContents {
    public static final Object[] NO_ARGS = new Object[0];
    private static final Codec<Object> PRIMITIVE_ARG_CODEC;
    private static final Codec<Object> ARG_CODEC;
    public static final MapCodec<ModTranslatableComponent> CODEC;
    public static final ComponentContents.Type<ModTranslatableComponent> TYPE;
    private static final FormattedText TEXT_PERCENT;
    private static final FormattedText TEXT_NULL;
    private final String key;
    @Nullable
    private final String fallback;
    private final Object[] args;
    @Nullable
    private Language decomposedWith;
    private List<FormattedText> decomposedParts = ImmutableList.of();
    private static final Pattern FORMAT_PATTERN;

    private static DataResult<Object> filterAllowedArguments(@Nullable Object input) {
        return !isAllowedPrimitiveArgument(input) ? DataResult.error(() -> "This value needs to be parsed as component") : DataResult.success(input);
    }

    public static boolean isAllowedPrimitiveArgument(@Nullable Object input) {
        return input instanceof Number || input instanceof Boolean || input instanceof String;
    }

    private static Optional<List<Object>> adjustArgs(Object[] args) {
        return args.length == 0 ? Optional.empty() : Optional.of(Arrays.asList(args));
    }

    private static Object[] adjustArgs(Optional<List<Object>> args) {
        return args.map((list) -> list.isEmpty() ? NO_ARGS : list.toArray()).orElse(NO_ARGS);
    }

    private static ModTranslatableComponent create(String key, Optional<String> fallback, Optional<List<Object>> args) {
        return new ModTranslatableComponent(key, (String)fallback.orElse(null), adjustArgs(args));
    }

    public ModTranslatableComponent(String key, @Nullable String fallback, Object[] args) {
        this.key = key;
        this.fallback = fallback;
        this.args = args;
        if (!FMLEnvironment.production) {
            for(Object arg : this.args) {
                if (!(arg instanceof Component) && !isAllowedPrimitiveArgument(arg)) {
                    String var10002 = String.valueOf(arg);
                    throw new IllegalArgumentException("ModTranslatableComponent' arguments must be either a Component, Number, Boolean, or a String. Was given " + var10002 + " for " + this.key);
                }
            }
        }

    }

    public ComponentContents.Type<?> type() {
        return TYPE;
    }

    private void decompose() {
        Language language = ClientWrapper.getCurrentClientLanguage();;
        if (language != this.decomposedWith) {
            this.decomposedWith = language;
            Component langComponent = language.getComponent(this.key);
            if (langComponent != null) {
                this.decomposedParts = ImmutableList.of(langComponent);
                return;
            }

            String s = this.fallback != null ? language.getOrDefault(this.key, this.fallback) : language.getOrDefault(this.key);

            try {
                ImmutableList.Builder<FormattedText> builder = ImmutableList.builder();
                Objects.requireNonNull(builder);
                this.decomposeTemplate(s, builder::add);
                this.decomposedParts = builder.build();
            } catch (TranslatableFormatException var5) {
                this.decomposedParts = ImmutableList.of(FormattedText.of(s));
            }
        }

    }

    private void decomposeTemplate(String formatTemplate, Consumer<FormattedText> consumer) {
        Matcher matcher = FORMAT_PATTERN.matcher(formatTemplate);

        try {
            int i = 0;

            int j;
            int l;
            for(j = 0; matcher.find(j); j = l) {
                int k = matcher.start();
                l = matcher.end();
                if (k > j) {
                    String s = formatTemplate.substring(j, k);
                    if (s.indexOf(37) != -1) {
                        throw new IllegalArgumentException();
                    }

                    consumer.accept(FormattedText.of(s));
                }

                String s4 = matcher.group(2);
                String s1 = formatTemplate.substring(k, l);
                if ("%".equals(s4) && "%%".equals(s1)) {
                    consumer.accept(TEXT_PERCENT);
                } else {
                    if (!"s".equals(s4)) {
                        throw new IllegalArgumentException("Unsupported format: '" + s1 + "'");
                    }

                    String s2 = matcher.group(1);
                    int i1 = s2 != null ? Integer.parseInt(s2) - 1 : i++;
                    consumer.accept(this.getArgument(i1));
                }
            }

            if (j < formatTemplate.length()) {
                String s3 = formatTemplate.substring(j);
                if (s3.indexOf(37) != -1) {
                    throw new IllegalArgumentException();
                }

                consumer.accept(FormattedText.of(s3));
            }

        } catch (IllegalArgumentException illegalargumentexception) {
            throw new IllegalArgumentException(illegalargumentexception);
        }
    }

    private FormattedText getArgument(int index) {
        if (index >= 0 && index < this.args.length) {
            Object object = this.args[index];
            if (object instanceof Component) {
                return (Component)object;
            } else {
                return object == null ? TEXT_NULL : FormattedText.of(object.toString());
            }
        } else {
            throw new IllegalArgumentException("Illegal Argument: " + index);
        }
    }

    public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> styledContentConsumer, Style style) {
        this.decompose();
        Optional var6;
        Iterator var3 = this.decomposedParts.iterator();

        Optional<T> optional;
        do {
            if (!var3.hasNext()) {
                Optional var10 = Optional.empty();
                return var10;
            }

            FormattedText formattedtext = (FormattedText)var3.next();
            optional = formattedtext.visit(styledContentConsumer, style);
        } while(!optional.isPresent());

        var6 = optional;

        return var6;
    }

    public <T> Optional<T> visit(FormattedText.ContentConsumer<T> contentConsumer) {
        this.decompose();
        Optional var5;
        Iterator var2 = this.decomposedParts.iterator();

        Optional<T> optional;
        do {
            if (!var2.hasNext()) {
                Optional var9 = Optional.empty();
                return var9;
            }

            FormattedText formattedtext = (FormattedText)var2.next();
            optional = formattedtext.visit(contentConsumer);
        } while(!optional.isPresent());

        var5 = optional;

        return var5;
    }

    public MutableComponent resolve(@Nullable CommandSourceStack nbtPathPattern, @Nullable Entity entity, int recursionDepth) throws CommandSyntaxException {
        Object[] aobject = new Object[this.args.length];

        for(int i = 0; i < aobject.length; ++i) {
            Object object = this.args[i];
            if (object instanceof Component component) {
                aobject[i] = ComponentUtils.updateForEntity(nbtPathPattern, component, entity, recursionDepth);
            } else {
                aobject[i] = object;
            }
        }

        return MutableComponent.create(new ModTranslatableComponent(this.key, this.fallback, aobject));
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else {
            if (object instanceof ModTranslatableComponent) {
                ModTranslatableComponent ModTranslatableComponent = (ModTranslatableComponent)object;
                if (Objects.equals(this.key, ModTranslatableComponent.key) && Objects.equals(this.fallback, ModTranslatableComponent.fallback) && Arrays.equals(this.args, ModTranslatableComponent.args)) {
                    return true;
                }
            }

            return false;
        }
    }

    public int hashCode() {
        int i = Objects.hashCode(this.key);
        i = 31 * i + Objects.hashCode(this.fallback);
        return 31 * i + Arrays.hashCode(this.args);
    }

    public String toString() {
        String var10000 = this.key;
        return "translation{key='" + var10000 + "'" + (this.fallback != null ? ", fallback='" + this.fallback + "'" : "") + ", args=" + Arrays.toString(this.args) + "}";
    }

    public String getKey() {
        return this.key;
    }

    @Nullable
    public String getFallback() {
        return this.fallback;
    }

    public Object[] getArgs() {
        return this.args;
    }

    static {
        PRIMITIVE_ARG_CODEC = ExtraCodecs.JAVA.validate(ModTranslatableComponent::filterAllowedArguments);
        ARG_CODEC = Codec.either(PRIMITIVE_ARG_CODEC, ComponentSerialization.CODEC).xmap((either) -> either.map((object) -> object, (arg) -> Objects.requireNonNullElse(arg.tryCollapseToString(), arg)), (object) -> {
            Either var10000;
            if (object instanceof Component component) {
                var10000 = Either.right(component);
            } else {
                var10000 = Either.left(object);
            }

            return var10000;
        });
        CODEC = RecordCodecBuilder.mapCodec((instance) -> instance.group(Codec.STRING.fieldOf("translate").forGetter((arg) -> arg.key), Codec.STRING.lenientOptionalFieldOf("fallback").forGetter((arg) -> Optional.ofNullable(arg.fallback)), ARG_CODEC.listOf().optionalFieldOf("with").forGetter((arg) -> adjustArgs(arg.args))).apply(instance, ModTranslatableComponent::create));
        TYPE = new ComponentContents.Type(CODEC, "translatable");
        TEXT_PERCENT = FormattedText.of("%");
        TEXT_NULL = FormattedText.of("null");
        FORMAT_PATTERN = Pattern.compile("%(?:(\\d+)\\$)?([A-Za-z%]|$)");
    }
}
