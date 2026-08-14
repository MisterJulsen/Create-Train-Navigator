package de.mrjulsen.crn.core.navigator.index;

import java.util.List;
import java.util.UUID;

import net.minecraft.resources.ResourceLocation;

/**
 * One usable run of a train, laid out as a single cycle of calls. A repeating train carries its
 * cycle {@link #period}, so any call's occurrence at or after an arbitrary time is found by adding
 * a whole number of periods to its base time - no future cycles are ever materialised, which is why
 * the search needs no time horizon.
 */
public record Trip(
    UUID trainId,
    UUID sessionId,
    String trainName,
    String displayName,
    ResourceLocation iconId,
    long period,
    List<TripCall> calls,
    int boardableCalls
) {

    /** Marks a call whose train cannot be caught at or after a queried time (a spent, non-repeating run). */
    public static final int UNREACHABLE = Integer.MIN_VALUE;

    public Trip {
        calls = calls == null ? List.of() : List.copyOf(calls);
        boardableCalls = Math.max(0, Math.min(boardableCalls, Math.max(0, calls.size() - 1)));
    }

    public TripCall call(int index) {
        return calls.get(index);
    }

    public int size() {
        return calls.size();
    }

    public boolean repeats() {
        return period > 0;
    }

    public long firstDeparture() {
        return calls.get(0).departure();
    }

    public long lastArrival() {
        return calls.get(calls.size() - 1).arrival();
    }

    public boolean isUsable() {
        return calls.size() >= 2;
    }

    /**
     * How many whole cycles must be added to the given call's base departure so it falls at or after
     * {@code notBefore}. Zero where the base departure already does; {@link #UNREACHABLE} for a
     * non-repeating run whose call has already departed.
     */
    public int cyclesAhead(int call, long notBefore) {
        long base = calls.get(call).departure();
        if (base >= notBefore) {
            return 0;
        }
        if (period <= 0) {
            return UNREACHABLE;
        }
        long diff = notBefore - base;
        return (int) ((diff + period - 1) / period);
    }

    /** The tick offset added to every base time of this trip when ridden {@code cycles} cycles later. */
    public long shiftFor(int cycles) {
        return cycles <= 0 || period <= 0 ? 0 : (long) cycles * period;
    }

    @Override
    public String toString() {
        return displayName + " (" + calls.size() + " calls, period " + period + ")";
    }
}
