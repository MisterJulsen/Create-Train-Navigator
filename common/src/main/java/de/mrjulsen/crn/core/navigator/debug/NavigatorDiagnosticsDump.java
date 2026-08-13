package de.mrjulsen.crn.core.navigator.debug;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.core.RailwayBackend;
import de.mrjulsen.crn.api.core.ref.TrainCategoryRef;
import de.mrjulsen.crn.api.core.snapshot.JourneySnapshot;
import de.mrjulsen.crn.api.core.ref.LineRef;
import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.api.core.snapshot.SectionSnapshot;
import de.mrjulsen.crn.api.core.ref.StationRef;
import de.mrjulsen.crn.api.core.snapshot.StopSnapshot;
import de.mrjulsen.crn.api.core.snapshot.TrainSnapshot;
import de.mrjulsen.crn.core.timing.StopTimes;
import de.mrjulsen.crn.core.navigator.NavigationQuery;
import de.mrjulsen.crn.core.navigator.NavigationResult;
import de.mrjulsen.crn.core.navigator.Navigator;
import de.mrjulsen.crn.core.navigator.index.TimetableIndex;
import de.mrjulsen.crn.core.navigator.index.Trip;
import de.mrjulsen.crn.core.navigator.index.TripCall;
import de.mrjulsen.crn.core.navigator.route.RouteCall;
import de.mrjulsen.crn.core.navigator.route.RouteJourney;
import de.mrjulsen.crn.core.navigator.route.RouteLeg;
import de.mrjulsen.crn.core.navigator.route.RouteTransfer;

public final class NavigatorDiagnosticsDump {

    private static final String FILE_PREFIX = "createrailwaysnavigator_navigator_";
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private NavigatorDiagnosticsDump() {}

