package de.mrjulsen.crn.data.schedule.instruction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.simibubi.create.content.trains.entity.Navigation;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.DiscoveredPath;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.station.GlobalStation;

import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.crn.util.PathCostCorrection;
import de.mrjulsen.crn.util.RedSignalBarrier;

final class PriorityChoice {

    enum Skip {
        NO_STATION,
        NO_ROUTE,
        STATION_OCCUPIED,
        STATION_RESERVED,
        RED_SIGNAL,
        TOO_FAR
    }

    record Passed(Skip reason, String blockedBy) {}

    record Result(DiscoveredPath path, List<Passed> passed, boolean waiting) {

        static Result waiting(List<Passed> passed) {
            return new Result(null, passed, true);
        }

        static Result none(List<Passed> passed) {
            return new Result(null, passed, false);
        }

        boolean hasPath() {
            return path != null;
        }
    }

    private record Blocked(Skip reason, String by) {
        static final Blocked OPEN = new Blocked(null, "");

        boolean free() {
            return reason == null;
        }
    }

    private record Pick(DiscoveredPath path, boolean keepCurrent) {

        static final Pick NOTHING = new Pick(null, false);
        static final Pick KEEP = new Pick(null, true);

        static Pick of(DiscoveredPath path) {
            return new Pick(path, false);
        }
    }

    private final Train train;
    private final Navigation navigation;
    private final TrackGraph graph;
    private final PrioritizedDestinationInstruction instruction;
    private final boolean avoidRedSignals;
    private final RouteScan scan;
    private final double maxCost;
    private final List<ArrayList<GlobalStation>> byPriority;
    private final Map<GlobalStation, Blocked> blockades = new IdentityHashMap<>();
    private final List<Passed> passed = new ArrayList<>();

    private final Map<Integer, Double> alternativesOutside = new HashMap<>();

    private boolean committedResolved;
    private double committed = -1;

    private PriorityChoice(Train train, TrackGraph graph, PrioritizedDestinationInstruction instruction) {
        this.train = train;
        this.navigation = train.navigation;
        this.graph = graph;
        this.instruction = instruction;
        this.avoidRedSignals = instruction.shouldAvoidRedSignals();

        boolean avoidTrains = instruction.shouldAvoidTrains();
        this.scan = avoidTrains ? RouteScan.of(train, graph) : null;
        this.maxCost = avoidTrains ? Double.MAX_VALUE : -1;
        List<String> filters = instruction.getFilters();
        this.byPriority = groupStations(graph, filters);
    }

    static Result select(Train train, PrioritizedDestinationInstruction instruction) {
        TrackGraph graph = train.graph;
        if (graph == null || instruction.getFilters().isEmpty()) {
            return Result.none(List.of());
        }
        return PathCostCorrection.run(() -> new PriorityChoice(train, graph, instruction).decide());
    }

    private Result decide() {
        Pick pick = RedSignalBarrier.run(train, avoidRedSignals, () -> walk(instruction.getDetourAllowance()));

        if (pick.keepCurrent()) {
            return Result.none(passed);
        }
        if (pick.path() != null) {
            return new Result(pick.path(), passed, false);
        }
        if (instruction.shouldWaitInstead()) {
            return Result.waiting(passed);
        }

        Pick fallback = leastBad();
        if (fallback.keepCurrent() || fallback.path() == null) {
            return Result.none(passed);
        }
        return new Result(fallback.path(), passed, false);
    }

    private double reachableDistance(GlobalStation station) {
        ArrayList<GlobalStation> only = new ArrayList<>(1);
        only.add(station);
        DiscoveredPath path = findPathTo(only);
        return path == null || path.destination == null ? -1 : Math.abs(path.distance);
    }

    private Pick walk(int allowance) {
        passed.clear();
        GlobalStation current = navigation.destination;
        boolean comparable = allowance >= 0;

        for (int index = 0; index < byPriority.size(); index++) {
            ArrayList<GlobalStation> stations = byPriority.get(index);
            if (stations.isEmpty()) {
                passed.add(new Passed(Skip.NO_STATION, ""));
                continue;
            }

            Blocked blockade = Blocked.OPEN;
            ArrayList<GlobalStation> open = new ArrayList<>(stations.size());
            for (GlobalStation station : stations) {
                Blocked reason = blockedBy(station);
                if (reason.free()) {
                    open.add(station);
                } else if (blockade.free()) {
                    blockade = reason;
                }
            }

            if (open.isEmpty()) {
                passed.add(new Passed(blockade.reason(), blockade.by()));
                continue;
            }

            DiscoveredPath path = findPathTo(open);
            if (path == null || path.destination == null) {
                passed.add(new Passed(unreachableReason(open), ""));
                continue;
            }
            if (current != null && contains(open, current)) {
                return Pick.KEEP;
            }
            if (comparable && exceedsDetour(path, allowance, index)) {
                passed.add(new Passed(Skip.TOO_FAR, ""));
                continue;
            }
            return Pick.of(path);
        }

        return Pick.NOTHING;
    }

