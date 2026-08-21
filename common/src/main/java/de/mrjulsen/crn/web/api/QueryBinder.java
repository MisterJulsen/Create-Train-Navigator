package de.mrjulsen.crn.web.api;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import de.mrjulsen.crn.web.annotation.QueryModel;
import de.mrjulsen.crn.web.annotation.QueryParam;

public final class QueryBinder {

    private static final Object SKIP = new Object();

    private QueryBinder() {}

    public static <T> T bind(Request request, Class<T> model) {
        if (isRecordClass(model)) {
            return bindRecord(request, model);
        }

        Binding binding = analyze(model);
        int minParams = model.getAnnotation(QueryModel.class).requiredParams();
        int totalParams = (binding.factory() != null ? 1 : 0) + binding.refiners().size();
        if (minParams > totalParams) {
            throw new IllegalStateException(model.getName() + " requires minParams=" + minParams + " but only declares " + totalParams + " query parameter(s)");
        }

        Object current;
        int provided = 0;
        if (binding.factory() != null) {
            current = invoke(binding.factory(), null, seedArgument(request, binding.factory()));
            provided++;
        } else {
            current = getDefaultInstance(model);
        }
        Applied applied = applyRefiners(request, current, binding.refiners());
        provided += applied.count();
        if (provided < minParams) {
            throw new BadRequestException("This query requires at least " + minParams + " query parameter(s), but " + provided + " were provided");
        }
        @SuppressWarnings("unchecked")
        T result = (T) applied.instance();
        return result;
    }

    public static <T> T bind(Request request, Supplier<T> seed) {
        Objects.requireNonNull(seed, "seed");
        T instance = seed.get();
        Objects.requireNonNull(instance, "seed supplier returned null");
        Class<?> model = instance.getClass();
        Binding binding = analyze(model);
        int minParams = model.getAnnotation(QueryModel.class).requiredParams();
        if (minParams > binding.refiners().size()) {
            throw new IllegalStateException(model.getName() + " requires minParams=" + minParams + " but only declares " + binding.refiners().size() + " refinable query parameter(s)");
        }
        Applied applied = applyRefiners(request, instance, binding.refiners());
        if (applied.count() < minParams) {
            throw new BadRequestException("This query requires at least " + minParams + " query parameter(s), but " + applied.count() + " were provided");
        }
        @SuppressWarnings("unchecked")
        T result = (T) applied.instance();
        return result;
    }

    private record Binding(Method factory, List<Method> refiners) {}

    private static Binding analyze(Class<?> model) {
        if (!model.isAnnotationPresent(QueryModel.class)) {
            throw new IllegalStateException(model.getName() + " is not annotated with @RestQueryModel");
        }
        Method factory = null;
        List<Method> refiners = new ArrayList<>();
        for (Method method : model.getMethods()) {
            if (!method.isAnnotationPresent(QueryParam.class)) {
                continue;
            }
            if (method.getParameterCount() != 1) {
                throw new IllegalStateException("@RestQueryParam method must take exactly one parameter: " + method);
            }
            if (Modifier.isStatic(method.getModifiers())) {
                if (factory != null) {
                    throw new IllegalStateException("Multiple @RestQueryParam factories on " + model.getName());
                }
                factory = method;
            } else {
                refiners.add(method);
            }
        }
        return new Binding(factory, refiners);
    }

    private static boolean isRecordClass(Class<?> model) {
        if (!model.isRecord()) {
            return false;
        }
        for (RecordComponent component : model.getRecordComponents()) {
            if (component.isAnnotationPresent(QueryParam.class)) {
                return true;
            }
        }
        return false;
    }

