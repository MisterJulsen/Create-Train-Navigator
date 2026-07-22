package de.mrjulsen.crn.navigator.index;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.resources.ResourceLocation;

/**
 * One uninterrupted travel opportunity: a train running a contiguous stretch of its journey once,
 * with concrete times a traveller can board and alight at.
 * <p>
 * A cyclic train produces one of these per cycle within the search horizon, since each cycle is a
 * separate opportunity at different times. A journey interrupted by a stretch passengers may not use
 * produces one per usable stretch, because the traveller cannot ride across the gap.
 *
 * @param trainId     The id of the train operating this trip.
 * @param sessionId   The train's tracking session, for detecting that a planned route went stale.
 * @param trainName   The train's own name.
 * @param displayName The line name if one is assigned, otherwise the train name.
 * @param iconId      The id of the train's icon, or {@code null} if it carries none.
 * @param cycle       How many journey cycles ahead of the current run this trip is. {@code 0} is the
 *                    run the train is on now.
 * @param calls       The calls in travel order, with times increasing throughout.
 * @param boardableCalls How many of the leading calls a traveller may get on at.
 *                    <p>
 *                    A cyclic trip lists two full cycles rather than one, so that someone boarding
 *                    near the end of a cycle can still reach a station lying beyond the point where
 *                    the journey wraps around. Only the first cycle is offered for boarding, since
 *                    the second one is the very same opportunity as the following trip and would
 *                    otherwise be searched twice.
 */
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

    /** The call at the given position in travel order. */
    public TripCall call(int index) {
        return calls.get(index);
    }

    /** How many calls this trip has. */
    public int size() {
        return calls.size();
    }

    /** When this trip leaves its first call, in transformed game ticks. */
    public long firstDeparture() {
        return calls.get(0).departure();
    }

    /** When this trip reaches its last call, in transformed game ticks. */
    public long lastArrival() {
        return calls.get(calls.size() - 1).arrival();
    }

    /** Whether a traveller could use this trip at all, which needs somewhere to get on and off. */
    public boolean isUsable() {
        return calls.size() >= 2;
    }

    /** This trip as it runs the given number of ticks later, i.e. a later cycle of the same journey. */
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
