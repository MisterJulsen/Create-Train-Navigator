package de.mrjulsen.crn.core.timing;

import java.util.List;

import com.simibubi.create.content.trains.schedule.ScheduleEntry;
import com.simibubi.create.content.trains.schedule.condition.ScheduleWaitCondition;

import de.mrjulsen.crn.data.schedule.IPredictableWaitCondition;

public final class DepartureEstimator {

    public record Result(long departure, long minDeparture) {}

    private DepartureEstimator() {}

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
