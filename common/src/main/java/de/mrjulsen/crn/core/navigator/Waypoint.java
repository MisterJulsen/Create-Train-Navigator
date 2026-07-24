package de.mrjulsen.crn.core.navigator;

public record Waypoint(String station, long minStay) {

    public Waypoint {
        station = station == null ? "" : station.trim();
        minStay = Math.max(0, minStay);
    }

    public static Waypoint of(String station) {
        return new Waypoint(station, 0);
    }

    public static Waypoint of(String station, long minStay) {
        return new Waypoint(station, minStay);
    }

    public boolean hasStay() {
        return minStay > 0;
    }

    @Override
    public String toString() {
        return hasStay() ? station + " (" + minStay + "t)" : station;
    }
}
