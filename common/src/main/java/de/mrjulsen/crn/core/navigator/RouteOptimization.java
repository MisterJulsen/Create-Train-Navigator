package de.mrjulsen.crn.core.navigator;

import java.util.Comparator;

import de.mrjulsen.crn.core.navigator.route.RouteJourney;

public enum RouteOptimization {

    FASTEST(Comparator
        .comparingLong(RouteJourney::arrival)
        .thenComparingInt(RouteJourney::transferCount)
        .thenComparing(Comparator.comparingLong(RouteJourney::departure).reversed())),

    FEWEST_TRANSFERS(Comparator
        .comparingInt(RouteJourney::transferCount)
        .thenComparingLong(RouteJourney::arrival)
        .thenComparing(Comparator.comparingLong(RouteJourney::departure).reversed()));

    private final Comparator<RouteJourney> comparator;

    RouteOptimization(Comparator<RouteJourney> comparator) {
        this.comparator = comparator;
    }

    public Comparator<RouteJourney> comparator() {
        return comparator;
    }
}
