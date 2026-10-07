package de.mrjulsen.crn.core.navigator;

/**
 * A station a journey must pass through, optionally staying there a while.
 *
 * @param station The station to pass through.
 * @param minStay The least time to spend there before travelling on, in ticks, or zero for none.
 */
public record Waypoint(String station, long minStay) {

    public Waypoint {
        station = station == null ? "" : station.trim();
        minStay = Math.max(0, minStay);
    }

    /** A waypoint at the given station with no minimum stay. */
    public static Waypoint of(String station) {
        return new Waypoint(station, 0);
    }

    /** A waypoint at the given station with the given minimum stay, in ticks. */
    public static Waypoint of(String station, long minStay) {
        return new Waypoint(station, minStay);
    }

    /** Whether a minimum stay is set. */
    public boolean hasStay() {
        return minStay > 0;
    }

    @Override
    public String toString() {
        return hasStay() ? station + " (" + minStay + "t)" : station;
    }
}
