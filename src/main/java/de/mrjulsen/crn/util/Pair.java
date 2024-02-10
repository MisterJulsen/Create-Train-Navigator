package de.mrjulsen.crn.util;

import java.util.Objects;

public class Pair<T, S> {
    protected T value1;
    protected S value2;

    public Pair(T value1, S value2) {
        this.value1 = value1;
        this.value2 = value2;
    }

    public static <T, S> Pair<T, S>of(T first, S second) {
        return new Pair<T,S>(first, second);
    }

    public T getFirst() {
        return value1;
    }

    public S getSecond() {
        return value2;
    }

    protected void setFirst(T value) {
        this.value1 = value;
    }

    protected void setSecond(S value) {
        this.value2 = value;
    }

    public Pair<T, S> swap(Pair<T, S> pair) {
        return Pair.of(pair.getFirst(), pair.getSecond());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Pair other) {
            return getFirst().equals(other.getFirst()) && getSecond().equals(other.getSecond());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFirst(), getSecond());
    }

    @Override
    public String toString() {
        return String.format("(%s, %s)", getFirst(), getSecond());
    }

    public static class MutablePair<T, S> extends Pair<T, S> {

        public MutablePair(T value1, S value2) {
            super(value1, value2);
        }

        @Override
        public void setFirst(T value) {
            super.setFirst(value);
        }

        @Override
        public void setSecond(S value) {
            super.setSecond(value);
        }        
    }
}
