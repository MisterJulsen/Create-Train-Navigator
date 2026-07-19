package de.mrjulsen.crn.navigator;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import de.mrjulsen.crn.navigator.route.RouteJourney;

/**
 * What a route search came back with: the routes worth offering, in the order the query asked for,
 * together with enough about the search itself to judge it.
 *
 * @param status         How the search turned out.
 * @param journeys       The routes found, best first according to the query's optimization.
 * @param computedAt     When the search ran, in transformed game ticks.
 * @param durationMs     How long it took, in milliseconds.
 * @param stationsSearched How many station nodes the search had available.
 * @param tripsScanned   How many travel opportunities it actually rode.
 */
public record NavigationResult(
    NavigationStatus status,
    List<RouteJourney> journeys,
    long computedAt,
    long durationMs,
    int stationsSearched,
    int tripsScanned
) {

    public NavigationResult {
        journeys = journeys == null ? List.of() : List.copyOf(journeys);
    }

    /** A result carrying nothing but the reason it is empty. */
    public static NavigationResult failed(NavigationStatus status, long computedAt, long durationMs) {
        return new NavigationResult(status, List.of(), computedAt, durationMs, 0, 0);
    }

    /** The route the query's optimization puts first. */
    public Optional<RouteJourney> best() {
        return journeys.isEmpty() ? Optional.empty() : Optional.of(journeys.get(0));
    }

    /** The route arriving earliest, whatever the query preferred. */
    public Optional<RouteJourney> fastest() {
        return journeys.stream().min(RouteOptimization.FASTEST.comparator());
    }

    /** The route with the fewest changes, whatever the query preferred. */
    public Optional<RouteJourney> mostComfortable() {
        return journeys.stream().min(RouteOptimization.FEWEST_TRANSFERS.comparator());
    }

    /** The routes ordered by when the traveller has to leave, as a departure board would list them. */
    public List<RouteJourney> byDeparture() {
        return journeys.stream().sorted(Comparator.comparingLong(RouteJourney::departure)).toList();
    }

    /** The routes needing no change of train. */
    public List<RouteJourney> directOnly() {
        return journeys.stream().filter(RouteJourney::isDirect).toList();
    }

    /** How many routes were found. */
    public int size() {
        return journeys.size();
    }

    /** Whether the search found nothing to offer. */
    public boolean isEmpty() {
        return journeys.isEmpty();
    }

    /** Whether the search found something. */
    public boolean isSuccess() {
        return status.isSuccess() && !journeys.isEmpty();
    }
}
