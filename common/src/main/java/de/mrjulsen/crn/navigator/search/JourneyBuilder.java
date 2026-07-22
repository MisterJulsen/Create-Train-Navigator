package de.mrjulsen.crn.navigator.search;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.mrjulsen.crn.backend.api.RailwayBackendApi;
import de.mrjulsen.crn.backend.api.TrainSnapshot;
import de.mrjulsen.crn.backend.timing.StopTimes;
import de.mrjulsen.crn.navigator.NavigationQuery;
import de.mrjulsen.crn.navigator.index.TimetableIndex;
import de.mrjulsen.crn.navigator.index.Trip;
import de.mrjulsen.crn.navigator.index.TripCall;
import de.mrjulsen.crn.navigator.index.TripSection;
import de.mrjulsen.crn.navigator.route.RouteCall;
import de.mrjulsen.crn.navigator.route.RouteJourney;
import de.mrjulsen.crn.navigator.route.RouteLeg;
import de.mrjulsen.crn.navigator.route.RouteTransfer;
import de.mrjulsen.crn.navigator.search.RaptorSearch.Ride;

/**
 * Turns what the search found into what a traveller is shown.
 * <p>
 * The search works in rides - get on this trip here, get off there - which is not quite what a
 * traveller experiences. A single ride can span a point where the train stops being one service and
 * becomes another, which is a leg boundary even though nobody gets out. This splits rides at those
 * points and puts a change between them.
 * <p>
 * How a change is actually doing is not decided here. It follows from the calls either side of it,
 * which go on moving long after the search is over.
 */
public final class JourneyBuilder {

    private final TimetableIndex index;
    private final NavigationQuery query;

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
                transfers.add(connect(false));
            }
            appendLegs(trip, ride, legs, transfers, isCancelled(trip.trainId()));
        }

        return legs.isEmpty() ? null : new RouteJourney(legs, transfers);
    }

    /**
     * Adds one ride to the plan, as one leg per service it is operated as.
     * <p>
     * The station a service hands over at belongs to both of them: it is the terminus the leaving
     * service advertises and the first stop of the one taking over. So it ends one leg and starts the
     * next. The traveller has to change there - one service ends and another begins, whatever it says
     * on the side of the carriage - but since it is the same train standing at the same platform they
     * may simply stay in their seat. Getting off exactly at such a station is not a change at all:
     * the leg arriving there is the whole story, and no leg on the new service is added.
     * <p>
     * A cyclic service hands over to itself. When it starts its next lap it is a new run of the same
     * service, ending and beginning at the same stop, and that is a change like any other - which is
     * why the section alone does not decide this: on a single-section journey it never changes.
     */
    private void appendLegs(Trip trip, Ride ride, List<RouteLeg> legs, List<RouteTransfer> transfers, boolean cancelled) {
        int start = ride.boardCall();
        TripSection section = trip.call(start).section();

        for (int i = start + 1; i <= ride.alightCall(); i++) {
            TripCall call = trip.call(i);
            if (call.section() == section && !call.startsNewLap(trip.call(i - 1))) {
                continue;
            }
            legs.add(slice(trip, section, start, i, cancelled));
            transfers.add(connect(true));
            start = i;
            section = call.section();
        }

        if (start < ride.alightCall()) {
            legs.add(slice(trip, section, start, ride.alightCall(), cancelled));
        } else if (!transfers.isEmpty()) {
            transfers.remove(transfers.size() - 1);
        }
    }

    /** The stretch of a trip between two of its calls, as one leg of the given service. */
    private static RouteLeg slice(Trip trip, TripSection section, int from, int to, boolean cancelled) {
        List<RouteCall> calls = new ArrayList<>(to - from + 1);
        for (int i = from; i <= to; i++) {
            calls.add(toRouteCall(trip.call(i), trip.cycle(), i == from, i == to));
        }

        String title = trip.call(from).title();
        String destination = title == null || title.isBlank() ? section.destination().name() : title;
        return new RouteLeg(trip.trainId(), trip.sessionId(), trip.trainName(), trip.iconId(),
            section.line(), section.category(), destination, section.sectionIndex(), cancelled, calls);
    }

    /**
     * A call as the traveller experiences it. Boarding, they do not care when the train got in, and
     * alighting they do not care when it leaves again, so those times are collapsed - which also
     * keeps a leg's own duration from including time spent standing at either end.
     */
    private static RouteCall toRouteCall(TripCall call, int cycle, boolean boarding, boolean alighting) {
        long arrival = boarding ? call.departure() : call.arrival();
        long departure = alighting ? call.arrival() : call.departure();
        StopTimes realtime = new StopTimes(arrival, Math.max(arrival, departure), Math.max(arrival, departure));
        return new RouteCall(call.scheduledStation(), call.station(), call.entryIndex(), call.visits() + cycle,
            call.scheduled(), realtime);
    }

    /**
     * A change of trains under the terms this search was run with. How it is actually doing follows
     * from the two calls it ends up between, so nothing is rated here - see
     * {@link RouteTransfer#state()}.
     */
    public RouteTransfer connect(boolean staysSeated) {
        return new RouteTransfer(query.minTransferTime(), query.transferRiskBuffer(), staysSeated);
    }

    private boolean isCancelled(UUID trainId) {
        return RailwayBackendApi.getTrain(trainId).map(TrainSnapshot::isCancelled).orElse(false);
    }
}
