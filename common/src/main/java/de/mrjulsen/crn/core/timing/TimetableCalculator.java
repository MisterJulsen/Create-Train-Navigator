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

            int residual = timing.dwellResidualTicks();

            DepartureEstimator.Result estimate = DepartureEstimator.estimate(stop.getScheduleEntry(), arrival);
            long departure = resolveDeparture(timing, estimate, arrival) + residual;
            if (i == 0 && atStation) {
                departure = Math.max(departure, now);
            }
            timing.setRealtime(new StopTimes(arrival, departure, estimate.minDeparture() + residual));
            time = departure;

            DepartureEstimator.Result nominalEstimate = DepartureEstimator.estimate(stop.getScheduleEntry(), nominalArrival);
            timing.setNominalTimes(new StopTimes(nominalArrival, nominalEstimate.departure() + residual, nominalEstimate.minDeparture() + residual));
            nominalTime = nominalEstimate.departure() + residual;
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

    private static final double LIVE_CLAMP_LOWER = 0.5;
    private static final double LIVE_CLAMP_UPPER = 2.0;

    /**
     * Estimates how many ticks the train still needs to reach the next stop. Unlike the downstream
     * legs (which chain over the learned durations), the current leg is anchored to the train's
     * remaining path distance so the ETA tracks the train's real progress instead of counting a
     * learned duration down at a fixed rate. The learned leg duration calibrates that anchor; Create's
     * own {@code distanceStartedAt}/{@code distanceToDestination} give the remaining fraction of this
     * exact run (correct across reroutes), and the live speed profile brackets the result so an
     * unusually fast run or a temporary speed sign still shows through.
     */
    public static int estimateRemainingTransit(Train train, StopTimings timing, int elapsedTransitTicks, int nonMovingTicks) {
        if (train.navigation == null) {
            return 0;
        }

        int leg = timing.legDuration().get();
        boolean navigating = train.navigation.destination != null;
        double remainingDistance = navigating ? train.navigation.distanceToDestination : -1;
        double startDistance = train.navigation.distanceStartedAt;

        Integer live = estimateFromLiveConditions(train, remainingDistance);

        if (leg > 0 && navigating && startDistance > 0) {
            double fraction = Math.min(1.0, Math.max(0.0, remainingDistance / startDistance));
            int reference = (int) Math.round(leg * fraction);
            return bracket(reference, live);
        }

        if (live != null) {
            return live;
        }

        if (leg > 0) {
            int movingTicks = Math.max(0, elapsedTransitTicks - nonMovingTicks);
            return Math.max(0, leg - movingTicks);
        }

        return 0;
    }

    private static Integer estimateFromLiveConditions(Train train, double distance) {
        if (distance <= 0) {
            return distance == 0 ? 0 : null;
        }

        double cruiseSpeed = (train.maxSpeed() + train.maxTurnSpeed()) / 2;
        Integer viaSpeedLimits = SpeedProfileEstimator.estimate(train, distance, cruiseSpeed);
        if (viaSpeedLimits != null) {
            return viaSpeedLimits;
        }

        double speed = Math.min(train.throttle * train.maxSpeed(), cruiseSpeed);
        return speed > 0.05 ? (int)(distance / speed) * 2 : null;
    }

    private static int bracket(int reference, Integer live) {
        if (live == null) {
            return Math.max(0, reference);
        }
        int lower = (int) (live * LIVE_CLAMP_LOWER);
        int upper = (int) (live * LIVE_CLAMP_UPPER);
        return Math.max(0, Math.min(Math.max(reference, lower), upper));
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
