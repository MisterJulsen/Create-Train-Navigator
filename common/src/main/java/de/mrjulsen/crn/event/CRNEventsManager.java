package de.mrjulsen.crn.event;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import de.mrjulsen.crn.CreateRailwaysNavigator;

import java.util.HashMap;
import java.util.HashSet;

public final class CRNEventsManager {

    @SuppressWarnings("rawtypes")
    protected static final Map<Class<? extends AbstractCRNEvent>, AbstractCRNEvent<?>> registeredEvents = new HashMap<>();

    static {
        registerEventInternal(new CRNClientEventsRegistryEvent());
        registerEventInternal(new CRNCommonEventsRegistryEvent());
    }

    /**
     * Returns the instance of the given Event, if registered, otherwiese a {@code NullPointerException} will be thrown.
     * @param <T> The type of the event to return.
     * @param clazz The class of the event.
     * @return The event, if available.
     */
    @SuppressWarnings("unchecked")
    public static <T extends AbstractCRNEvent<?>> T getEvent(Class<T> clazz) {
        if (registeredEvents.containsKey(clazz)) {
            return (T)registeredEvents.get(clazz);
        }
        throw new NullPointerException("The Event " + clazz.getName() + " is not registered!");
    }
    
    @SuppressWarnings("unchecked")
    public static <T extends AbstractCRNEvent<?>> Optional<T> getEventOptional(Class<T> clazz) {
        if (registeredEvents.containsKey(clazz)) {
            return Optional.ofNullable((T)registeredEvents.get(clazz));
        }
        return Optional.empty();
    }

    /**
     * Registers a new event.
     * @param eventInstance The instance of the new event.
     */
    public static void registerEvent(Supplier<AbstractCRNEvent<?>> eventInstance) {
        registerEvent(eventInstance.get());
    }

    /**
     * Registers a new event.
     * @param eventInstance The instance of the new event.
     */
    public static void registerEvent(AbstractCRNEvent<?> eventInstance) {
        if (eventInstance instanceof AbstractBuiltInCRNEvent) {
            throw new IllegalArgumentException("Cannot register CRN System events!");
        }
        registerEventInternal(eventInstance);
    }

    static void registerEventInternal(AbstractCRNEvent<?> eventInstance) {
        registeredEvents.put(eventInstance.getClass(), eventInstance);
    }

    /**
     * Removes all registered events. Usually done when stopping the server/world.
     */
    public static void clearEvents() {
        registeredEvents.values().removeIf(x -> !(x instanceof AbstractBuiltInCRNEvent));
        CreateRailwaysNavigator.LOGGER.info("All events have been closed.");
    }

    /**
     * Checks if the given event is registered to prevent errors.
     * @param <T> The type of the event to return.
     * @param clazz The class of the event.
     * @return {@true} if the event is registered.
     */
    public static <T extends AbstractCRNEvent<?>> boolean isRegistered(Class<T> clazz) {
        return registeredEvents.containsKey(clazz);
    }



    public static abstract class AbstractCRNEvent<T> implements IEvent<T> {
        protected final Map<String, T> listeners = new HashMap<>();
        protected final Set<String> idsToRemove = new HashSet<>();

        @Override
        public void register(String modid, T event) {
            listeners.put(modid, event);
        }

        @Override
        public void unregister(String modid) {
            if (listeners.containsKey(modid)) {
                idsToRemove.add(modid);
            }
        }

        public void tickPost() {
            listeners.keySet().removeAll(idsToRemove);
            idsToRemove.clear();
        }
    }
    
    static abstract class AbstractBuiltInCRNEvent<T> extends AbstractCRNEvent<T> {}
}
