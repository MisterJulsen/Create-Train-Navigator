package de.mrjulsen.crn.core.navigator.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.mrjulsen.crn.core.navigator.NavigationQuery;
import de.mrjulsen.crn.core.navigator.index.TimetableIndex;
import de.mrjulsen.crn.core.navigator.index.TimetableIndex.Boarding;
import de.mrjulsen.crn.core.navigator.index.Trip;
import de.mrjulsen.crn.core.navigator.index.TripCall;

public final class RaptorSearch {

    static final long UNREACHABLE = Long.MAX_VALUE;

    public record Ride(int fromNode, int trip, int boardCall, int alightCall) {}

    private final TimetableIndex index;
    private final NavigationQuery query;
    private final boolean[] avoided;

    private int tripsScanned;

    public RaptorSearch(TimetableIndex index, NavigationQuery query) {
        this.index = index;
        this.query = query;
        this.avoided = new boolean[index.nodeCount()];

        for (String station : query.avoidedStations()) {
            int node = index.resolveNode(station);
            if (node >= 0) {
                avoided[node] = true;
            }
        }
    }

    public List<List<Ride>> run(int origin, int destination, long departAfter) {
        int nodeCount = index.nodeCount();
        if (origin < 0 || destination < 0 || origin == destination || nodeCount == 0) {
            return List.of();
        }

        int maxRounds = query.maxLegs();
        long[] best = new long[nodeCount];
        long[][] roundArrival = new long[maxRounds + 1][nodeCount];
        Ride[][] rides = new Ride[maxRounds + 1][nodeCount];

        Arrays.fill(best, UNREACHABLE);
        for (long[] round : roundArrival) {
            Arrays.fill(round, UNREACHABLE);
        }

        boolean[] marked = new boolean[nodeCount];
        int[] markedNodes = new int[nodeCount];
        int markedCount = 1;
        markedNodes[0] = origin;
        marked[origin] = true;

        best[origin] = departAfter;
        roundArrival[0][origin] = departAfter;

        for (int round = 1; round <= maxRounds && markedCount > 0; round++) {
            List<Ride> boardings = collectBoardings(markedNodes, markedCount, roundArrival[round - 1], round);

            for (int i = 0; i < markedCount; i++) {
                marked[markedNodes[i]] = false;
            }
            markedCount = 0;

            for (Ride boarding : boardings) {
                Trip trip = index.trip(boarding.trip());
                if (trip.call(boarding.boardCall()).departure() >= best[destination]) {
                    break;
                }
                tripsScanned++;

                for (int call = boarding.boardCall() + 1; call < trip.size(); call++) {
                    TripCall tripCall = trip.call(call);
                    if (!query.accepts(tripCall.section().line(), tripCall.section().category())) {
                        break;
                    }
                    long arrival = tripCall.arrival();
                    if (arrival >= best[destination]) {
                        break;
                    }
                    int node = tripCall.node();
                    if (arrival >= best[node] || (avoided[node] && node != destination)) {
                        continue;
                    }

                    best[node] = arrival;
                    roundArrival[round][node] = arrival;
                    rides[round][node] = new Ride(boarding.fromNode(), boarding.trip(), boarding.boardCall(), call);
                    if (!marked[node]) {
                        marked[node] = true;
                        markedNodes[markedCount++] = node;
                    }
                }
            }
        }

        return collectResults(rides, roundArrival, destination, maxRounds, departAfter);
    }

    private List<Ride> collectBoardings(int[] markedNodes, int markedCount, long[] previousArrival, int round) {
        Map<Integer, Ride> earliest = new HashMap<>();
        long transferTime = round >= 2 ? query.minTransferTime() : 0;

        for (int i = 0; i < markedCount; i++) {
            int node = markedNodes[i];
            long readyAt = previousArrival[node] + transferTime;
            List<Boarding> available = index.boardingsAt(node);

            for (int b = index.firstBoardingAtOrAfter(node, readyAt); b < available.size(); b++) {
                Boarding boarding = available.get(b);
                Trip trip = index.trip(boarding.trip());
                TripCall call = trip.call(boarding.call());
                if (!query.accepts(call.section().line(), call.section().category())) {
                    continue;
                }
                Ride existing = earliest.get(boarding.trip());
                if (existing != null && existing.boardCall() <= boarding.call()) {
                    continue;
                }
                earliest.put(boarding.trip(), new Ride(node, boarding.trip(), boarding.call(), boarding.call()));
            }
        }

        List<Ride> boardings = new ArrayList<>(earliest.values());
        boardings.sort((a, b) -> Long.compare(
            index.trip(a.trip()).call(a.boardCall()).departure(),
            index.trip(b.trip()).call(b.boardCall()).departure()));
        return boardings;
    }

    private List<List<Ride>> collectResults(Ride[][] rides, long[][] roundArrival, int destination, int maxRounds,
                                            long departAfter) {
        List<List<Ride>> results = new ArrayList<>();
        long bestSoFar = UNREACHABLE;

        for (int round = 1; round <= maxRounds; round++) {
            long arrival = roundArrival[round][destination];
            if (arrival >= bestSoFar) {
                continue;
            }
            List<Ride> chain = reconstruct(rides, round, destination);
            if (chain == null) {
                continue;
            }
            bestSoFar = arrival;
            results.add(boardAsLateAsPossible(chain, departAfter));
        }

        return results;
    }

    private List<Ride> boardAsLateAsPossible(List<Ride> chain, long departAfter) {
        List<Ride> adjusted = new ArrayList<>(chain.size());
        long readyAt = departAfter;

        for (Ride ride : chain) {
            Trip trip = index.trip(ride.trip());
            int node = trip.call(ride.boardCall()).node();
            int board = ride.boardCall();

            for (int call = board + 1; call < ride.alightCall(); call++) {
                TripCall candidate = trip.call(call);
                if (candidate.node() == node && candidate.departure() >= readyAt) {
                    board = call;
                }
            }

            adjusted.add(new Ride(ride.fromNode(), ride.trip(), board, ride.alightCall()));
            readyAt = trip.call(ride.alightCall()).arrival() + query.minTransferTime();
        }

        return adjusted;
    }

    private List<Ride> reconstruct(Ride[][] rides, int round, int destination) {
        Ride[] chain = new Ride[round];
        int node = destination;

        for (int k = round; k >= 1; k--) {
            Ride ride = rides[k][node];
            if (ride == null) {
                return null;
            }
            chain[k - 1] = ride;
            node = ride.fromNode();
        }

        return List.of(chain);
    }

    public int getTripsScanned() {
        return tripsScanned;
    }
}
