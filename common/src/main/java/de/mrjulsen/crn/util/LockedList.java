package de.mrjulsen.crn.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

public class LockedList<T> extends ArrayList<T> {

    private boolean locked;

    private void lock() {
        while (locked) {}
        locked = true;
    }

    private void unlock() {
        locked = false;
    }

    @Override
    public boolean add(T e) {
        lock();
        boolean b = super.add(e);
        unlock();
        return b;
    }

    @Override
    public void add(int index, T element) {
        lock();
        super.add(index, element);
        unlock();
    }

    @Override
    public boolean remove(Object o) {
        lock();
        boolean b = super.remove(o);
        unlock();
        return b;
    }

    @Override
    public void clear() {
        lock();
        super.clear();
        unlock();
    }

    @Override
    public T get(int index) {
        lock();
        T t = super.get(index);
        unlock();
        return t;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        lock();
        boolean b = super.retainAll(c);
        unlock();
        return b;
    }

    @Override
    public Stream<T> stream() {
        lock();
        Stream<T> s = super.stream();
        unlock();
        return s;
    }
}
