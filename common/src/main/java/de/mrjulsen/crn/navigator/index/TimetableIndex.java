package de.mrjulsen.crn.navigator.index;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.mrjulsen.crn.backend.api.JourneySnapshot;
import de.mrjulsen.crn.backend.api.RailwayBackendApi;
import de.mrjulsen.crn.backend.api.SectionSnapshot;
import de.mrjulsen.crn.backend.api.StationRef;
import de.mrjulsen.crn.backend.api.StopSnapshot;
import de.mrjulsen.crn.backend.api.TrainSnapshot;
import de.mrjulsen.crn.data.train.TrainUtils;

/**
 * The whole network's departures over a stretch of time, in the shape a route search needs them.
 * <p>
 * The backend describes one train at a time: where it is, where it goes next, when it gets there. A
 * route search asks the opposite question - which trains leave this station after this moment - and
 * answering that by walking every train per query is what makes a search on a large network slow.
 * This index turns the data round once, so the search can look boarding opportunities up rather than
 * search for them.
 *
 * <h2>What a node is</h2>
 * Stations that share a {@linkplain de.mrjulsen.crn.data.StationTag station tag} are one node here,
 * because a traveller can change between them without travelling. A station without a tag is its own
 * node. This is what makes a change of trains between two platforms of the same station free.
 *
 * <h2>Time</h2>
 * The index covers a window, and cyclic trains are unrolled into one trip per cycle falling in it.
 * That is a projection, not a measurement - see
 * {@link de.mrjulsen.crn.backend.timing.CycleProjector} for what it is worth. Everything outside the
 * window is simply not there, so a search never runs away into the far future.
 *
 * <h2>Cost</h2>
 * Building this is the expensive part of a search, so the last one is kept and handed out again to
 * any query it still covers, until the backend has moved on. Filters from a query are deliberately
 * <em>not</em> applied here - they are applied while searching - so one index serves every traveller.
 *
 * <h2>Threading</h2>
 * Immutable once built, so it may be read from any thread, and the cache is synchronised.
 * <p>
 * Building one is <b>server thread only</b>. Nothing here needs the live trains - only their
 * schedules and times - but the listing it is built from hands back a position for every train,
 * which reads their carriages. Should that listing ever gain a form that leaves the position out,
 * this becomes safe to build anywhere, and cheaper besides.
 */
public final class TimetableIndex {

    /** How long a built index may be handed out again, in ticks. Matches the backend update rate. */
    private static final long MAX_AGE = 100;

    /** The most cycles a single train is unrolled into, whatever the window would allow. */
    private static final int MAX_CYCLES_PER_TRAIN = 16;

    /** One opportunity to get on a trip: the trip, and which of its calls to board at. */
    public record Boarding(int trip, int call, long departure) {}

    private static final Object CACHE_LOCK = new Object();
    private static TimetableIndex cached;

    private final List<StationRef> stations;
    private final Map<String, Integer> nodeByKey;
    private final Map<String, Integer> nodeByStation;
    private final Map<String, Integer> nodeByTagName;
    private final List<Trip> trips;
    private final List<List<Boarding>> boardings;
    private final long from;
    private final long until;
    private final long builtAt;
    private final long buildDurationMs;

    private TimetableIndex(List<StationRef> stations, Map<String, Integer> nodeByKey,
                           Map<String, Integer> nodeByStation, Map<String, Integer> nodeByTagName,
                           List<Trip> trips, List<List<Boarding>> boardings,
                           long from, long until, long builtAt, long buildDurationMs) {
        this.stations = stations;
        this.nodeByKey = nodeByKey;
        this.nodeByStation = nodeByStation;
        this.nodeByTagName = nodeByTagName;
        this.trips = trips;
        this.boardings = boardings;
        this.from = from;
        this.until = until;
        this.builtAt = builtAt;
        this.buildDurationMs = buildDurationMs;
    }

    /**
     * An index covering the given window, building one only if the last one does not already cover
     * it and is still current.
     */
    public static TimetableIndex obtain(long from, long until) {
        synchronized (CACHE_LOCK) {
            long now = RailwayBackendApi.currentTime();
            TimetableIndex index = cached;
            if (index != null && index.covers(from, until) && Math.abs(now - index.builtAt) <= MAX_AGE) {
                return index;
            }
            cached = build(from, until);
            return cached;
        }
    }

    /** Drops the cached index, so the next search builds a fresh one. */
    public static void invalidate() {
        synchronized (CACHE_LOCK) {
            cached = null;
        }
    }

    /** Builds an index covering the given window from the backend, bypassing the cache. */
    public static TimetableIndex build(long from, long until) {
        long startedAt = System.currentTimeMillis();
        Builder builder = new Builder(from, until);

        for (TrainSnapshot train : RailwayBackendApi.getAllTrains()) {
            if (!train.isUsable() || train.isCancelled()) {
                continue;
            }
            RailwayBackendApi.getJourney(train.trainId()).ifPresent(journey -> builder.add(train, journey));
        }

        return builder.finish(RailwayBackendApi.currentTime(), System.currentTimeMillis() - startedAt);
    }

