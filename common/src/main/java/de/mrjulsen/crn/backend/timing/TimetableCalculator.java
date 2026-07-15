package de.mrjulsen.crn.backend.timing;

import java.util.List;
import java.util.function.Function;

import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.crn.backend.schedule.JourneyStop;
import de.mrjulsen.crn.backend.schedule.TrainJourney;

/**
 * Computes the real-time projection for all stops of a journey.
 * <p>
 * Starting from the train's current position, the calculator walks through all stops in travel
 * order and chains learned leg durations with estimated departure times. The result is written
 * into the {@link StopTimings} of each stop. Scheduled times are never touched here; comparing
 * them with the projected real-time values yields the delay of the train.
 */
public final class TimetableCalculator {

    private TimetableCalculator() {}

    /**
     * Recalculates the real-time estimates of all stops.
     *
     * @param train     The Create train.
     * @param journey   The parsed journey.
     * @param timings   Lookup of the timing data per stop.
     * @param atStation           Whether the train is currently waiting at a station.
     * @param now                 The current transformed game time.
     * @param elapsedTransitTicks How many ticks the train already traveled on its current leg
     *                            (0 while at a station). Used to project the arrival at the
     *                            current destination without depending on instantaneous speed.
     * @param nonMovingTicks      Of {@code elapsedTransitTicks}, how many were spent not actually
     *                            moving (signal waits, stalls). Excluded from the "progress" the
     *                            countdown assumes, so a currently blocked train shows growing
     *                            delay immediately instead of only once the whole leg overruns.
     */
    public static void projectRealtime(Train train, TrainJourney journey, Function<JourneyStop, StopTimings> timings, boolean atStation, long now, int elapsedTransitTicks, int nonMovingTicks) {
        if (journey.isEmpty() || train.runtime == null) {
            return;
        }

        JourneyStop current = journey.getCurrentStop(train.runtime.currentEntry).orElse(null);
        if (current == null) {
            return;
        }

        List<JourneyStop> order = journey.getStopsInTravelOrder(current);
        // Two chains are walked in parallel:
        //  - "time"        : the real-time prediction. Departures are shortened to catch up delays
        //                    (resolveDeparture), so downstream arrivals reflect the actual expected
        //                    times. This is what the delay/ETA display reads.
        //  - "nominalTime" : the ideal timetable from the current position with zero carried-over
        //                    delay. Every departure uses the full nominal wait and the chain stays
        //                    internally consistent (scheduled leg == learned leg). A soft reset
        //                    anchors the schedule to this chain, so a train running normally departs
        //                    and arrives exactly on schedule instead of accruing a buffer-sized
        //                    phantom delay on every leg.
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
                    arrival = now + estimateRemainingTransit(train, timing, elapsedTransitTicks, nonMovingTicks);
                }
                // The current stop is the anchor point of both chains: its arrival is reality.
                nominalArrival = arrival;
            } else {
                int leg = timing.legDuration().get();
                if (leg < 0 || chainBroken) {
                    // Without a learned leg duration no reliable estimate is possible from here on.
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
                // The train is still here, so it cannot have departed in the past.
                departure = Math.max(departure, now);
            }
            timing.setRealtime(new StopTimes(arrival, departure, estimate.minDeparture()));
            time = departure;

            // Nominal chain: full wait, no catch-up shortening, chained from nominal departures.
            DepartureEstimator.Result nominalEstimate = DepartureEstimator.estimate(stop.getScheduleEntry(), nominalArrival);
            timing.setNominalTimes(new StopTimes(nominalArrival, nominalEstimate.departure(), nominalEstimate.minDeparture()));
            nominalTime = nominalEstimate.departure();
        }
    }

    /**
     * Applies the catch-up rule: a stop with flexible wait conditions (e.g.
     * {@code DynamicDelayCondition}) only shortens its stay proportionally to the train's current
     * delay, down to a minimum - mirroring the real in-game logic
     * ({@code DynamicDelayCondition.tickCompletion()}: {@code max(totalWaitTicks - currentDelay,
     * minWaitTicks)}). An on-time arrival must predict the full nominal wait, not the minimum;
     * jumping straight to the minimum regardless of delay makes an undelayed train's dwell look
     * like a growing "phantom" deviation as real time catches up to the (wrongly short) estimate.
     */
    private static long resolveDeparture(StopTimings timing, DepartureEstimator.Result estimate, long arrival) {
        boolean flexible = estimate.minDeparture() < estimate.departure();
        if (flexible && timing.getScheduled().isKnown()) {
            long currentDelay = Math.max(0, arrival - timing.getScheduled().arrival());
            long shortened = Math.max(estimate.departure() - currentDelay, estimate.minDeparture());
            return Math.max(Math.max(shortened, timing.getScheduled().departure()), arrival);
        }
        return estimate.departure();
    }

    /**
     * Estimates how many ticks the train still needs to reach its current destination.
     * <p>
     * Once a leg duration has been learned, this is a simple linear countdown (learned duration
     * minus elapsed transit time) rather than a live physics estimate: the train's instantaneous
     * speed fluctuates heavily right after departure (still accelerating) and produced large,
     * self-correcting over-/underestimates every full update. The countdown tracks reality
     * directly and only deviates when the leg genuinely takes longer or shorter than usual, which
     * is exactly the delay signal callers want to see.
     * <p>
     * Without a learned duration yet, a physics-based estimate (remaining distance / speed) is
     * used as a fallback. If any {@link de.mrjulsen.crn.backend.api.ISpeedLimitProvider} (e.g. an
     * addon like Tramways) reports speed limits ahead on the train's path, those are integrated
     * instead of extrapolating the train's current, possibly momentarily throttled, speed across
     * the entire remaining distance.
     * <p>
     * {@code elapsedTransitTicks} includes ticks the train spent not moving at all (signal waits,
     * stalls) - counting those as progress would hide a currently blocked train's delay until the
     * whole leg duration has elapsed. {@code nonMovingTicks} is subtracted first so the countdown
     * only credits actually traveled time; the excluded ticks show up as delay right away instead.
     */
    public static int estimateRemainingTransit(Train train, StopTimings timing, int elapsedTransitTicks, int nonMovingTicks) {
        if (train.navigation == null || train.navigation.destination == null) {
            return 0;
        }

        int leg = timing.legDuration().get();
        if (leg > 0) {
            int movingTicks = Math.max(0, elapsedTransitTicks - nonMovingTicks);
            return Math.max(0, leg - movingTicks);
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

    /**
     * The total duration of one full journey cycle: the sum of all leg durations and scheduled
     * stay durations.
     *
     * @return The duration in ticks, or {@code -1} if not all legs have been learned yet.
     */
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
