package de.mrjulsen.crn.core.navigator.index;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.mrjulsen.crn.api.core.ref.TrainCategoryRef;
import de.mrjulsen.crn.api.core.snapshot.JourneySnapshot;
import de.mrjulsen.crn.api.core.ref.LineRef;
import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.api.core.snapshot.SectionSnapshot;
import de.mrjulsen.crn.api.core.ref.StationRef;
import de.mrjulsen.crn.api.core.snapshot.StopSnapshot;
import de.mrjulsen.crn.api.core.snapshot.TrainSnapshot;
import de.mrjulsen.crn.core.util.StationLookup;
import de.mrjulsen.crn.data.settings.StationTag;
import de.mrjulsen.crn.util.TrainUtils;

public final class TimetableIndex {

    private static final long MAX_AGE = 100;

    /** A boarding opportunity: the {@code call}-th call of {@code trip}, catchable in any future cycle. */
    public record Boarding(int trip, int call) {}

    private static final Object CACHE_LOCK = new Object();
    private static TimetableIndex cached;

    private final List<StationRef> stations;
    private final Map<String, Integer> nodeByKey;
    private final Map<String, Integer> nodeByStation;
    private final Map<String, Integer> nodeByTagName;
    private final List<Trip> trips;
    private final List<List<Boarding>> boardings;
    private final long builtAt;
    private final long buildDurationMs;

    private TimetableIndex(List<StationRef> stations, Map<String, Integer> nodeByKey,
                           Map<String, Integer> nodeByStation, Map<String, Integer> nodeByTagName,
                           List<Trip> trips, List<List<Boarding>> boardings,
                           long builtAt, long buildDurationMs) {
        this.stations = stations;
        this.nodeByKey = nodeByKey;
        this.nodeByStation = nodeByStation;
        this.nodeByTagName = nodeByTagName;
        this.trips = trips;
        this.boardings = boardings;
        this.builtAt = builtAt;
        this.buildDurationMs = buildDurationMs;
    }

    /**
     * The current index, reused while it is fresh. The index is time-independent - it holds each
     * train's cycle once and lets the search project boardings analytically - so a single build
     * serves every query at every time until the schedules themselves move on.
     */
    public static TimetableIndex obtain(long now) {
        synchronized (CACHE_LOCK) {
            TimetableIndex index = cached;
            if (index != null && Math.abs(now - index.builtAt) <= MAX_AGE) {
                return index;
            }
            cached = build(now);
            return cached;
        }
    }

    /**
     * Rebuilds the cache only if it is already in use, so an idle server never pays for it. Meant to
     * be called from the backend worker after a full update, keeping searches off the server thread.
     */
    public static void refreshIfWarm(long now) {
        synchronized (CACHE_LOCK) {
            if (cached != null) {
                cached = build(now);
            }
        }
    }

    public static void invalidate() {
        synchronized (CACHE_LOCK) {
            cached = null;
        }
    }

    public static TimetableIndex build(long now) {
        long startedAt = System.currentTimeMillis();
        Builder builder = new Builder(now);

        for (TrainSnapshot train : RailwayBackendApi.getAllTrains()) {
            if (!train.isUsable() || train.isCancelled()) {
                continue;
            }
            RailwayBackendApi.getJourney(train.trainId()).ifPresent(journey -> builder.add(train, journey));
        }

        return builder.finish(RailwayBackendApi.getCurrentTime(), System.currentTimeMillis() - startedAt);
    }

    public StationRef station(int node) {
        return node < 0 || node >= stations.size() ? StationRef.NONE : stations.get(node);
    }

    public Trip trip(int index) {
        return trips.get(index);
    }

    public List<Boarding> boardingsAt(int node) {
        return node < 0 || node >= boardings.size() ? List.of() : boardings.get(node);
    }

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

