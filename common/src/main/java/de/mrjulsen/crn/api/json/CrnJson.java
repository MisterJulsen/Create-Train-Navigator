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
 * Turns the backend and navigator API records into JSON, for consumers outside the game - a REST
 * API, a log, an export.
 * <p>
 * The records are read by reflection, so a record is serializable by virtue of being a record and
 * nothing has to be registered here. Only types Gson cannot describe on its own get an adapter
 * below: the Minecraft and DragonLib value types that would otherwise be written as their internal
 * fields.
 *
 * <h2>What the JSON contains</h2>
 * Exactly the record components, and only those. Derived values - how late a train is, how long a
 * journey takes, what to display as its name - are methods rather than fields and do not appear.
 * A consumer computes them from the raw times the same way the game does.
 *
 * <h2>Field names</h2>
 * Component names are converted to {@code snake_case}, so {@code arrivalDeviation} is written as
 * {@code arrival_deviation}. The names therefore follow the Java component names: renaming a record
 * component changes the JSON and breaks consumers relying on the old name.
 */
public final class CrnJson {

    private static final Gson GSON = builder().create();
    private static final Gson PRETTY = builder().setPrettyPrinting().create();

    private CrnJson() {}

    /** A Gson configured for the API records, for callers needing their own instance. */
    public static GsonBuilder builder() {
        return new GsonBuilder()
            .setFieldNamingStrategy(SNAKE_CASE)
            .serializeNulls()
            .registerTypeAdapter(ResourceLocation.class, new ResourceLocationAdapter())
            .registerTypeAdapter(DLColor.class, new DLColorAdapter())
            .registerTypeAdapter(BlockPos.class, new BlockPosAdapter());
    }

    /** The given API object as compact JSON. */
    public static String toJson(Object value) {
        return GSON.toJson(value);
    }

    /** The given API object as indented JSON, for logs and manual inspection. */
    public static String toPrettyJson(Object value) {
        return PRETTY.toJson(value);
    }

    /** The given API object as a JSON tree, for embedding into a larger response. */
    public static JsonElement toJsonTree(Object value) {
        return GSON.toJsonTree(value);
    }

    /** Reads an API object of the given type back from JSON. */
    public static <T> T fromJson(String json, Class<T> type) {
        return GSON.fromJson(json, type);
    }

    /**
     * Converts {@code camelCase} component names to {@code snake_case}. Gson's built-in policy of
     * the same name is not used, so the exact spelling stays under this class's control.
     */
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
