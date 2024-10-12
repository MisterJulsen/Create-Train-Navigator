package de.mrjulsen.crn.util;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;

public interface IListenable<T> {

    Map<String, IdentityHashMap<Object, Consumer<T>>> getListeners();

    default void createEvent(String name) {
        if (getListeners().containsKey(name)) {
            throw new IllegalArgumentException("An event with this ID has already been created: " + name);
        }
        getListeners().put(name, new IdentityHashMap<>());
    }

    default void deleteEvent(String name) {
        if (getListeners().containsKey(name)) {
            getListeners().remove(name);
        }
    }

    default void clearEvents() {
        getListeners().clear();
    }

    default int eventsCount() {
        return getListeners().size();
    }

    default int listenersCount(String name) {
        if (!hasEvent(name)) {
            throw new IllegalArgumentException("This listener event does not exist: " + name);
        }

        return getListeners().get(name).size();
    }

    default boolean hasEvent(String name) {
        return getListeners().containsKey(name);
    }
    
    default void listen(String name, Object listenerObject, Consumer<T> listener) {
        if (!hasEvent(name)) {
            throw new IllegalArgumentException("This listener event does not exist: " + name);
        }

        getListeners().get(name).put(listenerObject, listener);
    }
    
    default void stopListening(String name, Object listenerObject) {
        if (!hasEvent(name)) {
            throw new IllegalArgumentException("This listener event does not exist: " + name);
        }

        getListeners().get(name).remove(listenerObject);
    }
    
    default void stopListeningAll(Object listenerObject) {
        getListeners().values().stream().forEach(x -> {
            if (x.containsKey(listenerObject)) {
                x.remove(listenerObject);
            }
        });
    }

    default void notifyListeners(String name, T data) {
        if (!hasEvent(name)) {
            throw new IllegalArgumentException("This listener event does not exist: " + name);
        }

        new ArrayList<>(getListeners().get(name).values()).stream().forEach(x -> x.accept(data));
    }
}
