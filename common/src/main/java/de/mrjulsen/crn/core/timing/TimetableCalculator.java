package de.mrjulsen.crn.core.timing;

import java.util.List;
import java.util.function.Function;

import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.crn.core.schedule.JourneyStop;
import de.mrjulsen.crn.core.schedule.TrainJourney;

public final class TimetableCalculator {

    private TimetableCalculator() {}

    public static void projectRealtime(TrainJourney journey, Function<JourneyStop, StopTimings> timings, int currentEntry, boolean atStation, long now, int remainingTransitTicks) {
        if (journey.isEmpty()) {
            return;
        }

        JourneyStop current = journey.getCurrentStop(currentEntry).orElse(null);
        if (current == null) {
            return;
        }

        List<JourneyStop> order = journey.getStopsInTravelOrder(current);
        long time = now;
        long nominalTime = now;
        boolean chainBroken = false;

        for (int i = 0; i < order.size(); i++) {
            JourneyStop stop = order.get(i);
            StopTimings timing = timings.apply(stop);
            if (timing == null) {
                continue;
            }

            long arrival;
            long nominalArrival;
            if (i == 0) {
                if (atStation) {
                    arrival = timing.getLastActualArrival() >= 0 ? timing.getLastActualArrival() : now;
                } else {
                    arrival = now + remainingTransitTicks;
                }
                nominalArrival = arrival;
            } else {
                int leg = timing.legDuration().get();
                if (leg < 0 || chainBroken) {
                    timing.setRealtime(StopTimes.UNKNOWN);
                    timing.setNominalTimes(StopTimes.UNKNOWN);
                    chainBroken = true;
                    continue;
                }
                arrival = time + leg;
                nominalArrival = nominalTime + leg;
            }

            DepartureEstimator.Result estimate = DepartureEstimator.estimate(stop.getScheduleEntry(), arrival);
            long departure = resolveDeparture(timing, estimate, arrival);
            if (i == 0 && atStation) {
                departure = Math.max(departure, now);
            }
            timing.setRealtime(new StopTimes(arrival, departure, estimate.minDeparture()));
            time = departure;

            DepartureEstimator.Result nominalEstimate = DepartureEstimator.estimate(stop.getScheduleEntry(), nominalArrival);
            timing.setNominalTimes(new StopTimes(nominalArrival, nominalEstimate.departure(), nominalEstimate.minDeparture()));
            nominalTime = nominalEstimate.departure();
        }
    }

    private static long resolveDeparture(StopTimings timing, DepartureEstimator.Result estimate, long arrival) {
        boolean flexible = estimate.minDeparture() < estimate.departure();
        if (flexible && timing.getScheduled().isKnown()) {
            long currentDelay = Math.max(0, arrival - timing.getScheduled().arrival());
            long shortened = Math.max(estimate.departure() - currentDelay, estimate.minDeparture());
            return Math.max(Math.max(shortened, timing.getScheduled().departure()), arrival);
        }
        return estimate.departure();
    }

    public static int estimateRemainingTransit(Train train, StopTimings timing, int elapsedTransitTicks, int nonMovingTicks) {
        if (train.navigation == null) {
            return 0;
        }

        int leg = timing.legDuration().get();
        if (leg > 0) {
            int movingTicks = Math.max(0, elapsedTransitTicks - nonMovingTicks);
            return Math.max(0, leg - movingTicks);
        }

        if (train.navigation.destination == null) {
            return 0;
        }

        double distance = train.navigation.distanceToDestination;
        double cruiseSpeed = (train.maxSpeed() + train.maxTurnSpeed()) / 2;

        Integer viaSpeedLimits = SpeedProfileEstimator.estimate(train, distance, cruiseSpeed);
        if (viaSpeedLimits != null) {
            return viaSpeedLimits;
        }

        double speed = Math.min(train.throttle * train.maxSpeed(), cruiseSpeed);
        int physics = speed > 0.05 ? (int)(distance / speed) * 2 : Integer.MAX_VALUE;
        return physics == Integer.MAX_VALUE ? 0 : physics;
    }

    public static long computeTotalDuration(TrainJourney journey, Function<JourneyStop, StopTimings> timings) {
        long total = 0;
        for (JourneyStop stop : journey.getStops()) {
            StopTimings timing = timings.apply(stop);
            if (timing == null || !timing.legDuration().isInitialized()) {
                return -1;
            }
            total += timing.legDuration().get() + timing.getScheduled().stayDuration();
        }
        return total;
    }
}
