package de.mrjulsen.crn.core;

import java.util.Set;

import de.mrjulsen.crn.data.TrainStationAlias;

public record NavigationResult(boolean success, Set<TrainStationAlias> failReasonStation) {

    public boolean impossible() {
        return !success && failReasonStation == null;
    }

    public boolean possible() {
        return success;
    }
}
