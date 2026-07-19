package de.mrjulsen.crn.navigator;

import java.util.Comparator;

import de.mrjulsen.crn.navigator.route.RouteJourney;

/**
 * What a traveller wants out of a route search when several routes are equally valid.
 * <p>
 * The search itself always produces the whole set of sensible answers - the fastest route, the one
 * with the fewest changes, and everything worthwhile in between. This only decides which of them is
 * presented first.
 */
public enum RouteOptimization {

    /** Be there as early as possible, accepting more changes on the way. */
    FASTEST(Comparator
        .comparingLong(RouteJourney::arrival)
        .thenComparingInt(RouteJourney::transferCount)
        .thenComparing(Comparator.comparingLong(RouteJourney::departure).reversed())),

    /** Change trains as rarely as possible, accepting a later arrival. */
    FEWEST_TRANSFERS(Comparator
        .comparingInt(RouteJourney::transferCount)
        .thenComparingLong(RouteJourney::arrival)
        .thenComparing(Comparator.comparingLong(RouteJourney::departure).reversed()));

    private final Comparator<RouteJourney> comparator;

    RouteOptimization(Comparator<RouteJourney> comparator) {
        this.comparator = comparator;
    }

    /** Orders journeys so the one this optimization asks for comes first. */
    public Comparator<RouteJourney> comparator() {
        return comparator;
    }
}
