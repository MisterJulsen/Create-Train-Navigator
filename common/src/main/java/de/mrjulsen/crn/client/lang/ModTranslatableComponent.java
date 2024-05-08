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
   private static final Object[] NO_ARGS = new Object[0];
   private static final FormattedText TEXT_PERCENT = FormattedText.of("%");
   private static final FormattedText TEXT_NULL = FormattedText.of("null");
   private final String key;
   private final Object[] args;
   @Nullable
   private Language decomposedWith;
   private List<FormattedText> decomposedParts = ImmutableList.of();
   private static final Pattern FORMAT_PATTERN = Pattern.compile("%(?:(\\d+)\\$)?([A-Za-z%]|$)");

   public ModTranslatableComponent(String string) {
      this.key = string;
      this.args = NO_ARGS;
   }

   public ModTranslatableComponent(String string, Object... objects) {
      this.key = string;
      this.args = objects;
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

   private void decomposeTemplate(String string, Consumer<FormattedText> consumer) {
      Matcher matcher = FORMAT_PATTERN.matcher(string);

      int i = 0;

        int j;
      int l;
      for (j = 0; matcher.find(j); j = l) {
          int k = matcher.start();
          l = matcher.end();
          String string2;
          if (k > j) {
              string2 = string.substring(j, k);
              if (string2.indexOf(37) != -1) {
                  throw new IllegalArgumentException();
              }

              consumer.accept(FormattedText.of(string2));
          }

          string2 = matcher.group(2);
          String string3 = string.substring(k, l);
          if ("%".equals(string2) && "%%".equals(string3)) {
              consumer.accept(TEXT_PERCENT);
          } else {
              if (!"s".equals(string2)) {
                  throw new IllegalArgumentException("Unsupported format: '" + string3 + "'");
              }

              String string4 = matcher.group(1);
              int m = string4 != null ? Integer.parseInt(string4) - 1 : i++;
              if (m < this.args.length) {
                  consumer.accept(this.getArgument(m));
              }
          }
      }

      if (j < string.length()) {
          String string5 = string.substring(j);
          if (string5.indexOf(37) != -1) {
              throw new IllegalArgumentException();
          }

          consumer.accept(FormattedText.of(string5));
      }
   }

   public final FormattedText getArgument(int i) {
      if (i >= this.args.length) {
         throw new IllegalArgumentException();
      } else {
         Object object = this.args[i];
         if (object instanceof Component) {
            return (Component)object;
         } else {
            return object == null ? TEXT_NULL : FormattedText.of(object.toString());
         }
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

   public MutableComponent resolve(@Nullable CommandSourceStack commandSourceStack, @Nullable Entity entity, int i) throws CommandSyntaxException {
      Object[] objects = new Object[this.args.length];

      for(int j = 0; j < objects.length; ++j) {
         Object object = this.args[j];
         if (object instanceof Component) {
            objects[j] = ComponentUtils.updateForEntity(commandSourceStack, (Component)object, entity, i);
         } else {
            objects[j] = object;
         }
      }

      return MutableComponent.create(new ModTranslatableComponent(this.key, objects));
   }

   public boolean equals(Object object) {
      if (this == object) {
         return true;
      } else {
         boolean var10000;
         if (object instanceof ModTranslatableComponent) {
            ModTranslatableComponent ModTranslatableComponent = (ModTranslatableComponent)object;
            if (this.key.equals(ModTranslatableComponent.getKey()) && Arrays.equals(this.args, ModTranslatableComponent.getArgs())) {
               var10000 = true;
               return var10000;
            }
         }

         var10000 = false;
         return var10000;
      }
   }

   public int hashCode() {
      int i = this.key.hashCode();
      i = 31 * i + Arrays.hashCode(this.args);
      return i;
   }

   public String toString() {
      String var10000 = this.key;
      return "translation{key='" + var10000 + "', args=" + Arrays.toString(this.args) + "}";
   }

   public String getKey() {
      return this.key;
   }

   public Object[] getArgs() {
      return this.args;
   }
}
