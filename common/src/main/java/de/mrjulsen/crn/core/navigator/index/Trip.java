package de.mrjulsen.crn.core.navigator.index;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.resources.ResourceLocation;

public record Trip(
    UUID trainId,
    UUID sessionId,
    String trainName,
    String displayName,
    ResourceLocation iconId,
    int cycle,
    List<TripCall> calls,
    int boardableCalls
) {

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

    public long firstDeparture() {
        return calls.get(0).departure();
    }

    public long lastArrival() {
        return calls.get(calls.size() - 1).arrival();
    }

    public boolean isUsable() {
        return calls.size() >= 2;
    }

    public Trip shifted(long ticks, int cycle) {
        List<TripCall> shifted = new ArrayList<>(calls.size());
        for (TripCall call : calls) {
            shifted.add(call.shifted(ticks));
        }
        return new Trip(trainId, sessionId, trainName, displayName, iconId, cycle, shifted, boardableCalls);
    }

    @Override
    public String toString() {
        return displayName + " #" + cycle + " (" + calls.size() + " calls)";
    }
}
