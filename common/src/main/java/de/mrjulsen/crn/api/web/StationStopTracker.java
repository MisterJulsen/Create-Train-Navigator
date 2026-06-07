package de.mrjulsen.crn.api.web;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.station.GlobalStation;

import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.data.train.TrainData;
import de.mrjulsen.crn.data.train.TrainListener;
import de.mrjulsen.crn.data.train.TrainPrediction;
import de.mrjulsen.crn.data.train.TrainUtils;
import de.mrjulsen.crn.event.ModCommonEvents;

/** Thread-safe in-memory state for trains currently at stations and recent stop events. */
public final class StationStopTracker {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private static final StationStopTracker INSTANCE = new StationStopTracker();

    private final Map<UUID, ActiveStop> activeStops = new LinkedHashMap<>();
    private final CopyOnWriteArrayList<StopEvent> events = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<StopEvent>> liveListeners = new CopyOnWriteArrayList<>();
    private final AtomicLong nextEventId = new AtomicLong(1);
    private final AtomicReference<CachedSnapshot> snapshot = new AtomicReference<>(CachedSnapshot.empty());

    private StationStopTracker() {}

    public static StationStopTracker getInstance() {
        return INSTANCE;
    }

    public void clear() {
        synchronized (activeStops) {
            activeStops.clear();
        }
        events.clear();
        nextEventId.set(1);
        rebuildSnapshot();
    }

    /** Seed stops for trains already waiting when the API starts. */
    public void syncExistingStops() {
        if (!ModCommonEvents.hasServer()) {
            return;
        }

        long tick = currentWorldTick();
        for (Train train : TrainUtils.getTrains(true)) {
            if (train == null || train.runtime == null || train.runtime.getSchedule() == null) {
                continue;
            }
            GlobalStation station = train.getCurrentStation();
            if (station == null) {
                continue;
            }
            registerArrival(train, station, tick, false);
        }
    }

    public void onArrival(Train train, GlobalStation station) {
        registerArrival(train, station, currentWorldTick(), true);
    }

    public void onDeparture(Train train, GlobalStation station) {
        if (train == null) {
            return;
        }

        ActiveStop removed;
        synchronized (activeStops) {
            removed = activeStops.remove(train.id);
        }

        long tick = currentWorldTick();
        String stationName = station != null ? station.name : removed != null ? removed.station() : "";
        StopEvent event = new StopEvent(
            nextEventId.getAndIncrement(),
            "departure",
            train.id,
            train.name.getString(),
            stationName,
            removed != null ? removed.title() : "",
            removed != null ? removed.scheduleIndex() : -1,
            removed != null ? removed.lineId() : "",
            tick,
            removed != null ? removed.arrivedAtTick() : tick,
            removed != null ? removed.scheduledDepartureTick() : 0,
            removed != null ? removed.realDepartureTick() : 0
        );
        appendEvent(event);
    }

    public CachedSnapshot getSnapshot() {
        return snapshot.get();
    }

    public List<StopEvent> getEventsSince(long sinceId) {
        List<StopEvent> result = new ArrayList<>();
        for (StopEvent event : events) {
            if (event.id() > sinceId) {
                result.add(event);
            }
        }
        return result;
    }

    public int getActiveStopCount() {
        synchronized (activeStops) {
            return activeStops.size();
        }
    }

    public int getBufferedEventCount() {
        return events.size();
    }

    public long getLatestEventId() {
        if (events.isEmpty()) {
            return 0L;
        }
        return events.get(events.size() - 1).id();
    }

    public void addLiveListener(Consumer<StopEvent> listener) {
        liveListeners.add(listener);
    }

    public void removeLiveListener(Consumer<StopEvent> listener) {
        liveListeners.remove(listener);
    }

    /** Rebuild station-stop snapshot so countdown fields stay current. */
    public void refreshCountdowns() {
        rebuildSnapshot();
    }

    private void registerArrival(Train train, GlobalStation station, long tick, boolean emitEvent) {
        if (train == null || station == null) {
            return;
        }

        StopDetails details = detailsFor(train);
        ActiveStop stop = new ActiveStop(
            train.id,
            train.name.getString(),
            station.name,
            details.title(),
            details.scheduleIndex(),
            details.lineId(),
            tick,
            details.scheduledDepartureTick(),
            details.realDepartureTick()
        );

        synchronized (activeStops) {
            activeStops.put(train.id, stop);
        }
        rebuildSnapshot();
        TrainScheduleCache.rebuild();

        if (emitEvent) {
            StopEvent event = new StopEvent(
                nextEventId.getAndIncrement(),
                "arrival",
                stop.trainId(),
                stop.trainName(),
                stop.station(),
                stop.title(),
                stop.scheduleIndex(),
                stop.lineId(),
                tick,
                stop.arrivedAtTick(),
                stop.scheduledDepartureTick(),
                stop.realDepartureTick()
            );
            appendEvent(event);
        }
    }

    private void appendEvent(StopEvent event) {
        trimEvents();
        events.add(event);
        rebuildSnapshot();
        TrainScheduleCache.rebuild();
        for (Consumer<StopEvent> listener : liveListeners) {
            try {
                listener.accept(event);
            } catch (Exception ignored) {
            }
        }
    }

    private void trimEvents() {
        int max = ModCommonConfig.WEB_API_MAX_EVENTS.get();
        while (events.size() >= max) {
            events.remove(0);
        }
    }

