package de.mrjulsen.crn.core.navigator.index;

import java.util.List;
import java.util.UUID;

import net.minecraft.resources.ResourceLocation;

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

    public long shiftFor(int cycles) {
        return cycles <= 0 || period <= 0 ? 0 : (long) cycles * period;
    }

    @Override
    public String toString() {
        return displayName + " (" + calls.size() + " calls, period " + period + ")";
    }
}
