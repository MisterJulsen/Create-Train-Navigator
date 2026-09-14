package de.mrjulsen.crn.web.openapi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import de.mrjulsen.mcdragonlib.util.DLColor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class SchemaTypeRegistry {

    @FunctionalInterface
    public interface SchemaFactory {
        Map<String, Object> create();
    }

    private static final Map<Class<?>, SchemaFactory> EXACT = new LinkedHashMap<>();
    private static final List<Map.Entry<Class<?>, SchemaFactory>> ASSIGNABLE = new ArrayList<>();

    private SchemaTypeRegistry() {}

    static {
        SchemaFactory string = () -> Schemas.typed(OpenApiKeys.TYPE_STRING);
        register(string, String.class, char.class, Character.class);

        SchemaFactory bool = () -> Schemas.typed(OpenApiKeys.TYPE_BOOLEAN);
        register(bool, boolean.class, Boolean.class);

        SchemaFactory int32 = () -> Schemas.formatted(OpenApiKeys.TYPE_INTEGER, OpenApiKeys.FORMAT_INT32);
        register(int32, int.class, Integer.class, short.class, Short.class, byte.class, Byte.class);

        SchemaFactory int64 = () -> Schemas.formatted(OpenApiKeys.TYPE_INTEGER, OpenApiKeys.FORMAT_INT64);
        register(int64, long.class, Long.class);

        register(() -> Schemas.formatted(OpenApiKeys.TYPE_NUMBER, OpenApiKeys.FORMAT_FLOAT), float.class, Float.class);
        register(() -> Schemas.formatted(OpenApiKeys.TYPE_NUMBER, OpenApiKeys.FORMAT_DOUBLE), double.class, Double.class);

        register(() -> Schemas.formatted(OpenApiKeys.TYPE_STRING, OpenApiKeys.FORMAT_UUID), UUID.class);

        register(() -> string(null, "minecraft:overworld"), ResourceLocation.class);
        register(() -> string("ARGB color as #AARRGGBB.", "#FF3366CC"), DLColor.class);
        register(SchemaTypeRegistry::blockPos, BlockPos.class);

        registerAssignable(Component.class, () -> string("Serialized Minecraft text component (JSON string).", null));
    }

    public static void register(SchemaFactory factory, Class<?>... types) {
        for (Class<?> type : types) {
            EXACT.put(type, factory);
        }
    }

    public static void registerAssignable(Class<?> base, SchemaFactory factory) {
        ASSIGNABLE.add(Map.entry(base, factory));
    }

    public static Optional<Map<String, Object>> lookup(Class<?> type) {
        SchemaFactory exact = EXACT.get(type);
        if (exact != null) {
            return Optional.of(exact.create());
        }
        for (Map.Entry<Class<?>, SchemaFactory> entry : ASSIGNABLE) {
            if (entry.getKey().isAssignableFrom(type)) {
                return Optional.of(entry.getValue().create());
            }
        }
        return Optional.empty();
    }

    private static Map<String, Object> string(String description, String example) {
        Map<String, Object> node = Schemas.typed(OpenApiKeys.TYPE_STRING);
        if (description != null) {
            node.put(OpenApiKeys.DESCRIPTION, description);
        }
        if (example != null) {
            node.put(OpenApiKeys.EXAMPLE, example);
        }
        return node;
    }

    private static Map<String, Object> blockPos() {
        Map<String, Object> node = Schemas.object();
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("x", Schemas.formatted(OpenApiKeys.TYPE_INTEGER, OpenApiKeys.FORMAT_INT32));
        properties.put("y", Schemas.formatted(OpenApiKeys.TYPE_INTEGER, OpenApiKeys.FORMAT_INT32));
        properties.put("z", Schemas.formatted(OpenApiKeys.TYPE_INTEGER, OpenApiKeys.FORMAT_INT32));
        node.put(OpenApiKeys.PROPERTIES, properties);
        return node;
    }
}
