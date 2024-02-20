package de.mrjulsen.crn.util;

import java.util.Objects;

public class Pair<A, B> extends Single<A> {
    protected B value2;

    public Pair(A value1, B value2) {
        super(value1);
        this.value2 = value2;
    }

    public static <A, B> Pair<A, B>of(A first, B second) {
        return new Pair<A, B>(first, second);
    }

    public B getSecond() {
        return value2;
    }

    protected void setSecond(B value) {
        this.value2 = value;
    }

    public Pair<A, B> swap(Pair<A, B> pair) {
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

    public static class MutablePair<A, B> extends Pair<A, B> {

        public MutablePair(A value1, B value2) {
            super(value1, value2);
        }

        @Override
        protected void setFirst(A value) {
            super.setFirst(value);
        }

        @Override
        public void setSecond(B value) {
            super.setSecond(value);
        }        
    }
}
