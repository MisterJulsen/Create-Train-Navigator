package de.mrjulsen.crn.navigator.route;

import de.mrjulsen.crn.backend.api.StationRef;
import de.mrjulsen.crn.backend.timing.StopTimes;

/**
 * One station a leg of a route calls at, with the times the traveller experiences there.
 *
 * @param station    The station called at, with its tag and platform attached.
 * @param entryIndex The index of this call's instruction in the train's schedule.
 * @param cycle      Which visit of this station the call is, counted from the first one the backend
 *                   ever saw. A projection onto a later journey cycle has a higher one, which is how
 *                   a call still to come is told apart from the one the train is working on now.
 * @param scheduled  The timetable times, against which any delay is measured.
 * @param arrival    When the train gets there, in transformed game ticks.
 * @param departure  When it leaves again. Equal to the arrival at the end of a leg.
 */
public record RouteCall(StationRef station, int entryIndex, int cycle, StopTimes scheduled, long arrival, long departure) {

    /** The station's name, as a shorthand for {@code station().name()}. */
    public String stationName() {
        return station.name();
    }

    /** The platform this call uses, or empty if none is known. */
    public String platform() {
        return station.platform();
    }

    /** How long the train stays here, in ticks. */
    public long stayDuration() {
        return Math.max(0, departure - arrival);
    }

    /** How much later than the timetable the train gets here, in ticks. */
    public long arrivalDeviation() {
        return scheduled.isKnown() ? arrival - scheduled.arrival() : 0;
    }

    /** How much later than the timetable the train leaves here, in ticks. */
    public long departureDeviation() {
        return scheduled.isKnown() ? departure - scheduled.departure() : 0;
    }

    @Override
    public String toString() {
        return station.name() + "@" + arrival;
    }
}
