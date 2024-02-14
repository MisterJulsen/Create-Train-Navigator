package de.mrjulsen.crn.util;

import java.util.Objects;

public class Single<A> {
    protected A value1;

    public Single(A value1) {
        this.value1 = value1;
    }

    public static <A> Single<A>of(A first) {
        return new Single<A>(first);
    }

    public A getFirst() {
        return value1;
    }

    protected void setFirst(A value) {
        this.value1 = value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Single other) {
            return getFirst().equals(other.getFirst());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFirst());
    }

    @Override
    public String toString() {
        return String.format("(%s)", getFirst());
    }

    public static class MutableSingle<A> extends Single<A> {

        public MutableSingle(A value1) {
            super(value1);
        }

        @Override
        public void setFirst(A value) {
            super.setFirst(value);
        }     
    }
}
