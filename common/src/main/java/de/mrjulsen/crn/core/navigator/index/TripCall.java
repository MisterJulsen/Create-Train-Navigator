package de.mrjulsen.crn.core.navigator.index;

import de.mrjulsen.crn.api.core.ref.StationRef;
import de.mrjulsen.crn.core.timing.StopTimes;

public record TripCall(int node, StationRef station, StationRef scheduledStation, int entryIndex, int stopIndex,
                       StopTimes scheduled, long arrival, long departure, int visits, TripSection section,
                       String title) {

    public boolean startsNewLap(TripCall previous) {
        return previous != null && stopIndex <= previous.stopIndex();
    }
}