    /** Whether this index covers the whole of the given window. */
    public boolean covers(long from, long until) {
        return this.from <= from && this.until >= until;
    }

    /** The station a node stands for. Stations sharing a tag report the first one seen. */
    public StationRef station(int node) {
        return node < 0 || node >= stations.size() ? StationRef.NONE : stations.get(node);
    }

    /** The trip at the given position. */
    public Trip trip(int index) {
        return trips.get(index);
    }

    /** Every opportunity to board at a node, ordered by departure. */
    public List<Boarding> boardingsAt(int node) {
        return node < 0 || node >= boardings.size() ? List.of() : boardings.get(node);
    }

    /**
     * The position of the first boarding at a node departing at or after the given time, or the size
     * of the list if there is none.
     */
    public int firstBoardingAtOrAfter(int node, long time) {
        List<Boarding> list = boardingsAt(node);
        int low = 0;
        int high = list.size();
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (list.get(mid).departure() < time) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    /**
     * The node a traveller means by the given text: a station name, a station tag name, or a filter
     * containing wildcards. Returns {@code -1} if nothing in the network matches.
     */
    public int resolveNode(String stationOrTagName) {
        if (stationOrTagName == null || stationOrTagName.isBlank()) {
            return -1;
        }
        String name = stationOrTagName.trim();

        Integer exact = nodeByStation.get(name);
        if (exact != null) {
            return exact;
        }
        Integer byTag = nodeByTagName.get(name.toLowerCase());
        if (byTag != null) {
            return byTag;
        }
        for (Map.Entry<String, Integer> entry : nodeByStation.entrySet()) {
            if (TrainUtils.stationMatches(entry.getKey(), name)) {
                return entry.getValue();
            }
        }
        return -1;
    }

    /** How many station nodes the index knows. */
    public int nodeCount() {
        return stations.size();
    }

    /** How many travel opportunities the index holds. */
    public int tripCount() {
        return trips.size();
    }

    /** The earliest time this index covers, in transformed game ticks. */
    public long from() {
        return from;
    }

    /** The latest time this index covers, in transformed game ticks. */
    public long until() {
        return until;
    }

    /** When this index was built, in transformed game ticks. */
    public long builtAt() {
        return builtAt;
    }

    /** How long building this index took, in milliseconds. */
    public long buildDurationMs() {
        return buildDurationMs;
    }

    /**
     * Turns the backend's per-train view into the index's per-station one.
     * <p>
     * The work of one build, kept apart from the finished index so the index itself has no mutable
     * state to hide.
     */
    private static final class Builder {

        private final long from;
        private final long until;

        private final List<StationRef> stations = new ArrayList<>();
        private final Map<String, Integer> nodeByKey = new HashMap<>();
        private final Map<String, Integer> nodeByStation = new HashMap<>();
        private final Map<String, Integer> nodeByTagName = new HashMap<>();
        private final List<Trip> trips = new ArrayList<>();

        private Builder(long from, long until) {
            this.from = from;
            this.until = until;
        }

        /** The usable stretch of one train's journey, before it is unrolled into cycles. */
        private record Template(List<TripCall> calls, int boardableCalls) {}

        private void add(TrainSnapshot train, JourneySnapshot journey) {
            Template template = buildTemplate(journey);
            if (template == null) {
                return;
            }

            Trip base = new Trip(train.trainId(), train.sessionId(), train.trainName(),
                train.displayName(), 0, template.calls(), template.boardableCalls());

            if (!journey.repeats()) {
                addTrip(base);
                return;
            }

            long cycleDuration = journey.totalDuration();
            for (int cycle = 0; cycle < MAX_CYCLES_PER_TRAIN; cycle++) {
                long shift = cycle * cycleDuration;
                if (base.firstDeparture() + shift > until) {
                    break;
                }
                addTrip(cycle == 0 ? base : base.shifted(shift, cycle));
            }
        }

        private void addTrip(Trip trip) {
            if (trip.isUsable() && trip.lastArrival() >= from && trip.boardableCalls() > 0) {
                trips.add(trip);
            }
        }

        /**
         * The stretch of a journey a traveller can actually ride, as one trip of the current cycle.
         * <p>
         * A cyclic journey is laid out twice over, so that a ride starting near the end of a cycle can
         * still reach a station lying beyond the point where the journey wraps around. The longest
         * uninterrupted run of usable calls in that layout is what gets offered; a stretch closed to
         * passengers, a stop without times, or times that fail to increase all end a run. Taking the
         * longest run rather than the first makes the result independent of where the train happens
         * to be standing right now.
         */
        private Template buildTemplate(JourneySnapshot journey) {
            List<StopSnapshot> ordered = journey.upcomingStops();
            if (ordered.isEmpty()) {
                ordered = journey.stops();
            }
            int cycleLength = ordered.size();
            if (cycleLength < 2) {
                return null;
            }

            Map<Integer, SectionSnapshot> sections = new HashMap<>();
            for (SectionSnapshot section : journey.sections()) {
                sections.put(section.index(), section);
            }
            Map<Integer, TripSection> tripSections = new HashMap<>();

            boolean repeats = journey.repeats();
            int length = repeats ? cycleLength * 2 : cycleLength;
            TripCall[] laid = new TripCall[length];

            for (int i = 0; i < length; i++) {
                StopSnapshot stop = ordered.get(i % cycleLength);
                if (!stop.hasTimes() || !isServiceable(stop, journey, sections)) {
                    continue;
                }
                long shift = (i / cycleLength) * journey.totalDuration();
                TripSection section = tripSections.computeIfAbsent(stop.sectionIndex(),
                    x -> toTripSection(x, sections.get(x)));
                laid[i] = new TripCall(
                    nodeOf(stop.station()),
                    stop.station(),
                    stop.entryIndex(),
                    stop.scheduled().shifted(shift),
                    stop.realtime().arrival() + shift,
                    stop.realtime().departure() + shift,
                    stop.completedVisits(),
                    section,
                    stop.title()
                );
            }

            int bestStart = -1;
            int bestLength = 0;
            int runStart = -1;
            for (int i = 0; i < length; i++) {
                boolean breaks = laid[i] == null
                    || (runStart >= 0 && (laid[i].arrival() < laid[i - 1].departure() || !ridesThrough(laid[i - 1], laid[i])));
                if (breaks) {
                    runStart = laid[i] == null ? -1 : i;
                } else if (runStart < 0) {
                    runStart = i;
                }
                if (runStart >= 0 && i - runStart + 1 > bestLength) {
                    bestStart = runStart;
                    bestLength = i - runStart + 1;
                }
            }

            if (bestLength < 2) {
                return null;
            }
            List<TripCall> calls = new ArrayList<>(bestLength);
            for (int i = bestStart; i < bestStart + bestLength; i++) {
                calls.add(laid[i]);
            }
            return new Template(calls, cycleLength - bestStart);
        }

        /**
         * Whether a traveller on board at one call is still on board at the next.
         * <p>
         * Within a section they always are. Across a section boundary only if the section being left
         * declares that it still covers the following one's first stop - otherwise the train changes
         * role there and carries nobody over.
         */
        private static boolean ridesThrough(TripCall from, TripCall to) {
            return from.section() == to.section() || from.section().includesNextSectionStart();
        }

        /**
         * Whether a traveller may use a stop at all.
         * <p>
         * A stop belonging to a section that is not for public use is out, unless it is the first
         * stop of that section and the previous, usable section still advertises it - which is the
         * same rule the departure boards apply.
         */
        private static boolean isServiceable(StopSnapshot stop, JourneySnapshot journey, Map<Integer, SectionSnapshot> sections) {
            if (stop.sectionUsable()) {
                return true;
            }
            if (!stop.firstStopOfSection() || sections.size() < 2) {
                return false;
            }
            int previousIndex = Math.floorMod(stop.sectionIndex() - 1, journey.sections().size());
            SectionSnapshot previous = sections.get(previousIndex);
            return previous != null && previous.usable() && previous.includesNextSectionStart();
        }

        private static TripSection toTripSection(int index, SectionSnapshot section) {
            if (section == null) {
                return new TripSection(index, null, null, StationRef.NONE, false);
            }
            return new TripSection(index, section.line(), section.category(),
                section.destination(), section.includesNextSectionStart());
        }

        private int nodeOf(StationRef station) {
            String key = station.hasTag()
                ? "#" + station.tag().getId()
                : "@" + station.name();

            Integer existing = nodeByKey.get(key);
            if (existing != null) {
                nodeByStation.putIfAbsent(station.name(), existing);
                return existing;
            }

            int node = stations.size();
            stations.add(station);
            nodeByKey.put(key, node);
            nodeByStation.put(station.name(), node);
            if (station.hasTag()) {
                for (String member : station.tag().getAllStationNames()) {
                    nodeByStation.putIfAbsent(member, node);
                }
                nodeByTagName.putIfAbsent(station.tagName().toLowerCase(), node);
            }
            return node;
        }

        private TimetableIndex finish(long builtAt, long buildDurationMs) {
            List<List<Boarding>> boardings = new ArrayList<>(stations.size());
            for (int i = 0; i < stations.size(); i++) {
                boardings.add(new ArrayList<>());
            }

            for (int tripIndex = 0; tripIndex < trips.size(); tripIndex++) {
                Trip trip = trips.get(tripIndex);
                for (int call = 0; call < trip.boardableCalls(); call++) {
                    TripCall tripCall = trip.call(call);
                    if (tripCall.departure() < from || tripCall.departure() > until) {
                        continue;
                    }
                    boardings.get(tripCall.node()).add(new Boarding(tripIndex, call, tripCall.departure()));
                }
            }

            List<List<Boarding>> sorted = new ArrayList<>(boardings.size());
            for (List<Boarding> list : boardings) {
                list.sort((a, b) -> Long.compare(a.departure(), b.departure()));
                sorted.add(List.copyOf(list));
            }

            return new TimetableIndex(List.copyOf(stations), Map.copyOf(nodeByKey),
                Map.copyOf(nodeByStation), Map.copyOf(nodeByTagName), List.copyOf(trips),
                List.copyOf(sorted), from, until, builtAt, buildDurationMs);
        }
    }
}