    private Blocked blockedBy(GlobalStation station) {
        if (scan == null) {
            return Blocked.OPEN;
        }
        return blockades.computeIfAbsent(station, x -> {
            RouteScan.Occupancy at = scan.at(graph, x);
            Train occupant = at.present() != null ? at.present() : at.onTrack() != null ? at.onTrack() : at.inBlock();
            if (occupant != null) {
                return new Blocked(Skip.STATION_OCCUPIED, occupant.name.getString());
            }
            if (at.inbound() != null
                && !(x == navigation.destination && RouteScan.hasBetterClaim(train, at.inbound()))) {
                return new Blocked(Skip.STATION_RESERVED, at.inbound().name.getString());
            }
            return Blocked.OPEN;
        });
    }

    private Skip unreachableReason(ArrayList<GlobalStation> open) {
        if (!avoidRedSignals) {
            return Skip.NO_ROUTE;
        }
        DiscoveredPath ignoringSignals = RedSignalBarrier.without(() -> findPathTo(open));
        return ignoringSignals != null && ignoringSignals.destination != null ? Skip.RED_SIGNAL : Skip.NO_ROUTE;
    }

    private boolean exceedsDetour(DiscoveredPath path, int allowance, int entryIndex) {
        double reference = navigation.destination != null ? committedRoute() : alternativesOutside(entryIndex);
        return reference >= 0 && Math.abs(path.distance) > reference + allowance;
    }

    private double committedRoute() {
        if (committedResolved) {
            return committed;
        }
        committedResolved = true;

        ArrayList<GlobalStation> only = new ArrayList<>(1);
        only.add(navigation.destination);
        DiscoveredPath path = RedSignalBarrier.without(() -> findPathTo(only));
        committed = path == null || path.destination == null ? -1 : Math.abs(path.distance);
        if (committed < 0 && navigation.distanceToDestination > 0) {
            committed = navigation.distanceToDestination;
        }
        return committed;
    }

    private double alternativesOutside(int entryIndex) {
        Double known = alternativesOutside.get(entryIndex);
        if (known != null) {
            return known;
        }

        GlobalStation standingAt = train.getCurrentStation();
        Set<GlobalStation> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        ArrayList<GlobalStation> free = new ArrayList<>();
        for (int index = 0; index < byPriority.size(); index++) {
            if (index == entryIndex) {
                continue;
            }
            for (GlobalStation station : byPriority.get(index)) {
                if (station != standingAt && seen.add(station) && blockedBy(station).free()) {
                    free.add(station);
                }
            }
        }

        DiscoveredPath nearest = free.isEmpty() ? null : findPathTo(free);
        double reference = nearest == null ? -1 : Math.abs(nearest.distance);
        alternativesOutside.put(entryIndex, reference);
        return reference;
    }

    private Pick leastBad() {
        GlobalStation current = navigation.destination;
        if (current != null && listed(current) && reachable(current)) {
            return Pick.KEEP;
        }
        int allowance = instruction.getDetourAllowance();
        DiscoveredPath shortest = null;
        for (int index = 0; index < byPriority.size(); index++) {
            ArrayList<GlobalStation> stations = byPriority.get(index);
            if (stations.isEmpty()) {
                continue;
            }
            DiscoveredPath path = findPathTo(stations);
            if (path == null || path.destination == null) {
                continue;
            }
            if (current != null && contains(stations, current)) {
                return Pick.KEEP;
            }
            if (allowance < 0 || !exceedsDetour(path, allowance, index)) {
                return Pick.of(path);
            }
            if (shortest == null || Math.abs(path.distance) < Math.abs(shortest.distance)) {
                shortest = path;
            }
        }
        return shortest == null ? Pick.NOTHING : Pick.of(shortest);
    }

    private boolean listed(GlobalStation station) {
        for (ArrayList<GlobalStation> stations : byPriority) {
            if (contains(stations, station)) {
                return true;
            }
        }
        return false;
    }

    private boolean reachable(GlobalStation station) {
        return reachableDistance(station) >= 0;
    }

    private DiscoveredPath findPathTo(ArrayList<GlobalStation> stations) {
        return navigation.findPathTo(stations, maxCost);
    }

    private static boolean contains(List<GlobalStation> stations, GlobalStation station) {
        for (GlobalStation candidate : stations) {
            if (candidate == station) {
                return true;
            }
        }
        return false;
    }

    private static List<ArrayList<GlobalStation>> groupStations(TrackGraph graph, List<String> filters) {
        List<Pattern> patterns = filters.stream().map(ModUtils::buildPattern).toList();
        List<ArrayList<GlobalStation>> byPriority = new ArrayList<>(patterns.size());
        for (int i = 0; i < patterns.size(); i++) {
            byPriority.add(new ArrayList<>());
        }

        for (GlobalStation station : graph.getPoints(EdgePointType.STATION)) {
            for (int i = 0; i < patterns.size(); i++) {
                if (patterns.get(i).matcher(station.name).matches()) {
                    byPriority.get(i).add(station);
                }
            }
        }
        return byPriority;
    }

}
