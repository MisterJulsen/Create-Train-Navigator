package de.mrjulsen.crn.navigator.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.mrjulsen.crn.navigator.NavigationQuery;
import de.mrjulsen.crn.navigator.index.TimetableIndex;
import de.mrjulsen.crn.navigator.index.TimetableIndex.Boarding;
import de.mrjulsen.crn.navigator.index.Trip;
import de.mrjulsen.crn.navigator.index.TripCall;

/**
 * The route search itself: given a station, a time and a destination, which trains to take.
 *
 * <h2>How it works</h2>
 * The search proceeds in rounds, and a round is a change of trains. Round one finds everywhere
 * reachable without changing, round two everywhere reachable with one change, and so on. Each round
 * takes the stations found by the previous one, boards every train leaving them from that moment,
 * rides each to the end and notes wherever it gets somewhere earlier than anything found so far.
 * <p>
 * That structure is what makes this fast and what makes its answers sensible. The number of rounds
 * is the number of changes, so it is bounded by what the traveller asked for rather than by the size
 * of the network, and every round is a single sweep over the departures that are actually relevant.
 * <p>
 * It also means the result is not one route but a set of them, one per number of changes, each the
 * earliest arrival achievable with that many. A route with more changes is only ever kept if it
 * genuinely gets the traveller there earlier, so nothing pointless is offered.
 *
 * <h2>Why not a shortest path over a map of the network</h2>
 * Weighing stations by average travel time and finding a shortest path answers "which way round is
 * shortest", which is not the question. Trains only leave at particular moments, so a slightly
 * longer way with a connection waiting can beat a shorter one with an hour's wait, and a path chosen
 * without looking at the times may turn out not to be travellable at all. Searching over departures
 * from the start avoids both, and never has to backtrack.
 */
public final class RaptorSearch {

    /** Stands for "not reached", and is deliberately far beyond any real time. */
    static final long UNREACHABLE = Long.MAX_VALUE;

    /**
     * One ride within a found route: which trip was boarded, where, and where it was left.
     *
     * @param fromNode   The station node the traveller boarded at.
     * @param trip       The position of the trip in the index.
     * @param boardCall  The index of the call boarded at within that trip.
     * @param alightCall The index of the call alighted at.
     */
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

    /**
     * Every worthwhile way from one station to another leaving at or after the given time, as one
     * chain of rides per number of changes.
     * <p>
     * The chains come back in order of increasing changes and strictly decreasing arrival, which is
     * exactly the set worth showing a traveller: anything left out either arrives later than an
     * alternative with fewer changes, or needs more changes to arrive no earlier.
     */
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

    /**
     * The trips worth riding this round, each with the earliest call it can be boarded at, in the
     * order they leave.
     * <p>
     * A trip reachable from several stations is only ridden once, from whichever of them lets the
     * traveller get on soonest, since boarding earlier covers everything boarding later would.
     * Riding them in departure order matters: the earliest of them establishes an arrival at the
     * destination straight away, after which every later trip that cannot beat it is dismissed the
     * moment it is looked at.
     * <p>
     * That earliest boarding is what the search needs, not what the traveller wants to be told; the
     * two are reconciled by {@link #boardAsLateAsPossible} once a route has been found.
     */
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

    /**
     * The chains of rides leading to the destination, one per round that improved on all before it.
     */
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

    /**
     * The same chain of rides, but boarding each train as late as the traveller possibly can.
     * <p>
     * The search boards every train at the first opportunity, because for the question it is
     * answering - what can be reached, and when - getting on earlier covers everything getting on
     * later would. For the traveller the two are not the same at all. A station node is a whole
     * station tag, so a train that serves one platform of it, goes round its loop and comes back to
     * another platform offers two boardings at what the search considers one place; taking the first
     * of them means riding a full lap to arrive back where you started.
     * <p>
     * Nothing about the journey changes here except where the traveller gets on: same train, same
     * stop, same arrival - only later, and without the pointless lap. Which also means a route that
     * used to differ from a sensible one only by that lap now becomes identical to it and is dropped
     * as a duplicate rather than offered as an alternative.
     */
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

    /** How many trips this search has ridden, for diagnostics. */
    public int getTripsScanned() {
        return tripsScanned;
    }
}
