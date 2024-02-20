package de.mrjulsen.crn.util;

import java.util.Objects;

public class Tripple<A, B, C> extends Pair<A, B>{

    protected C value3;

    protected Tripple(A value1, B value2, C value3) {
        super(value1, value2);
        this.value3 = value3;
    }

    public static <A, B, C> Tripple<A, B, C>of(A first, B second, C third) {
        return new Tripple<A, B, C>(first, second, third);
    }

    public C getThird() {
        return value3;
    }

    protected void setThird(C value) {
        this.value3 = value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Tripple other) {
            return getFirst().equals(other.getFirst()) &&
                getSecond().equals(other.getSecond())&&
                getThird().equals(other.getThird());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFirst(), getSecond(), getThird());
    }

    @Override
    public String toString() {
        return String.format("(%s, %s, %s)", getFirst(), getSecond(), getThird());
    }

    public static class MutableTripple<A, B, C> extends Tripple<A, B, C> {

        public MutableTripple(A value1, B value2, C value3) {
            super(value1, value2, value3);
        }

        @Override
        public void setFirst(A value) {
            super.setFirst(value);
        }

        @Override
        public void setSecond(B value) {
            super.setSecond(value);
        }    
        
        @Override
        protected void setThird(C value) {
            super.setThird(value);
        }
    }
}
