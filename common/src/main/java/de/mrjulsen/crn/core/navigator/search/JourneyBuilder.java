package de.mrjulsen.crn.core.navigator.search;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.api.core.snapshot.TrainSnapshot;
import de.mrjulsen.crn.core.timing.StopTimes;
import de.mrjulsen.crn.core.navigator.NavigationQuery;
import de.mrjulsen.crn.core.navigator.index.TimetableIndex;
import de.mrjulsen.crn.core.navigator.index.Trip;
import de.mrjulsen.crn.core.navigator.index.TripCall;
import de.mrjulsen.crn.core.navigator.index.TripSection;
import de.mrjulsen.crn.core.navigator.route.RouteCall;
import de.mrjulsen.crn.core.navigator.route.RouteJourney;
import de.mrjulsen.crn.core.navigator.route.RouteLeg;
import de.mrjulsen.crn.core.navigator.route.RouteTransfer;
import de.mrjulsen.crn.core.navigator.search.RaptorSearch.Ride;

public final class JourneyBuilder {

    private final TimetableIndex index;
    private final NavigationQuery query;

    public JourneyBuilder(TimetableIndex index, NavigationQuery query) {
        this.index = index;
        this.query = query;
    }

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

    private static RouteLeg slice(Trip trip, TripSection section, int from, int to, boolean cancelled) {
        List<RouteCall> calls = new ArrayList<>(to - from + 1);
        for (int i = from; i <= to; i++) {
            calls.add(toRouteCall(trip.call(i), trip.cycle(), i == from, i == to));
        }

        String title = trip.call(from).title();
        String destination = title == null || title.isBlank() ? section.destination().displayName() : title;
        return new RouteLeg(trip.trainId(), trip.sessionId(), trip.trainName(), trip.iconId(),
            section.line(), section.category(), destination, section.sectionIndex(), cancelled, calls);
    }

    private static RouteCall toRouteCall(TripCall call, int cycle, boolean boarding, boolean alighting) {
        long arrival = boarding ? call.departure() : call.arrival();
        long departure = alighting ? call.arrival() : call.departure();
        StopTimes realtime = new StopTimes(arrival, Math.max(arrival, departure), Math.max(arrival, departure));
        return new RouteCall(call.scheduledStation(), call.station(), call.entryIndex(), call.visits() + cycle,
            call.scheduled(), realtime);
    }

    public RouteTransfer connect(boolean staysSeated) {
        return new RouteTransfer(query.minTransferTime(), query.transferRiskBuffer(), staysSeated);
    }

    private boolean isCancelled(UUID trainId) {
        return RailwayBackendApi.getTrain(trainId).map(TrainSnapshot::isCancelled).orElse(false);
    }
}
