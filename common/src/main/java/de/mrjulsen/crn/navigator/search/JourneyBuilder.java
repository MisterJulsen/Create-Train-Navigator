package de.mrjulsen.crn.navigator.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.mrjulsen.crn.backend.api.RailwayBackendApi;
import de.mrjulsen.crn.backend.api.TrainSnapshot;
import de.mrjulsen.crn.navigator.NavigationQuery;
import de.mrjulsen.crn.navigator.index.TimetableIndex;
import de.mrjulsen.crn.navigator.index.Trip;
import de.mrjulsen.crn.navigator.index.TripCall;
import de.mrjulsen.crn.navigator.index.TripSection;
import de.mrjulsen.crn.navigator.route.RouteCall;
import de.mrjulsen.crn.navigator.route.RouteJourney;
import de.mrjulsen.crn.navigator.route.RouteLeg;
import de.mrjulsen.crn.navigator.route.RouteTransfer;
import de.mrjulsen.crn.navigator.route.TransferRisk;
import de.mrjulsen.crn.navigator.search.RaptorSearch.Ride;

/**
 * Turns what the search found into what a traveller is shown.
 * <p>
 * The search works in rides - get on this trip here, get off there - which is not quite what a
 * traveller experiences. A single ride can span a point where the train stops being one service and
 * becomes another, which is a leg boundary even though nobody gets out. This splits rides at those
 * points, and works out for every change how much room it leaves for things to go wrong.
 * <p>
 * One instance covers one search, so a train's current delay is looked up once however often it
 * turns up in the results.
 */
public final class JourneyBuilder {

    private final TimetableIndex index;
    private final NavigationQuery query;
    private final Map<UUID, Long> delayByTrain = new HashMap<>();

    public JourneyBuilder(TimetableIndex index, NavigationQuery query) {
        this.index = index;
        this.query = query;
    }

    /** The travel plan a chain of rides describes, or {@code null} if it describes nothing usable. */
    public RouteJourney build(List<Ride> chain) {
        if (chain == null || chain.isEmpty()) {
            return null;
        }

        List<RouteLeg> legs = new ArrayList<>();
        List<RouteTransfer> transfers = new ArrayList<>();

        for (Ride ride : chain) {
            Trip trip = index.trip(ride.trip());
            if (!legs.isEmpty()) {
                transfers.add(transferBetween(legs.get(legs.size() - 1), trip.call(ride.boardCall()), false));
            }
            appendLegs(trip, ride, legs, transfers);
        }

        return legs.isEmpty() ? null : new RouteJourney(legs, transfers);
    }

    /**
     * Adds one ride to the plan, as one leg per service it is operated as.
     * <p>
     * The station a service hands over at belongs to both of them: it is the terminus the leaving
     * service advertises and the first stop of the one taking over. So it ends one leg and starts the
     * next, and since the traveller stays seated the change between them needs no time and carries no
     * risk. Getting off exactly at such a station is not a change at all - the leg arriving there is
     * the whole story, and no leg on the new service is added.
     */
    private void appendLegs(Trip trip, Ride ride, List<RouteLeg> legs, List<RouteTransfer> transfers) {
        int start = ride.boardCall();
        TripSection section = trip.call(start).section();

        for (int i = start + 1; i <= ride.alightCall(); i++) {
            TripCall call = trip.call(i);
            if (call.section() == section) {
                continue;
            }
            legs.add(slice(trip, section, start, i));
            transfers.add(new RouteTransfer(call.station(), call.arrival(), call.departure(), TransferRisk.SAFE, true));
            start = i;
            section = call.section();
        }

        if (start < ride.alightCall()) {
            legs.add(slice(trip, section, start, ride.alightCall()));
        } else if (!transfers.isEmpty()) {
            transfers.remove(transfers.size() - 1);
        }
    }

    /** The stretch of a trip between two of its calls, as one leg of the given service. */
    private static RouteLeg slice(Trip trip, TripSection section, int from, int to) {
        List<RouteCall> calls = new ArrayList<>(to - from + 1);
        for (int i = from; i <= to; i++) {
            calls.add(toRouteCall(trip.call(i), trip.cycle(), i == from, i == to));
        }

        String title = trip.call(from).title();
        String destination = title == null || title.isBlank() ? section.destination().name() : title;
        return new RouteLeg(trip.trainId(), trip.sessionId(), trip.trainName(), trip.displayName(),
            section.line(), section.category(), destination, section.sectionIndex(), calls);
    }

    /**
     * A call as the traveller experiences it. Boarding, they do not care when the train got in, and
     * alighting they do not care when it leaves again, so those times are collapsed - which also
     * keeps a leg's own duration from including time spent standing at either end.
     */
    private static RouteCall toRouteCall(TripCall call, int cycle, boolean boarding, boolean alighting) {
        long arrival = boarding ? call.departure() : call.arrival();
        long departure = alighting ? call.arrival() : call.departure();
        return new RouteCall(call.station(), call.entryIndex(), call.visits() + cycle,
            call.scheduled(), arrival, Math.max(arrival, departure));
    }

    private RouteTransfer transferBetween(RouteLeg feeder, TripCall boarding, boolean sameTrain) {
        return connect(feeder, boarding.departure(), sameTrain);
    }

    /**
     * The change from one leg to a train leaving at the given time, rated by how much slack is left
     * once the feeding train's present delay is allowed for a second time.
     * <p>
     * That delay is already in the times the search used, so the connection does work as projected.
     * The question this answers is the one a traveller actually asks: what if it loses that much
     * again on the way.
     */
    public RouteTransfer connect(RouteLeg feeder, long departure, boolean sameTrain) {
        long arrival = feeder.arrival();
        long margin = departure - arrival - query.minTransferTime();
        long feederDelay = Math.max(0, delayOf(feeder.trainId()));

        TransferRisk risk;
        if (sameTrain || margin >= feederDelay + query.transferRiskBuffer()) {
            risk = TransferRisk.SAFE;
        } else if (margin >= feederDelay) {
            risk = TransferRisk.TIGHT;
        } else {
            risk = TransferRisk.RISKY;
        }

        return new RouteTransfer(feeder.to(), arrival, departure, risk, sameTrain);
    }

    private long delayOf(UUID trainId) {
        return delayByTrain.computeIfAbsent(trainId,
            x -> RailwayBackendApi.getTrain(x).map(TrainSnapshot::currentDelay).orElse(0L));
    }
}