    private static <T> T bindRecord(Request request, Class<T> model) {
        if (!model.isAnnotationPresent(QueryModel.class)) {
            throw new IllegalStateException(model.getName() + " is not annotated with @QueryModel");
        }

        RecordComponent[] components = model.getRecordComponents();
        Class<?>[] types = new Class<?>[components.length];
        Object[] args = new Object[components.length];
        int provided = 0;

        for (int i = 0; i < components.length; i++) {
            RecordComponent component = components[i];
            types[i] = component.getType();
            QueryParam param = component.getAnnotation(QueryParam.class);

            if (param == null) {
                args[i] = getDefaultValue(component.getType());
                continue;
            }
            Object value = parseParam(request, component.getType(), component.getGenericType(), param);
            if (value == SKIP) {
                args[i] = getDefaultValue(component.getType());
            } else {
                args[i] = value;
                provided++;
            }
        }

        int minParams = model.getAnnotation(QueryModel.class).requiredParams();
        if (provided < minParams) {
            throw new BadRequestException("This query requires at least " + minParams + " query parameter(s), but " + provided + " were provided");
        }
        return buildInstance(model, types, args);
    }



    private record Applied(Object instance, int count) {}

    private static Applied applyRefiners(Request request, Object current, List<Method> refiners) {
        int count = 0;
        for (Method refiner : refiners) {
            Object argument = getArg(request, refiner);
            if (argument != SKIP) {
                current = invoke(refiner, current, argument);
                count++;
            }
        }
        return new Applied(current, count);
    }

    private static Object seedArgument(Request request, Method factory) {
        Object argument = getArg(request, factory);
        if (argument == SKIP) {
            throw new BadRequestException("Missing required query parameter: " + factory.getAnnotation(QueryParam.class).value());
        }
        return argument;
    }

    private static Object getArg(Request request, Method method) {
        Parameter parameter = method.getParameters()[0];
        return parseParam(request, parameter.getType(), parameter.getParameterizedType(), method.getAnnotation(QueryParam.class));
    }

    private static Object parseParam(Request request, Class<?> type, Type genericType, QueryParam param) {
        String name = param.value();

        if (List.class.isAssignableFrom(type) || Set.class.isAssignableFrom(type)) {
            Function<String, ?> parser = ParamType.forType(getElementType(genericType));
            List<?> values = param.required() ? request.requireValues(name, parser) : request.queryValues(name, parser);
            if (values.isEmpty()) {
                return SKIP;
            }
            return Set.class.isAssignableFrom(type) ? new LinkedHashSet<>(values) : values;
        }

        Function<String, ?> parser = ParamType.forType(type);
        if (param.required()) {
            return request.requireQuery(name, parser);
        }
        Optional<?> value = request.query(name, parser);
        return value.isPresent() ? value.get() : SKIP;
    }

    private static <T> T getDefaultInstance(Class<T> model) {
        if (!model.isRecord()) {
            throw new IllegalStateException("Query model without a static @RestQueryParam factory must be a record: " + model.getName());
        }
        RecordComponent[] components = model.getRecordComponents();
        Class<?>[] types = new Class<?>[components.length];
        Object[] defaults = new Object[components.length];
        for (int i = 0; i < components.length; i++) {
            types[i] = components[i].getType();
            defaults[i] = getDefaultValue(types[i]);
        }
        return buildInstance(model, types, defaults);
    }

    private static <T> T buildInstance(Class<T> model, Class<?>[] types, Object[] args) {
        try {
            Constructor<T> constructor = model.getDeclaredConstructor(types);
            constructor.setAccessible(true);
            return constructor.newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to instantiate " + model.getName(), e);
        }
    }

    private static Object getDefaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class || type == short.class || type == byte.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == double.class) {
            return 0.0;
        }
        if (type == float.class) {
            return 0.0f;
        }
        if (type == char.class) {
            return '\0';
        }
        if (type == String.class) {
            return "";
        }
        if (Set.class.isAssignableFrom(type)) {
            return Set.of();
        }
        if (List.class.isAssignableFrom(type)) {
            return List.of();
        }
        if (Map.class.isAssignableFrom(type)) {
            return Map.of();
        }
        if (Optional.class.isAssignableFrom(type)) {
            return Optional.empty();
        }
        return null;
    }

    private static Class<?> getElementType(Type generic) {
        if (generic instanceof ParameterizedType parameterized && parameterized.getActualTypeArguments()[0] instanceof Class<?> element) {
            return element;
        }
        throw new IllegalStateException("Cannot resolve element type of " + generic);
    }

    private static Object invoke(Method method, Object target, Object argument) {
        try {
            return method.invoke(target, argument);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException("Failed to apply " + method.getName(), e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to apply " + method.getName(), e);
        }
    }
}
