package de.mrjulsen.crn.api.json;

import java.lang.reflect.Field;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import de.mrjulsen.mcdragonlib.util.DLColor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

public final class JsonConvert {

    private static final Gson GSON = builder().create();
    private static final Gson PRETTY = builder().setPrettyPrinting().create();

    private JsonConvert() {}

    public static GsonBuilder builder() {
        return new GsonBuilder()
            .setFieldNamingStrategy(JsonConvert::toSnakeCase)
            .serializeNulls()
            .registerTypeAdapter(ResourceLocation.class, new ResourceLocationAdapter())
            .registerTypeAdapter(DLColor.class, new DLColorAdapter())
            .registerTypeAdapter(BlockPos.class, new BlockPosAdapter())
            .registerTypeAdapter(Component.class, new TextComponentAdapter())
            .registerTypeAdapter(MutableComponent.class, new MutableTextComponentAdapter());
    }

    public static String toJson(Object value) {
        return GSON.toJson(value);
    }

    public static String toPrettyJson(Object value) {
        return PRETTY.toJson(value);
    }

    public static JsonElement toJsonTree(Object value) {
        return GSON.toJsonTree(value);
    }

    public static <T> T fromJson(String json, Class<T> type) {
        return GSON.fromJson(json, type);
    }

    private static String toSnakeCase(Field field) {
        return toSnakeCase(field.getName());
    }

    public static String toSnakeCase(String name) {
        StringBuilder result = new StringBuilder(name.length() + 4);
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
