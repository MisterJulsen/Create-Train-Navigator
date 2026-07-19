package de.mrjulsen.crn.navigator.index;

import de.mrjulsen.crn.backend.api.StationRef;
import de.mrjulsen.crn.backend.timing.StopTimes;

/**
 * One call of a trip, as the route search sees it.
 *
 * @param node      The id of the station node this call serves, i.e. the position in the index's
 *                  station table. Stations sharing a tag share a node.
 * @param station   The station called at, for presenting the result.
 * @param entryIndex The index of this call's instruction in the train's schedule.
 * @param scheduled The timetable times of this call.
 * @param arrival   When the train gets there, in transformed game ticks.
 * @param departure When it leaves again.
 * @param visits    How often the train has already departed from this call while being tracked, so
 *                  that a call of a later cycle can be told apart from the current one.
 * @param section   The service this call is operated as.
 * @param title     The schedule title the train carries towards this call. May be empty.
 */
public record TripCall(int node, StationRef station, int entryIndex, StopTimes scheduled,
                       long arrival, long departure, int visits, TripSection section, String title) {

    /** This call as it happens the given number of ticks later. */
    public TripCall shifted(long ticks) {
        return new TripCall(node, station, entryIndex, scheduled.shifted(ticks),
            arrival + ticks, departure + ticks, visits, section, title);
    }
}