    public static Optional<Path> write(NavigationQuery query) {
        if (!RailwayBackendApi.isActive() || RailwayBackend.getDataDirectory() == null) {
            return Optional.empty();
        }

        long now = RailwayBackendApi.getCurrentTime();
        long horizon = query == null ? NavigationQuery.DEFAULT_SEARCH_HORIZON : query.searchHorizon();

        TimetableIndex.invalidate();
        TimetableIndex index = TimetableIndex.build(now, now + horizon);

        JsonObject root = new JsonObject();
        root.addProperty("writtenAt", now);
        root.addProperty("writtenAtClock", clock(now));
        root.add("index", describeIndex(index));
        root.add("trains", describeTrains());
        root.add("trips", describeTrips(index));
        root.add("nodes", describeNodes(index));
        if (query != null) {
            root.add("query", describeQuery(query));
            root.add("result", describeResult(Navigator.search(query)));
        }

        try {
            Path dir = RailwayBackend.getDataDirectory().toPath();
            Files.createDirectories(dir);
            Path file = dir.resolve(FILE_PREFIX + LocalDateTime.now().format(FILE_TIMESTAMP) + ".json");
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
            }
            return Optional.of(file);
        } catch (IOException e) {
            CreateRailwaysNavigator.LOGGER.error("Unable to write navigator diagnostics dump.", e);
            return Optional.empty();
        }
    }

    private static JsonObject describeIndex(TimetableIndex index) {
        JsonObject json = new JsonObject();
        json.addProperty("from", index.from());
        json.addProperty("until", index.until());
        json.addProperty("fromClock", clock(index.from()));
        json.addProperty("untilClock", clock(index.until()));
        json.addProperty("buildDurationMs", index.buildDurationMs());
        json.addProperty("nodeCount", index.nodeCount());
        json.addProperty("tripCount", index.tripCount());
        return json;
    }

    private static JsonArray describeTrains() {
        JsonArray trains = new JsonArray();
        for (TrainSnapshot train : RailwayBackendApi.getAllTrains()) {
            JsonObject json = new JsonObject();
            json.addProperty("trainId", String.valueOf(train.trainId()));
            json.addProperty("name", train.trainName());
            json.addProperty("displayName", train.displayName());
            json.addProperty("usable", train.isUsable());
            json.addProperty("cancelled", train.isCancelled());
            json.addProperty("indexed", train.isUsable() && !train.isCancelled());

            RailwayBackendApi.getJourney(train.trainId()).ifPresent(journey -> {
                json.addProperty("cyclic", journey.cyclic());
                json.addProperty("repeats", journey.repeats());
                json.addProperty("totalDuration", journey.totalDuration());
                json.addProperty("currentStopIndex", journey.currentStopIndex());
                json.addProperty("currentSectionIndex", journey.currentSectionIndex());
                json.add("sections", describeSections(journey));
                json.add("stops", describeStops(journey));
            });

            trains.add(json);
        }
        return trains;
    }

    private static JsonArray describeSections(JourneySnapshot journey) {
        JsonArray sections = new JsonArray();
        for (SectionSnapshot section : journey.sections()) {
            JsonObject json = new JsonObject();
            json.addProperty("index", section.index());
            json.addProperty("entryIndex", section.entryIndex());
            json.addProperty("line", name(section.line()));
            json.addProperty("category", name(section.category()));
            json.addProperty("origin", section.origin().name());
            json.addProperty("destination", section.destination().name());
            json.addProperty("usable", section.usable());
            json.addProperty("default", section.defaultSection());
            json.addProperty("includesNextSectionStart", section.includesNextSectionStart());
            json.addProperty("current", section.current());

            JsonArray stops = new JsonArray();
            section.stops().forEach(stop -> stops.add(stop.station().name()));
            json.add("stops", stops);
            sections.add(json);
        }
        return sections;
    }

    private static JsonArray describeStops(JourneySnapshot journey) {
        JsonArray stops = new JsonArray();
        for (StopSnapshot stop : journey.stops()) {
            JsonObject json = new JsonObject();
            json.addProperty("stopIndex", stop.stopIndex());
            json.addProperty("entryIndex", stop.entryIndex());
            json.addProperty("sectionIndex", stop.sectionIndex());
            json.addProperty("filter", stop.stationFilter());
            json.addProperty("station", stop.station().name());
            json.addProperty("scheduledStation", stop.scheduledStation().name());
            json.addProperty("tag", stop.station().tagName());
            json.addProperty("platform", stop.station().platform());
            json.addProperty("hasTimes", stop.hasTimes());
            json.addProperty("completedVisits", stop.completedVisits());
            json.add("scheduled", describeTimes(stop.scheduled()));
            json.add("realtime", describeTimes(stop.realtime()));
            json.add("previousActual", describeTimes(stop.previousActual()));
            json.addProperty("title", stop.title());
            stops.add(json);
        }
        return stops;
    }

    private static JsonArray describeTrips(TimetableIndex index) {
        JsonArray trips = new JsonArray();
        for (int i = 0; i < index.tripCount(); i++) {
            Trip trip = index.trip(i);
            JsonObject json = new JsonObject();
            json.addProperty("trip", i);
            json.addProperty("trainId", String.valueOf(trip.trainId()));
            json.addProperty("displayName", trip.displayName());
            json.addProperty("cycle", trip.cycle());
            json.addProperty("callCount", trip.size());
            json.addProperty("boardableCalls", trip.boardableCalls());
            json.addProperty("sections", sectionsOf(trip));

            JsonArray calls = new JsonArray();
            for (int c = 0; c < trip.size(); c++) {
                TripCall call = trip.call(c);
                JsonObject callJson = new JsonObject();
                callJson.addProperty("call", c);
                callJson.addProperty("boardable", c < trip.boardableCalls());
                callJson.addProperty("node", call.node());
                callJson.addProperty("station", call.station().name());
                callJson.addProperty("platform", call.station().platform());
                callJson.addProperty("sectionIndex", call.section().sectionIndex());
                callJson.addProperty("includesNextSectionStart", call.section().includesNextSectionStart());
                callJson.addProperty("entryIndex", call.entryIndex());
                callJson.addProperty("visits", call.visits());
                callJson.addProperty("arrival", call.arrival());
                callJson.addProperty("departure", call.departure());
                callJson.addProperty("arrivalClock", clock(call.arrival()));
                callJson.addProperty("departureClock", clock(call.departure()));
                calls.add(callJson);
            }
            json.add("calls", calls);
            trips.add(json);
        }
        return trips;
    }

    private static String sectionsOf(Trip trip) {
        StringBuilder sections = new StringBuilder();
        for (int c = 0; c < trip.size(); c++) {
            if (c > 0) {
                sections.append('>');
            }
            sections.append(trip.call(c).section().sectionIndex());
        }
        return sections.toString();
    }

    private static JsonArray describeNodes(TimetableIndex index) {
        List<Set<String>> stationsByNode = new ArrayList<>(index.nodeCount());
        int[] callsByNode = new int[index.nodeCount()];
        for (int i = 0; i < index.nodeCount(); i++) {
            stationsByNode.add(new LinkedHashSet<>());
        }
        for (int i = 0; i < index.tripCount(); i++) {
            Trip trip = index.trip(i);
            for (int c = 0; c < trip.size(); c++) {
                TripCall call = trip.call(c);
                if (call.node() >= 0 && call.node() < stationsByNode.size()) {
                    stationsByNode.get(call.node()).add(call.station().name());
                    callsByNode[call.node()]++;
                }
            }
        }

        JsonArray nodes = new JsonArray();
        for (int node = 0; node < index.nodeCount(); node++) {
            StationRef station = index.station(node);
            JsonObject json = new JsonObject();
            json.addProperty("node", node);
            json.addProperty("station", station.name());
            json.addProperty("tag", station.tagName());
            json.addProperty("calls", callsByNode[node]);
            json.addProperty("boardings", index.boardingsAt(node).size());

            JsonArray stations = new JsonArray();
            stationsByNode.get(node).forEach(stations::add);
            json.add("stationsServed", stations);
            nodes.add(json);
        }
        return nodes;
    }

    private static JsonObject describeQuery(NavigationQuery query) {
        JsonObject json = new JsonObject();
        json.addProperty("origin", query.origin());
        json.addProperty("destination", query.destination());
        json.addProperty("departAfter", query.resolvedDepartAfter());
        json.addProperty("departAfterClock", clock(query.resolvedDepartAfter()));
        json.addProperty("minTransferTime", query.minTransferTime());
        json.addProperty("transferRiskBuffer", query.transferRiskBuffer());
        json.addProperty("maxLegs", query.maxLegs());
        json.addProperty("searchHorizon", query.searchHorizon());
        json.addProperty("waypoints", String.valueOf(query.waypoints()));
        return json;
    }

    private static JsonObject describeResult(NavigationResult result) {
        JsonObject json = new JsonObject();
        json.addProperty("status", String.valueOf(result.status()));
        json.addProperty("durationMs", result.durationMs());
        json.addProperty("stationsSearched", result.stationsSearched());
        json.addProperty("tripsScanned", result.tripsScanned());

        JsonArray journeys = new JsonArray();
        for (RouteJourney journey : result.journeys()) {
            JsonObject journeyJson = new JsonObject();
            journeyJson.addProperty("departure", journey.departure());
            journeyJson.addProperty("arrival", journey.arrival());
            journeyJson.addProperty("departureClock", clock(journey.departure()));
            journeyJson.addProperty("arrivalClock", clock(journey.arrival()));
            journeyJson.addProperty("duration", journey.duration());
            journeyJson.addProperty("transferCount", journey.transferCount());
            journeyJson.addProperty("legCount", journey.legs().size());

            JsonArray legs = new JsonArray();
            for (RouteLeg leg : journey.legs()) {
                JsonObject legJson = new JsonObject();
                legJson.addProperty("trainId", String.valueOf(leg.trainId()));
                legJson.addProperty("displayName", leg.displayName());
                legJson.addProperty("sectionIndex", leg.sectionIndex());
                legJson.addProperty("destinationText", leg.destinationText());

                JsonArray calls = new JsonArray();
                for (RouteCall call : leg.calls()) {
                    JsonObject callJson = new JsonObject();
                    callJson.addProperty("station", call.stationName());
                    callJson.addProperty("platform", call.platform());
                    callJson.addProperty("entryIndex", call.entryIndex());
                    callJson.addProperty("cycle", call.cycle());
                    callJson.addProperty("arrivalClock", clock(call.realtime().arrival()));
                    callJson.addProperty("departureClock", clock(call.realtime().departure()));
                    calls.add(callJson);
                }
                legJson.add("calls", calls);
                legs.add(legJson);
            }
            journeyJson.add("legs", legs);

            JsonArray transfers = new JsonArray();
            for (RouteTransfer transfer : journey.transfers()) {
                JsonObject transferJson = new JsonObject();
                transferJson.addProperty("at", transfer.arrivalStationName());
                transferJson.addProperty("to", transfer.departureStationName());
                transferJson.addProperty("staysSeated", transfer.staysSeated());
                transferJson.addProperty("duration", transfer.duration());
                transferJson.addProperty("state", String.valueOf(transfer.state()));
                transfers.add(transferJson);
            }
            journeyJson.add("transfers", transfers);
            journeys.add(journeyJson);
        }
        json.add("journeys", journeys);
        return json;
    }

    private static JsonObject describeTimes(StopTimes times) {
        JsonObject json = new JsonObject();
        json.addProperty("known", times.isKnown());
        json.addProperty("arrival", times.arrival());
        json.addProperty("departure", times.departure());
        if (times.isKnown()) {
            json.addProperty("arrivalClock", clock(times.arrival()));
            json.addProperty("departureClock", clock(times.departure()));
        }
        return json;
    }

    private static String name(LineRef line) {
        return line.isKnown() ? line.name() : "-";
    }

    private static String name(TrainCategoryRef category) {
        return category.isKnown() ? category.name() : "-";
    }

    private static String clock(long ticks) {
        long day = Math.floorMod(ticks, 24000L);
        return String.format("%02d:%02d", Math.floorDiv(day, 1000L), (day % 1000L) * 60 / 1000);
    }
}
