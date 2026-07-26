package de.mrjulsen.crn.data.schedule.instruction;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.DiscoveredPath;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.station.GlobalStation;

import de.mrjulsen.crn.util.ModUtils;

final class PriorityChoice {

    enum Skip {
        NO_STATION,
        NO_ROUTE,
        DETOUR,
        STATION_OCCUPIED,
        STATION_RESERVED,
        RED_SIGNAL,
        TRAIN_ON_ROUTE
    }

    record Passed(int index, String filter, Skip reason, String blockedBy) {}

    private record Blocked(Skip reason, String by) {}

    record Result(DiscoveredPath path, int index, List<Passed> passed, boolean waiting, List<String> notes) {

        static Result waiting(List<Passed> passed, List<String> notes) {
            return new Result(null, -1, passed, true, notes);
        }

        static Result none(List<Passed> passed, List<String> notes) {
            return new Result(null, -1, passed, false, notes);
        }

        boolean hasPath() {
            return path != null;
        }
    }

    private static final class Notes {

        private final List<String> lines;

        Notes(boolean enabled) {
            this.lines = enabled ? new ArrayList<>() : null;
        }

        void add(String format, Object... args) {
            if (lines != null) {
                lines.add(String.format(format, args));
            }
        }

        List<String> lines() {
            return lines == null ? List.of() : lines;
        }
    }

    private static final int ROUTE_OBSTRUCTED = 300;

    private PriorityChoice() {}

    static Result select(Train train, PrioritizedDestinationInstruction instruction, boolean explain) {
        TrackGraph graph = train.graph;
        List<String> filters = instruction.getFilters();
        if (graph == null || filters.isEmpty()) {
            return Result.none(List.of(), List.of());
        }

        boolean ignoreTrains = !instruction.shouldAvoidTrains();
        boolean inspect = instruction.shouldAvoidTrains() || instruction.shouldAvoidRedSignals() || explain;

        List<ArrayList<GlobalStation>> byEntry = groupStations(graph, filters);
        RouteScan scan = inspect ? RouteScan.of(train, graph) : null;

        List<Passed> passed = new ArrayList<>();
        Notes notes = new Notes(explain);
        DiscoveredPath fallback = null;

        for (int i = 0; i < byEntry.size(); i++) {
            ArrayList<GlobalStation> stations = byEntry.get(i);
            if (stations.isEmpty()) {
                passed.add(new Passed(i, filters.get(i), Skip.NO_STATION, ""));
                notes.add("%d (%s): no station matches", i + 1, filters.get(i));
                continue;
            }

            DiscoveredPath path = train.navigation.findPathTo(stations, ignoreTrains ? -1 : Double.MAX_VALUE);
            if (path == null || path.destination == null) {
                passed.add(new Passed(i, filters.get(i), Skip.NO_ROUTE, ""));
                notes.add("%d (%s): no route", i + 1, filters.get(i));
                continue;
            }

            DiscoveredPath trainFree = ignoreTrains ? path : unobstructed(train, path);

            if (isDetour(path, trainFree, instruction)) {
                passed.add(new Passed(i, filters.get(i), Skip.DETOUR, ""));
                notes.add("%d (%s) -> %s: detour, %d instead of %d", i + 1, filters.get(i), path.destination.name,
                    (long)Math.abs(path.distance), (long)Math.abs(trainFree.distance));
                continue;
            }

            if (fallback == null) {
                fallback = path;
            }

            Blocked blocked = inspect ? checkBlocked(graph, scan, path, trainFree, instruction, notes) : null;
            if (blocked == null) {
                notes.add("%d (%s) -> %s: taken", i + 1, filters.get(i), path.destination.name);
                return new Result(path, i, passed, false, notes.lines());
            }
            passed.add(new Passed(i, filters.get(i), blocked.reason(), blocked.by()));
        }

        if (instruction.shouldWaitInstead()) {
            return Result.waiting(passed, notes.lines());
        }
        return fallback == null
            ? Result.none(passed, notes.lines())
            : new Result(fallback, -1, passed, false, notes.lines());
    }

    private static List<ArrayList<GlobalStation>> groupStations(TrackGraph graph, List<String> filters) {
        List<Pattern> patterns = filters.stream().map(ModUtils::buildPattern).toList();
        List<ArrayList<GlobalStation>> byEntry = new ArrayList<>(patterns.size());
        for (int i = 0; i < patterns.size(); i++) {
            byEntry.add(new ArrayList<>());
        }

        for (GlobalStation station : graph.getPoints(EdgePointType.STATION)) {
            for (int i = 0; i < patterns.size(); i++) {
                if (patterns.get(i).matcher(station.name).matches()) {
                    byEntry.get(i).add(station);
                }
            }
        }
        return byEntry;
    }

    private static DiscoveredPath unobstructed(Train train, DiscoveredPath path) {
        DiscoveredPath free = train.navigation.findPathTo(path.destination, -1);
        return free != null && Math.signum(free.distance) == Math.signum(path.distance) ? free : null;
    }

    private static boolean isDetour(DiscoveredPath path, DiscoveredPath trainFree,
        PrioritizedDestinationInstruction instruction) {
        int allowance = instruction.getDetourAllowance();
        if (allowance < 0 || trainFree == null) {
            return false;
        }
        return Math.abs(path.distance) - Math.abs(trainFree.distance) > allowance;
    }

    private static Blocked checkBlocked(TrackGraph graph, RouteScan scan, DiscoveredPath path,
        DiscoveredPath trainFree, PrioritizedDestinationInstruction instruction, Notes notes) {

        GlobalStation destination = path.destination;
        RouteScan.Occupancy at = scan.at(graph, destination);

        long obstruction = penaltyOf(path);
        long harmless = trainFree == null ? -1 : penaltyOf(trainFree);
        long onRoute = harmless < 0
            ? (obstruction >= ROUTE_OBSTRUCTED ? obstruction : 0)
            : Math.max(0, obstruction - harmless);

        notes.add("%s: dist=%d ref=%d penalty=%d harmless=%d onRoute=%d present=%s onTrack=%s inbound=%s heldRed=%s avoidTrains=%s avoidSignals=%s",
            destination.name, (long)Math.abs(path.distance),
            trainFree == null ? -1L : (long)Math.abs(trainFree.distance), obstruction, harmless, onRoute,
            nameOf(at.present()), nameOf(at.onTrack()), nameOf(at.inbound()),
            at.heldRed(), instruction.shouldAvoidTrains(), instruction.shouldAvoidRedSignals()
        );

        if (instruction.shouldAvoidTrains()) {
            if (at.present() != null) {
                return new Blocked(Skip.STATION_OCCUPIED, at.present().name.getString());
            }
            if (at.onTrack() != null) {
                return new Blocked(Skip.STATION_OCCUPIED, at.onTrack().name.getString());
            }
            if (at.inbound() != null) {
                return new Blocked(Skip.STATION_RESERVED, at.inbound().name.getString());
            }
            if (onRoute > 0) {
                return new Blocked(Skip.TRAIN_ON_ROUTE, "");
            }
        }
        if (instruction.shouldAvoidRedSignals() && at.heldRed()) {
            return new Blocked(Skip.RED_SIGNAL, "");
        }
        return null;
    }

    private static long penaltyOf(DiscoveredPath path) {
        return Math.max(0, Math.round(path.cost - Math.abs(path.distance)));
    }

    private static String nameOf(Train train) {
        return train == null ? "-" : train.name.getString();
    }

}
