package de.mrjulsen.crn.core.navigator;

import java.util.Comparator;

import de.mrjulsen.crn.core.navigator.route.RouteJourney;

/** How found journeys should be ordered, so the best one for the chosen goal comes first. */
public enum RouteOptimization {

    /** Prefers the journey that arrives earliest, then the one with fewer transfers. */
    FASTEST(Comparator
        .comparingLong(RouteJourney::arrival)
        .thenComparingInt(RouteJourney::transferCount)
        .thenComparing(Comparator.comparingLong(RouteJourney::departure).reversed())),

    /** Prefers the journey with fewer transfers, then the one that arrives earliest. */
    FEWEST_TRANSFERS(Comparator
        .comparingInt(RouteJourney::transferCount)
        .thenComparingLong(RouteJourney::arrival)
        .thenComparing(Comparator.comparingLong(RouteJourney::departure).reversed()));

    private final Comparator<RouteJourney> comparator;

    RouteOptimization(Comparator<RouteJourney> comparator) {
        this.comparator = comparator;
    }

    /** The ordering this optimization applies, putting the best journey first. */
    public Comparator<RouteJourney> comparator() {
        return comparator;
    }
}
