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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableFormatException;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("all")
public class ModTranslatableComponent implements ComponentContents {
   public static final Object[] NO_ARGS = new Object[0];
   private static final FormattedText TEXT_PERCENT = FormattedText.of("%");
   private static final FormattedText TEXT_NULL = FormattedText.of("null");
   private final String key;
   @Nullable
   private final String fallback;
   private final Object[] args;
   @Nullable
   private Language decomposedWith;
   private List<FormattedText> decomposedParts = ImmutableList.of();
   private static final Pattern FORMAT_PATTERN = Pattern.compile("%(?:(\\d+)\\$)?([A-Za-z%]|$)");

   public ModTranslatableComponent(String key, @Nullable String fallback, Object[] args) {
      this.key = key;
      this.fallback = fallback;
      this.args = args;
   }

   private void decompose() {
      Language language = ClientWrapper.getCurrentClientLanguage();
      if (language != this.decomposedWith) {
         this.decomposedWith = language;
         String string = this.fallback != null ? language.getOrDefault(this.key, this.fallback) : language.getOrDefault(this.key);

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
      for(j = 0; matcher.find(j); j = l) {
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
            consumer.accept(this.getArgument(m));
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

   public final FormattedText getArgument(int index) {
      if (index >= 0 && index < this.args.length) {
         Object object = this.args[index];
         if (object instanceof Component) {
            return (Component)object;
         } else {
            return object == null ? TEXT_NULL : FormattedText.of(object.toString());
         }
      } else {
         throw new IllegalArgumentException(this.toString());
      }
   }

   public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> styledContentConsumer, Style style) {
      this.decompose();
      Iterator var3 = this.decomposedParts.iterator();

      Optional optional;
      do {
         if (!var3.hasNext()) {
            return Optional.empty();
         }

         FormattedText formattedText = (FormattedText)var3.next();
         optional = formattedText.visit(styledContentConsumer, style);
      } while(!optional.isPresent());

      return optional;
   }

   public <T> Optional<T> visit(FormattedText.ContentConsumer<T> contentConsumer) {
      this.decompose();
      Iterator var2 = this.decomposedParts.iterator();

      Optional optional;
      do {
         if (!var2.hasNext()) {
            return Optional.empty();
         }

         FormattedText formattedText = (FormattedText)var2.next();
         optional = formattedText.visit(contentConsumer);
      } while(!optional.isPresent());

      return optional;
   }

   public MutableComponent resolve(@Nullable CommandSourceStack nbtPathPattern, @Nullable Entity entity, int recursionDepth) throws CommandSyntaxException {
      Object[] objects = new Object[this.args.length];

      for(int i = 0; i < objects.length; ++i) {
         Object object = this.args[i];
         if (object instanceof Component) {
            objects[i] = ComponentUtils.updateForEntity(nbtPathPattern, (Component)object, entity, recursionDepth);
         } else {
            objects[i] = object;
         }
      }

      return MutableComponent.create(new ModTranslatableComponent(this.key, this.fallback, objects));
   }

   public boolean equals(Object object) {
      if (this == object) {
         return true;
      } else {
         boolean var10000;
         if (object instanceof ModTranslatableComponent) {
            ModTranslatableComponent translatableContents = (ModTranslatableComponent)object;
            if (Objects.equals(this.key, translatableContents.key) && Objects.equals(this.fallback, translatableContents.fallback) && Arrays.equals(this.args, translatableContents.args)) {
               var10000 = true;
               return var10000;
            }
         }

         var10000 = false;
         return var10000;
      }
   }

   public int hashCode() {
      int i = Objects.hashCode(this.key);
      i = 31 * i + Objects.hashCode(this.fallback);
      i = 31 * i + Arrays.hashCode(this.args);
      return i;
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
}
