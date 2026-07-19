package de.mrjulsen.crn.navigator;

/**
 * How a route search turned out, and if it found nothing, why.
 * <p>
 * Finding nothing is a normal outcome rather than an error, so the reason is carried here instead of
 * being thrown. Each of these tells the traveller something different about what to try next.
 */
public enum NavigationStatus {

    /** Routes were found. */
    OK,

    /** No server is running, so there is nothing to search. */
    BACKEND_INACTIVE,

    /** The query does not name both a start and a destination. */
    INCOMPLETE_QUERY,

    /** A station named in the query is not served by any train the search can see. */
    UNKNOWN_STATION,

    /** Start and destination are the same place, so there is nothing to travel. */
    SAME_STATION,

    /** Only direct connections were acceptable, and no single train covers the route. */
    NO_DIRECT_ROUTE,

    /** Nothing gets the traveller there within the limits the query set. */
    NO_ROUTE;

    /** Whether the search produced routes. */
    public boolean isSuccess() {
        return this == OK;
    }

    /** Whether the search itself worked and simply found nothing to offer. */
    public boolean isEmptyResult() {
        return this == NO_ROUTE || this == NO_DIRECT_ROUTE;
    }
}
