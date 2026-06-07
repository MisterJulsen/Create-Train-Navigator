package de.mrjulsen.crn.api.web;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import de.mrjulsen.crn.data.train.TrainStop;
import de.mrjulsen.crn.data.train.TrainUtils;

/** Exports upcoming departures and arrivals at a station. */
public final class StationDeparturesJsonExporter {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private StationDeparturesJsonExporter() {}

    public static JsonObject buildRoot(String stationName, long worldTick, int limit, boolean realTimeOnly) {
        JsonObject root = new JsonObject();
        root.addProperty("v", 1);
        root.addProperty("t", worldTick);
        root.addProperty("station", stationName);

        if (stationName == null || stationName.isBlank()) {
            root.addProperty("error", "station parameter required");
            root.add("departures", new JsonArray());
            root.addProperty("count", 0);
            return root;
        }

        String decodedStation = URLDecoder.decode(stationName.trim(), StandardCharsets.UTF_8);
        int cappedLimit = Math.max(1, Math.min(limit <= 0 ? DEFAULT_LIMIT : limit, MAX_LIMIT));

        List<TrainStop> stops = new ArrayList<>(
            TrainUtils.getDeparturesAtStationName(decodedStation, null, realTimeOnly, false)
        );
        stops.sort(Comparator.comparingLong(StationDeparturesJsonExporter::sortKey));

        JsonArray departures = new JsonArray();
        int added = 0;
        for (TrainStop stop : stops) {
            if (added >= cappedLimit) {
                break;
            }
            if (stop.isDeparted()) {
                continue;
            }
            long ticksUntilDep = Math.max(0, stop.getRealTimeDepartureTime() - worldTick);
            long ticksUntilArr = Math.max(0, stop.getRealTimeArrivalTime() - worldTick);
            if (ticksUntilDep <= 0 && ticksUntilArr <= 0 && stop.getState().getPositionMultiplier() > 0) {
                continue;
            }
            departures.add(departureObject(stop, worldTick));
            added++;
        }

        root.add("departures", departures);
        root.addProperty("count", departures.size());
        return root;
    }

    private static long sortKey(TrainStop stop) {
        long dep = stop.getRealTimeDepartureTime();
        if (dep > 0) {
            return dep;
        }
        return stop.getRealTimeArrivalTime();
    }

    private static JsonObject departureObject(TrainStop stop, long worldTick) {
        JsonObject obj = new JsonObject();
        obj.addProperty("trainId", stop.getTrainId().toString());
        obj.addProperty("train", stop.getTrainName());
        obj.addProperty("displayName", stop.getTrainDisplayName());
        obj.addProperty("destination", stop.getDisplayTitle());
        if (stop.getScheduleTitle() != null && !stop.getScheduleTitle().isEmpty()) {
            obj.addProperty("title", stop.getScheduleTitle());
        }
        if (stop.getTerminusText() != null && !stop.getTerminusText().isEmpty()) {
            obj.addProperty("terminus", stop.getTerminusText());
        }
        obj.addProperty("station", stop.getRealTimeStationTag().tagName());
        obj.addProperty("scheduleIndex", stop.getScheduleIndex());
        obj.addProperty("state", stop.getState().name().toLowerCase());
        obj.addProperty("scheduledArrival", stop.getScheduledArrivalTime());
        obj.addProperty("scheduledDeparture", stop.getScheduledDepartureTime());
        obj.addProperty("realArrival", stop.getRealTimeArrivalTime());
        obj.addProperty("realDeparture", stop.getRealTimeDepartureTime());

        long ticksUntilDep = Math.max(0, stop.getRealTimeDepartureTime() - worldTick);
        long ticksUntilArr = Math.max(0, stop.getRealTimeArrivalTime() - worldTick);
        obj.addProperty("ticksUntilDeparture", ticksUntilDep);
        obj.addProperty("ticksUntilArrival", ticksUntilArr);
        obj.addProperty("secondsUntilDeparture", (int) (ticksUntilDep / 20L));
        obj.addProperty("secondsUntilArrival", (int) (ticksUntilArr / 20L));
        obj.addProperty("delayed", stop.isAnyDelayed());

        WebApiJsonHelper.addTrainInfo(obj, stop.getTrainInfo());
        obj.add("color", WebApiJsonHelper.colorObject(stop.getTrainDisplayColor()));
        return obj;
    }
}
