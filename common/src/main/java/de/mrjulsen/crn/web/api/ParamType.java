package de.mrjulsen.crn.web.api;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public final class ParamType<T> implements Function<String, T> {

    private static final Map<Class<?>, ParamType<?>> BY_TYPE = new HashMap<>();

    public static final ParamType<String> STRING = register(String.class, "string", value -> value);
    public static final ParamType<Integer> INT = register(Integer.class, "integer", Integer::parseInt);
    public static final ParamType<Long> LONG = register(Long.class, "long", Long::parseLong);
    public static final ParamType<Double> DOUBLE = register(Double.class, "double", Double::parseDouble);
    public static final ParamType<Boolean> BOOL = register(Boolean.class, "boolean", ParamType::parseBool);
    public static final ParamType<UUID> UUID = register(UUID.class, "uuid", java.util.UUID::fromString);

    static {
        INT.registerAlias(int.class);
        LONG.registerAlias(long.class);
        DOUBLE.registerAlias(double.class);
        BOOL.registerAlias(boolean.class);
    }

    private final String name;
    private final Function<String, T> parser;

    private ParamType(String name, Function<String, T> parser) {
        this.name = name;
        this.parser = parser;
    }

    public static <T> ParamType<T> of(String name, Function<String, T> parser) {
        return new ParamType<>(name, parser);
    }

    public static <E extends Enum<E>> ParamType<E> enumType(Class<E> type) {
        return of(type.getSimpleName().toLowerCase(), raw -> Enum.valueOf(type, raw.trim().toUpperCase()));
    }

    public static <T> ParamType<T> register(Class<T> type, String name, Function<String, T> parser) {
        return of(name, parser).registerAlias(type);
    }

    public static <T> ParamType<T> registerAlias(Class<T> type, ParamType<T> parser) {
        BY_TYPE.put(type, parser);
        return parser;
    }

    public ParamType<T> registerAlias(Class<T> type) {
        BY_TYPE.put(type, this);
        return this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> ParamType<T> forType(Class<T> type) {
        ParamType<?> found = BY_TYPE.get(type);
        if (found != null) {
            return (ParamType<T>) found;
        }
        if (type.isEnum()) {
            return (ParamType<T>) enumType((Class) type);
        }
        throw new IllegalStateException("No ParamType registered for " + type.getName());
    }

    @Override
    public T apply(String raw) {
        return parser.apply(raw);
    }

    public String typeName() {
        return name;
    }

    private static Boolean parseBool(String raw) {
        if ("true".equalsIgnoreCase(raw) || "1".equals(raw)) {
            return true;
        }
        if ("false".equalsIgnoreCase(raw) || "0".equals(raw)) {
            return false;
        }
        throw new IllegalArgumentException(raw);
    }
}
