package de.mrjulsen.crn.backend.timing;

import java.util.List;

import com.simibubi.create.content.trains.schedule.ScheduleEntry;
import com.simibubi.create.content.trains.schedule.condition.ScheduleWaitCondition;

import de.mrjulsen.crn.api.IPredictableWaitCondition;

/**
 * Estimates a train's departure time at a stop from the wait conditions of its schedule entry.
 * <p>
 * Wait conditions are combined in groups: all conditions within a group must be fulfilled, while
 * the train may depart as soon as any one group is. This estimator mirrors that - each group's
 * completion is approximated by chaining its predictable conditions, and the earliest group wins.
 */
public final class DepartureEstimator {

    /**
     * @param departure    The regular departure time.
     * @param minDeparture The earliest possible departure time, if waits can be shortened.
     */
    public record Result(long departure, long minDeparture) {}

    private DepartureEstimator() {}

    /**
     * @param entry       The schedule entry of the stop.
     * @param arrivalTime The estimated arrival time in transformed game ticks.
     * @return The estimated departure times, never before the arrival time.
     */
    public static Result estimate(ScheduleEntry entry, long arrivalTime) {
        if (entry == null || entry.conditions == null || entry.conditions.isEmpty()) {
            return new Result(arrivalTime, arrivalTime);
        }

        long bestDeparture = Long.MAX_VALUE;
        long bestMinDeparture = Long.MAX_VALUE;
        boolean anyPredictable = false;

        for (List<ScheduleWaitCondition> group : entry.conditions) {
            long departure = arrivalTime;
            long minDeparture = arrivalTime;
            for (ScheduleWaitCondition condition : group) {
                if (condition instanceof IPredictableWaitCondition predictable) {
                    departure = Math.max(departure, predictable.waitUntil(departure));
                    minDeparture = Math.max(minDeparture, predictable.waitMinUntil(minDeparture));
                    anyPredictable = true;
                }
            }
            bestDeparture = Math.min(bestDeparture, departure);
            bestMinDeparture = Math.min(bestMinDeparture, minDeparture);
        }

        if (!anyPredictable) {
            return new Result(arrivalTime, arrivalTime);
        }
        return new Result(Math.max(arrivalTime, bestDeparture), Math.max(arrivalTime, bestMinDeparture));
    }
}
