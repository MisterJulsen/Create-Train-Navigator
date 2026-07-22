package de.mrjulsen.crn.navigator.index;

import de.mrjulsen.crn.backend.api.StationRef;
import de.mrjulsen.crn.backend.timing.StopTimes;

/**
 * One call of a trip, as the route search sees it.
 *
 * @param node      The id of the station node this call serves, i.e. the position in the index's
 *                  station table. Stations sharing a tag share a node.
 * @param station   The station actually called at, which is what the search routes on.
 * @param scheduledStation The station the timetable plans for, carried along only so the result can
 *                  tell the traveller that the two differ.
 * @param entryIndex The index of this call's instruction in the train's schedule.
 * @param stopIndex The position of this call's stop within the train's journey, counting from its
 *                  first. A trip is laid out in travel order, so this only ever grows - except where
 *                  the journey starts over, which is the one place it drops back. That is how
 *                  {@link #startsNewLap(TripCall)} tells a lap boundary from an ordinary step, and it
 *                  cannot be told from the section alone: a journey with a single section has the
 *                  same one either side of the boundary.
 * @param scheduled The timetable times of this call.
 * @param arrival   When the train gets there, in transformed game ticks.
 * @param departure When it leaves again.
 * @param visits    Which visit of this stop the call is, counted as departures completed while being
 *                  tracked. A cyclic journey is laid out twice over, and a call in the second lap
 *                  counts one higher than the same stop in the first - without that, a call the train
 *                  reaches again before the traveller boards would be mistaken for the planned one.
 *                  {@link Trip#shifted(long, int)} deliberately leaves this alone: the offset for
 *                  whole unrolled cycles is added once, where the route call is built.
 * @param section   The service this call is operated as.
 * @param title     The schedule title the train carries towards this call. May be empty.
 */
public record TripCall(int node, StationRef station, StationRef scheduledStation, int entryIndex, int stopIndex,
                       StopTimes scheduled, long arrival, long departure, int visits, TripSection section,
                       String title) {

    /**
     * Whether the journey has started over between the given call and this one, i.e. whether this
     * call belongs to the next lap rather than the same run.
     */
    public boolean startsNewLap(TripCall previous) {
        return previous != null && stopIndex <= previous.stopIndex();
    }

    /** This call as it happens the given number of ticks later. */
    public TripCall shifted(long ticks) {
        return new TripCall(node, station, scheduledStation, entryIndex, stopIndex, scheduled.shifted(ticks),
            arrival + ticks, departure + ticks, visits, section, title);
    }
}
