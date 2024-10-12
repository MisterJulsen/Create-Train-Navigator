package de.mrjulsen.crn.data.navigation;

public interface ITrainListenerClient<T> extends AutoCloseable {
    void update(T data);
}
