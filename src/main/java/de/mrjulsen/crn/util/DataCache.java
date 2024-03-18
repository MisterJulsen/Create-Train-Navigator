package de.mrjulsen.crn.util;

import java.util.Optional;
import java.util.function.Function;

public class DataCache<T, D> {
    private T obj = null;
    private Function<D, T> provider;

    public DataCache(Function<D, T> provider) {
        this.provider = provider;
    }

    public boolean isCached() {
        return this.obj != null;
    }
    
    public T get(D data) {
        return !this.isCached() ? this.obj = this.provider.apply(data) : this.obj;
    }

    public Optional<T> getIfAvailable() {
        return !this.isCached() ? Optional.empty() : Optional.of(this.obj);
    }

    public void clear() {
        this.obj = null;
    }
}