package de.mrjulsen.crn.core.navigator;

/** Why a route search returned the result it did. */
public enum NavigationStatus {

    /** A journey was found. */
    OK,

    /** No server was running, so the backend could not be searched. */
    BACKEND_INACTIVE,

    /** The query was missing an origin or a destination. */
    INCOMPLETE_QUERY,

    /** An origin, destination or waypoint named a station that does not exist. */
    UNKNOWN_STATION,

    /** The same station was named more than once in the query. */
    SAME_STATION,

    /** No direct journey exists, and only direct ones were asked for. */
    NO_DIRECT_ROUTE,

    /** No journey could be found within the search limits. */
    NO_ROUTE;

    /** Whether the search succeeded. */
    public boolean isSuccess() {
        return this == OK;
    }

    /** Whether the search ran without error but found no journey. */
    public boolean isEmptyResult() {
        return this == NO_ROUTE || this == NO_DIRECT_ROUTE;
    }
}
