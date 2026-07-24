package de.mrjulsen.crn.api.json;

import java.lang.reflect.Field;

import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import de.mrjulsen.mcdragonlib.util.DLColor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Converts API snapshots to and from JSON, for interfaces that carry data out of the game.
 * <p>
 * Field names become snake_case, nulls are written out rather than omitted, and Minecraft types
 * that Gson cannot handle on its own are supported: a resource location becomes its string form, a
 * colour an {@code #AARRGGBB} string, and a block position an object with {@code x}, {@code y} and
 * {@code z}.
 */
public final class CrnJson {

    private static final Gson GSON = builder().create();
    private static final Gson PRETTY = builder().setPrettyPrinting().create();

    private CrnJson() {}

    /**
     * A builder configured as described above, for callers needing their own settings or further
     * type adapters. Each call returns a new builder.
     */
    public static GsonBuilder builder() {
        return new GsonBuilder()
            .setFieldNamingStrategy(SNAKE_CASE)
            .serializeNulls()
            .registerTypeAdapter(ResourceLocation.class, new ResourceLocationAdapter())
            .registerTypeAdapter(DLColor.class, new DLColorAdapter())
            .registerTypeAdapter(BlockPos.class, new BlockPosAdapter());
    }

    public static String toJson(Object value) {
        return GSON.toJson(value);
    }

    /** The same as {@link #toJson(Object)}, but indented for reading. */
    public static String toPrettyJson(Object value) {
        return PRETTY.toJson(value);
    }

    public static JsonElement toJsonTree(Object value) {
        return GSON.toJsonTree(value);
    }

    public static <T> T fromJson(String json, Class<T> type) {
        return GSON.fromJson(json, type);
    }

    private static final FieldNamingStrategy SNAKE_CASE = new FieldNamingStrategy() {
        @Override
        public String translateName(Field field) {
            String name = field.getName();
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
    };
}