    private void rebuildSnapshot() {
        long tick = currentWorldTick();
        JsonObject root = new JsonObject();
        root.addProperty("v", 1);
        root.addProperty("t", tick);

        JsonArray stops = new JsonArray();
        synchronized (activeStops) {
            for (ActiveStop stop : activeStops.values()) {
                stops.add(toJson(stop, tick));
            }
        }
        root.add("stops", stops);

        byte[] body = GSON.toJson(root).getBytes(StandardCharsets.UTF_8);
        String etag = Integer.toHexString(java.util.Arrays.hashCode(body));
        snapshot.set(new CachedSnapshot(body, etag, tick));
    }

    private static JsonObject toJson(ActiveStop stop, long worldTick) {
        JsonObject obj = new JsonObject();
        obj.addProperty("trainId", stop.trainId().toString());
        obj.addProperty("train", stop.trainName());
        obj.addProperty("station", stop.station());
        if (!stop.title().isEmpty()) {
            obj.addProperty("title", stop.title());
        }
        if (stop.scheduleIndex() >= 0) {
            obj.addProperty("idx", stop.scheduleIndex());
        }
        if (!stop.lineId().isEmpty()) {
            obj.addProperty("lineId", stop.lineId());
        }
        obj.addProperty("arrivedTick", stop.arrivedAtTick());
        if (stop.scheduledDepartureTick() > 0) {
            obj.addProperty("schedDep", stop.scheduledDepartureTick());
            obj.addProperty("ticksUntilDeparture", Math.max(0, stop.realDepartureTick() - worldTick));
            obj.addProperty("secondsUntilDeparture", (int) (Math.max(0, stop.realDepartureTick() - worldTick) / 20L));
        }
        if (stop.realDepartureTick() > 0) {
            obj.addProperty("realDep", stop.realDepartureTick());
        }

        TrainListener.getTrainData(stop.trainId()).ifPresent(data -> {
            obj.add("train", TrainScheduleJsonExporter.buildTrain(data, worldTick));
        });
        return obj;
    }

    public static String eventToJson(StopEvent event) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", event.id());
        obj.addProperty("type", event.type());
        obj.addProperty("trainId", event.trainId().toString());
        obj.addProperty("train", event.trainName());
        obj.addProperty("station", event.station());
        if (!event.title().isEmpty()) {
            obj.addProperty("title", event.title());
        }
        if (event.scheduleIndex() >= 0) {
            obj.addProperty("idx", event.scheduleIndex());
        }
        if (!event.lineId().isEmpty()) {
            obj.addProperty("lineId", event.lineId());
        }
        obj.addProperty("tick", event.tick());
        obj.addProperty("arrivedTick", event.arrivedAtTick());
        if (event.scheduledDepartureTick() > 0) {
            obj.addProperty("schedDep", event.scheduledDepartureTick());
        }
        if (event.realDepartureTick() > 0) {
            obj.addProperty("realDep", event.realDepartureTick());
        }
        long worldTick = currentWorldTick();
        if (event.realDepartureTick() > 0) {
            long untilDep = Math.max(0, event.realDepartureTick() - worldTick);
            if (untilDep > 0) {
                obj.addProperty("ticksUntilDeparture", untilDep);
                obj.addProperty("secondsUntilDeparture", (int) (untilDep / 20L));
            }
        }
        TrainListener.getTrainData(event.trainId()).ifPresent(data -> {
            obj.add("train", TrainScheduleJsonExporter.buildTrain(data, worldTick));
        });
        return GSON.toJson(obj);
    }

    private static StopDetails detailsFor(Train train) {
        Optional<TrainData> data = TrainListener.getTrainData(train.id);
        if (data.isEmpty()) {
            return StopDetails.empty();
        }

        TrainData trainData = data.get();
        Optional<TrainPrediction> prediction = trainData.getPredictionByIndex(trainData.getCurrentScheduleIndex());
        String title = prediction.map(TrainPrediction::getTitle).orElse("");
        long schedDep = prediction.map(p -> p.scheduled().departureTime()).orElse(0L);
        long realDep = prediction.map(p -> p.realTime().departureTime()).orElse(0L);
        String lineId = trainData.getCurrentSection().getTrainLine()
            .map(line -> line.getId().toString())
            .orElse("");
        return new StopDetails(title, trainData.getCurrentScheduleIndex(), lineId, schedDep, realDep);
    }

    private static long currentWorldTick() {
        return WebApiSupport.currentWorldTick();
    }

    public record ActiveStop(
        UUID trainId,
        String trainName,
        String station,
        String title,
        int scheduleIndex,
        String lineId,
        long arrivedAtTick,
        long scheduledDepartureTick,
        long realDepartureTick
    ) {}

    public record StopEvent(
        long id,
        String type,
        UUID trainId,
        String trainName,
        String station,
        String title,
        int scheduleIndex,
        String lineId,
        long tick,
        long arrivedAtTick,
        long scheduledDepartureTick,
        long realDepartureTick
    ) {}

    public record CachedSnapshot(byte[] body, String etag, long worldTick) {
        static CachedSnapshot empty() {
            return new CachedSnapshot("{\"v\":1,\"t\":0,\"stops\":[]}".getBytes(StandardCharsets.UTF_8), "0", 0L);
        }
    }

    private record StopDetails(String title, int scheduleIndex, String lineId, long scheduledDepartureTick, long realDepartureTick) {
        static StopDetails empty() {
            return new StopDetails("", -1, "", 0L, 0L);
        }
    }
}
