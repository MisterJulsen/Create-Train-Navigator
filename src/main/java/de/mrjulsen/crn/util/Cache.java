package de.mrjulsen.crn.util;

import java.util.Optional;
import java.util.function.Supplier;

public class Cache<T> {
    private T obj = null;
    private Supplier<T> provider;

    public Cache(Supplier<T> provider) {
        this.provider = provider;
    }

    public boolean isCached() {
        return this.obj != null;
    }
    
    public T get() {
        return !this.isCached() ? this.obj = this.provider.get() : this.obj;
    }

    public Optional<T> getIfAvailable() {
        return !this.isCached() ? Optional.empty() : Optional.of(this.obj);
    }

    public void clear() {
        this.obj = null;
    }
}