        String best = null;
        for (Map.Entry<String, Integer> entry : nodeByStation.entrySet()) {
            if (TrainUtils.stationMatches(entry.getKey(), name) && (best == null || entry.getKey().compareTo(best) < 0)) {
                best = entry.getKey();
            }
        }
        return best == null ? -1 : nodeByStation.get(best);
    }

    public int nodeCount() {
        return stations.size();
    }

    public int tripCount() {
        return trips.size();
    }

    public long builtAt() {
        return builtAt;
    }

    public long buildDurationMs() {
        return buildDurationMs;
    }

    private static final class Builder {

        private final long now;

        private final List<StationRef> stations = new ArrayList<>();
        private final Map<String, Integer> nodeByKey = new HashMap<>();
        private final Map<String, Integer> nodeByStation = new HashMap<>();
        private final Map<String, Integer> nodeByTagName = new HashMap<>();
        private final List<Trip> trips = new ArrayList<>();

        private Builder(long now) {
            this.now = now;
        }

        private record Template(List<TripCall> calls, int boardableCalls) {}

        private void add(TrainSnapshot train, JourneySnapshot journey) {
            long period = journey.repeats() ? journey.totalDuration() : 0;
            for (Template template : buildTemplates(journey)) {
                addTrip(new Trip(train.trainId(), train.sessionId(), train.trainName(),
                    train.displayName(), train.iconId(), period, template.calls(), template.boardableCalls()));
            }
        }

        private void addTrip(Trip trip) {
            if (!trip.isUsable() || trip.boardableCalls() <= 0) {
                return;
            }
            // A repeating trip can always be caught in some future cycle; a one-off only while it has not fully run.
            if (!trip.repeats() && trip.lastArrival() < now) {
                return;
            }
            trips.add(trip);
        }

        private List<Template> buildTemplates(JourneySnapshot journey) {
            List<StopSnapshot> ordered = journey.upcomingStops();
            if (ordered.isEmpty()) {
                ordered = journey.stops();
            }
            int cycleLength = ordered.size();
            if (cycleLength < 2) {
                return List.of();
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
                int lap = i / cycleLength;
                long shift = lap * journey.totalDuration();
                TripSection section = tripSections.computeIfAbsent(stop.sectionIndex(),
                    x -> toTripSection(x, sections.get(x)));
                laid[i] = new TripCall(
                    nodeOf(stop.station()),
                    stop.station(),
                    stop.scheduledStation(),
                    stop.entryIndex(),
                    stop.stopIndex(),
                    stop.scheduled().shifted(shift),
                    stop.realtime().arrival() + shift,
                    stop.realtime().departure() + shift,
                    stop.completedVisits() + lap,
                    section,
                    stop.title()
                );
            }

            List<Template> templates = new ArrayList<>();
            int runStart = -1;
            for (int i = 0; i < length; i++) {
                if (laid[i] == null) {
                    addTemplate(templates, laid, runStart, i, cycleLength);
                    runStart = -1;
                    continue;
                }
                if (runStart < 0) {
                    runStart = i;
                } else if (laid[i].arrival() < laid[i - 1].departure() || !ridesThrough(laid[i - 1], laid[i])) {
                    addTemplate(templates, laid, runStart, i, cycleLength);
                    runStart = i;
                }
            }
            addTemplate(templates, laid, runStart, length, cycleLength);
            return templates;
        }

        private static void addTemplate(List<Template> templates, TripCall[] laid, int start, int end, int cycleLength) {
            if (start < 0 || end - start < 2) {
                return;
            }
            int boardable = Math.min(end - start, cycleLength - start);
            if (boardable <= 0) {
                return;
            }
            List<TripCall> calls = new ArrayList<>(end - start);
            for (int i = start; i < end; i++) {
                calls.add(laid[i]);
            }
            templates.add(new Template(calls, boardable));
        }

        private static boolean ridesThrough(TripCall from, TripCall to) {
            if (to.startsNewLap(from)) {
                return from.section().includesNextSectionStart();
            }
            return from.section() == to.section() || from.section().includesNextSectionStart();
        }

        private static boolean isServiceable(StopSnapshot stop, JourneySnapshot journey, Map<Integer, SectionSnapshot> sections) {
            SectionSnapshot section = sections.get(stop.sectionIndex());
            if (section == null || section.usable()) {
                return true;
            }
            boolean firstStopOfSection = !section.stops().isEmpty()
                && section.stops().get(0).stopIndex() == stop.stopIndex();
            if (!firstStopOfSection || sections.size() < 2) {
                return false;
            }
            if (stop.sectionIndex() == 0 && !journey.cyclic()) {
                return false;
            }
            int previousIndex = Math.floorMod(stop.sectionIndex() - 1, journey.sections().size());
            SectionSnapshot previous = sections.get(previousIndex);
            return previous != null && previous.usable() && previous.includesNextSectionStart();
        }

        private static TripSection toTripSection(int index, SectionSnapshot section) {
            if (section == null) {
                return new TripSection(index, LineRef.NONE, TrainCategoryRef.NONE, StationRef.NONE, false);
            }
            return new TripSection(index, section.line(), section.category(),
                section.destination(), section.includesNextSectionStart());
        }

        private int nodeOf(StationRef station) {
            String key = station.hasTag()
                ? "#" + station.tagId()
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
                StationTag tag = StationLookup.findTag(station.name());
                if (tag != null) {
                    for (String member : tag.getAllStationNames()) {
                        nodeByStation.putIfAbsent(member, node);
                    }
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
                    boardings.get(trip.call(call).node()).add(new Boarding(tripIndex, call));
                }
            }

            List<List<Boarding>> byNode = new ArrayList<>(boardings.size());
            for (List<Boarding> list : boardings) {
                byNode.add(List.copyOf(list));
            }

            return new TimetableIndex(List.copyOf(stations), Map.copyOf(nodeByKey),
                Map.copyOf(nodeByStation), Map.copyOf(nodeByTagName), List.copyOf(trips),
                List.copyOf(byNode), builtAt, buildDurationMs);
        }
    }
}
