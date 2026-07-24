package de.mrjulsen.crn.core.navigator;

public enum NavigationStatus {

    OK,

    BACKEND_INACTIVE,

    INCOMPLETE_QUERY,

    UNKNOWN_STATION,

    SAME_STATION,

    NO_DIRECT_ROUTE,

    NO_ROUTE;

    public boolean isSuccess() {
        return this == OK;
    }

    public boolean isEmptyResult() {
        return this == NO_ROUTE || this == NO_DIRECT_ROUTE;
    }
}
