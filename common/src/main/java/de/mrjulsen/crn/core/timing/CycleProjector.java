package de.mrjulsen.crn.core.timing;

public final class CycleProjector {

    private CycleProjector() {}

    public static int cyclesUntil(StopTimes times, long cycleDuration, long notBefore) {
        if (!times.isKnown() || cycleDuration <= 0) {
            return 0;
        }
        long behind = notBefore - times.arrival();
        if (behind <= 0) {
            return 0;
        }
        return (int) ((behind + cycleDuration - 1) / cycleDuration);
    }

    public static StopTimes atOrAfter(StopTimes times, long cycleDuration, long notBefore) {
        return advancedBy(times, cycleDuration, cyclesUntil(times, cycleDuration, notBefore));
    }

    public static StopTimes advancedBy(StopTimes times, long cycleDuration, int cycles) {
        if (cycles <= 0 || cycleDuration <= 0 || !times.isKnown()) {
            return times;
        }
        return times.shifted(cycles * cycleDuration);
    }
}
