package de.mrjulsen.crn.navigator;

/**
 * A station a route has to pass through, given by the traveller rather than found by the search.
 * <p>
 * A waypoint is a real call: the train has to stop there and the traveller has to be able to get
 * off, so a service that merely passes through does not satisfy it.
 *
 * @param station The station or station tag name to travel via.
 * @param minStay How long the traveller wants to stay there before continuing, in ticks. The search
 *                never allows less than the minimum transfer time, whatever this says.
 */
public record Waypoint(String station, long minStay) {

    public Waypoint {
        station = station == null ? "" : station.trim();
        minStay = Math.max(0, minStay);
    }

    /** A waypoint the traveller only passes through, without asking for time there. */
    public static Waypoint of(String station) {
        return new Waypoint(station, 0);
    }

    /** A waypoint the traveller wants to spend at least the given number of ticks at. */
    public static Waypoint of(String station, long minStay) {
        return new Waypoint(station, minStay);
    }

    /** Whether the traveller asked for time at this waypoint rather than just a change of trains. */
    public boolean hasStay() {
        return minStay > 0;
    }

    @Override
    public String toString() {
        return hasStay() ? station + " (" + minStay + "t)" : station;
    }
}
