package de.mrjulsen.crn.backend.timing;

/**
 * Projects a stop's times into future journey cycles.
 * <p>
 * The backend measures one run: when the train reaches each stop on the cycle it is currently
 * working through. A route search needs more than that - "this train also calls here again one
 * cycle later, and again after that" - because a connection may only work on a later run, and
 * because a traveller arriving at a station wants the next departure, not the one that already left.
 * <p>
 * That question needs no new measurement: a cyclic journey repeats every
 * {@linkplain de.mrjulsen.crn.backend.core.TrackedTrain#getTotalDuration() total duration}, so a
 * later occurrence of a stop is its current times shifted by whole multiples of that. This class is
 * that shift and nothing more - stateless, so it never has to be kept in sync with anything.
 *
 * <h2>What this does not model</h2>
 * A projected cycle assumes the train repeats its current run unchanged. It carries the delay of the
 * current run forward rather than assuming the train recovers, which keeps a connection from being
 * advertised as reachable when the feeding train is late. The further ahead the projection reaches,
 * the more it is a timetable statement and the less it is a prediction.
 */
public final class CycleProjector {

    private CycleProjector() {}

    /** How many whole cycles a stop has to advance for its arrival to be at or after a time. */
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

    /**
     * The given times advanced to the first occurrence at or after {@code notBefore}.
     * <p>
     * Returns them unchanged if they already are, if nothing is known, or if the journey has no
     * cycle duration - a non-cyclic run happens once, so there is no later occurrence to project.
     */
    public static StopTimes atOrAfter(StopTimes times, long cycleDuration, long notBefore) {
        return advancedBy(times, cycleDuration, cyclesUntil(times, cycleDuration, notBefore));
    }

    /** The given times advanced by a number of whole cycles. */
    public static StopTimes advancedBy(StopTimes times, long cycleDuration, int cycles) {
        if (cycles <= 0 || cycleDuration <= 0 || !times.isKnown()) {
            return times;
        }
        return times.shifted(cycles * cycleDuration);
    }
}
