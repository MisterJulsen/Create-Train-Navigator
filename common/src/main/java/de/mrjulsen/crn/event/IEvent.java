package de.mrjulsen.crn.event;

public interface IEvent<T> {
    /**
     * Registers a new event listener.
     * @param modid The mod ID of the mod listening for this event. This does not have to be the mod ID, but it is recommended to be.
     * @param event Your callback which will be executed when the event triggers.
     */
    void register(String modid, T event);
    
    /**
     * Removes the event listener.
     * @param modid The mod ID of event listener to be removed.
     */
    void unregister(String modid);
}
