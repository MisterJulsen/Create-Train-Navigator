package de.mrjulsen.crn.util;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
                throw new IllegalArgumentException("This listener event does not exist: " + name + ". Valid events are: [" + getListeners().keySet().stream().collect(Collectors.joining(", ")) + "]");
        }

        return getListeners().get(name).size();
    }

    default boolean hasEvent(String name) {
        return getListeners().containsKey(name);
    }
    
    default void listen(String name, Object listenerObject, Consumer<T> listener) {
        if (!hasEvent(name)) {
            throw new IllegalArgumentException("This listener event does not exist: " + name + ". Valid events are: [" + getListeners().keySet().stream().collect(Collectors.joining(", ")) + "]");
        }

        getListeners().get(name).put(listenerObject, listener);
    }
    
    default void stopListening(String name, Object listenerObject) {
        if (!hasEvent(name)) {
            throw new IllegalArgumentException("This listener event does not exist: " + name + ". Valid events are: [" + getListeners().keySet().stream().collect(Collectors.joining(", ")) + "]");
        }

        getListeners().get(name).remove(listenerObject);
    }
    
    default void stopListeningAll(Object listenerObject) {        
        Iterator<IdentityHashMap<Object, Consumer<T>>> iterator = getListeners().values().iterator();
        while (iterator.hasNext()) {
            Map<Object, Consumer<T>> x = iterator.next();
            if (x.containsKey(listenerObject)) {
                iterator.remove();
            }
        }
    }

    default void notifyListeners(String name, T data) {
        if (!hasEvent(name)) {
            throw new IllegalArgumentException("This listener event does not exist: " + name + ". Valid events are: [" + getListeners().keySet().stream().collect(Collectors.joining(", ")) + "]");
        }

        Iterator<Consumer<T>> iterator = getListeners().get(name).values().iterator();
        while (iterator.hasNext()) {
            iterator.next().accept(data);
        }
    }
}
