/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

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
