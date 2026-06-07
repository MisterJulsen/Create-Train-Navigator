package de.mrjulsen.crn.api.web;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.event.CRNEventsManager;
import de.mrjulsen.crn.event.events.GlobalTrainDisplayDataRefreshEventPost;
import de.mrjulsen.crn.event.events.TrainArrivalAndDepartureEvent;

/** Lightweight JSON HTTP API for live station stop updates. */
public final class StationStopWebApi {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final String EVENT_LISTENER_ID = CreateRailwaysNavigator.MOD_ID + "_web_api";
    private static final String REFRESH_LISTENER_ID = CreateRailwaysNavigator.MOD_ID + "_web_api_refresh";

    private static HttpServer server;
    private static ExecutorService executor;
    private static final AtomicBoolean running = new AtomicBoolean(false);
    private static int tickCounter = 0;
    private static int startupSyncTicks = 0;
    private static final int SCHEDULE_REFRESH_INTERVAL = 20;
    private static final int STARTUP_SYNC_DURATION = 200;

    private StationStopWebApi() {}

    public static boolean isRunning() {
        return running.get();
    }

    public static boolean isEnabledInConfig() {
        return ModCommonConfig.WEB_API_ENABLED.get();
    }

    /** Local URL suitable for curl / browser use on the host machine. */
    public static String getBaseUrl() {
        String host = ModCommonConfig.WEB_API_BIND_ADDRESS.get();
        if ("0.0.0.0".equals(host) || "::".equals(host) || "[::]".equals(host)) {
            host = "127.0.0.1";
        }
        return "http://" + host + ":" + ModCommonConfig.WEB_API_PORT.get();
    }

    public static String getBindAddress() {
        return ModCommonConfig.WEB_API_BIND_ADDRESS.get();
    }

    public static int getPort() {
        return ModCommonConfig.WEB_API_PORT.get();
    }

    public static void start() {
        if (!ModCommonConfig.WEB_API_ENABLED.get() || running.get()) {
            return;
        }

        try {
            InetSocketAddress address = new InetSocketAddress(
                ModCommonConfig.WEB_API_BIND_ADDRESS.get(),
                ModCommonConfig.WEB_API_PORT.get()
            );
            server = HttpServer.create(address, 0);
            executor = Executors.newFixedThreadPool(2, r -> {
                Thread t = new Thread(r, "CRN Station Stop API");
                t.setDaemon(true);
                return t;
            });
            server.setExecutor(executor);

            server.createContext("/health", StationStopWebApi::handleHealth);
            server.createContext("/api/v1/station-stops", StationStopWebApi::handleSnapshot);
            server.createContext("/api/v1/station-stops/events", StationStopWebApi::handleEvents);
            server.createContext("/api/v1/trains", StationStopWebApi::handleTrains);
            server.createContext("/api/v1/train", StationStopWebApi::handleTrainById);
            server.createContext("/api/v1/stations/departures", StationStopWebApi::handleStationDepartures);
            server.createContext("/api/v1/lines", StationStopWebApi::handleLines);

            CRNEventsManager.getEvent(TrainArrivalAndDepartureEvent.class).register(EVENT_LISTENER_ID, (train, station, isArrival) -> {
                if (isArrival) {
                    station.ifPresent(s -> StationStopTracker.getInstance().onArrival(train, s));
                } else {
                    station.ifPresent(s -> StationStopTracker.getInstance().onDeparture(train, s));
                }
            });

            CRNEventsManager.getEvent(GlobalTrainDisplayDataRefreshEventPost.class).register(REFRESH_LISTENER_ID, () -> {
                if (running.get()) {
                    StationStopTracker.getInstance().syncExistingStops();
                    TrainScheduleCache.rebuild();
                }
            });

            StationStopTracker.getInstance().clear();
            StationStopTracker.getInstance().syncExistingStops();
            TrainScheduleCache.rebuild();
            tickCounter = 0;
            startupSyncTicks = 0;

            server.start();
            running.set(true);
            CreateRailwaysNavigator.LOGGER.info(
                "Station stop web API listening on http://{}:{}/api/v1/station-stops",
                ModCommonConfig.WEB_API_BIND_ADDRESS.get(),
                ModCommonConfig.WEB_API_PORT.get()
            );
        } catch (IOException e) {
            CreateRailwaysNavigator.LOGGER.error("Unable to start station stop web API.", e);
            stop();
        }
    }

    public static void stop() {
        if (!running.getAndSet(false)) {
            return;
        }

        CRNEventsManager.getEvent(TrainArrivalAndDepartureEvent.class).unregister(EVENT_LISTENER_ID);
        CRNEventsManager.getEvent(GlobalTrainDisplayDataRefreshEventPost.class).unregister(REFRESH_LISTENER_ID);
        StationStopTracker.getInstance().clear();
        TrainScheduleCache.rebuild();
        tickCounter = 0;
        startupSyncTicks = 0;

        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    /** Refresh countdown fields ~once per second while the API is running. */
    public static void tick() {
        if (!running.get()) {
            return;
        }

        if (startupSyncTicks < STARTUP_SYNC_DURATION && startupSyncTicks % SCHEDULE_REFRESH_INTERVAL == 0) {
            StationStopTracker.getInstance().syncExistingStops();
            TrainScheduleCache.rebuild();
        }
        if (startupSyncTicks < STARTUP_SYNC_DURATION) {
            startupSyncTicks++;
        }

        if (++tickCounter >= SCHEDULE_REFRESH_INTERVAL) {
            tickCounter = 0;
            TrainScheduleCache.rebuild();
            StationStopTracker.getInstance().refreshCountdowns();
        }
    }

    private static void handleTrains(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            sendCors(exchange, 204, 0);
            return;
        }
        if (!"GET".equalsIgnoreCase(method)) {
            sendText(exchange, 405, "Method Not Allowed");
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        String trainIdParam = parseQueryParam(query, "id");
        if (trainIdParam != null && !trainIdParam.isEmpty()) {
            handleSingleTrain(exchange, trainIdParam);
            return;
        }

        TrainScheduleCache.CachedSnapshot snapshot = TrainScheduleCache.getSnapshot();
        String ifNoneMatch = exchange.getRequestHeaders().getFirst("If-None-Match");
        if (ifNoneMatch != null && ifNoneMatch.equals(snapshot.etag())) {
            sendNotModified(exchange, snapshot.etag());
            return;
        }

        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        headers.set("Cache-Control", "no-store");
        headers.set("ETag", snapshot.etag());
        addCors(headers);
        exchange.sendResponseHeaders(200, snapshot.body().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(snapshot.body());
        }
    }

    private static void handleSingleTrain(HttpExchange exchange, String trainIdParam) throws IOException {
        java.util.UUID trainId;
        try {
            trainId = java.util.UUID.fromString(trainIdParam);
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, "{\"error\":\"invalid train id\"}");
            return;
        }

        byte[] body = TrainScheduleCache.buildSingleTrainBody(trainId);
        if (body == null) {
            sendJson(exchange, 404, "{\"error\":\"train not found\"}");
            return;
        }

        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        headers.set("Cache-Control", "no-store");
        addCors(headers);
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private static void handleTrainById(HttpExchange exchange) throws IOException {
        if (!allowGet(exchange)) {
            return;
        }
        String trainIdParam = parseQueryParam(exchange.getRequestURI().getQuery(), "id");
        if (trainIdParam == null || trainIdParam.isEmpty()) {
            sendJson(exchange, 400, "{\"error\":\"id parameter required\"}");
            return;
        }
        handleSingleTrain(exchange, trainIdParam);
    }

    private static void handleStationDepartures(HttpExchange exchange) throws IOException {
        if (!allowGet(exchange)) {
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        String station = parseQueryParam(query, "station");
        if (station == null || station.isEmpty()) {
            sendJson(exchange, 400, "{\"error\":\"station parameter required\"}");
            return;
        }

        int limit = parseIntQueryParam(query, "limit", 20);
        boolean realTimeOnly = !"false".equalsIgnoreCase(parseQueryParam(query, "realtime"));
        long tick = WebApiSupport.currentWorldTick();
        sendJsonObject(exchange, 200, StationDeparturesJsonExporter.buildRoot(station, tick, limit, realTimeOnly));
    }

    private static void handleLines(HttpExchange exchange) throws IOException {
        if (!allowGet(exchange)) {
            return;
        }
        sendJsonObject(exchange, 200, TrainLinesJsonExporter.buildRoot(WebApiSupport.currentWorldTick()));
    }

    private static boolean allowGet(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            sendCors(exchange, 204, 0);
            return false;
        }
        if (!"GET".equalsIgnoreCase(method)) {
            sendText(exchange, 405, "Method Not Allowed");
            return false;
        }
        return true;
    }

    private static void sendJsonObject(HttpExchange exchange, int code, JsonObject root) throws IOException {
        sendJson(exchange, code, GSON.toJson(root));
    }

    private static int parseIntQueryParam(String query, String key, int defaultValue) {
        String value = parseQueryParam(query, key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static void sendJson(HttpExchange exchange, int code, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        addCors(headers);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String parseQueryParam(String query, String key) {
        if (query == null || query.isEmpty()) {
            return null;
        }
        for (String part : query.split("&")) {
            if (part.startsWith(key + "=")) {
                return part.substring(key.length() + 1);
            }
        }
        return null;
    }

    private static void handleHealth(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendText(exchange, 405, "Method Not Allowed");
            return;
        }
        sendText(exchange, 200, "ok");
    }

    private static void handleSnapshot(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            sendCors(exchange, 204, 0);
            return;
        }
        if (!"GET".equalsIgnoreCase(method)) {
            sendText(exchange, 405, "Method Not Allowed");
            return;
        }

        StationStopTracker.CachedSnapshot snapshot = StationStopTracker.getInstance().getSnapshot();
        String ifNoneMatch = exchange.getRequestHeaders().getFirst("If-None-Match");
        if (ifNoneMatch != null && ifNoneMatch.equals(snapshot.etag())) {
            sendNotModified(exchange, snapshot.etag());
            return;
        }

        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        headers.set("Cache-Control", "no-store");
        headers.set("ETag", snapshot.etag());
        addCors(headers);
        exchange.sendResponseHeaders(200, snapshot.body().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(snapshot.body());
        }
    }

    private static void handleEvents(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            sendCors(exchange, 204, 0);
            return;
        }
        if (!"GET".equalsIgnoreCase(method)) {
            sendText(exchange, 405, "Method Not Allowed");
            return;
        }

        String accept = exchange.getRequestHeaders().getFirst("Accept");
        boolean wantsSse = accept != null && accept.contains("text/event-stream");
        String query = exchange.getRequestURI().getQuery();
        long since = parseSince(query);

        if (wantsSse) {
            handleSse(exchange, since);
            return;
        }

        List<StationStopTracker.StopEvent> events = StationStopTracker.getInstance().getEventsSince(since);
        JsonObject root = new JsonObject();
        root.addProperty("v", 1);
        JsonArray arr = new JsonArray();
        for (StationStopTracker.StopEvent event : events) {
            arr.add(GSON.fromJson(StationStopTracker.eventToJson(event), JsonObject.class));
        }
        root.add("events", arr);
        byte[] body = GSON.toJson(root).getBytes(StandardCharsets.UTF_8);

        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        headers.set("Cache-Control", "no-store");
        addCors(headers);
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private static void handleSse(HttpExchange exchange, long since) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "text/event-stream; charset=utf-8");
        headers.set("Cache-Control", "no-cache, no-transform");
        headers.set("Connection", "keep-alive");
        addCors(headers);
        exchange.sendResponseHeaders(200, 0);

        OutputStream os = exchange.getResponseBody();
        for (StationStopTracker.StopEvent event : StationStopTracker.getInstance().getEventsSince(since)) {
            writeSseEvent(os, event);
        }

        Consumer<StationStopTracker.StopEvent> listener = event -> writeSseEventQuiet(os, event);
        StationStopTracker.getInstance().addLiveListener(listener);

        try {
            while (running.get()) {
                writeSseComment(os);
                Thread.sleep(15000L);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            // Client disconnected.
        } finally {
            StationStopTracker.getInstance().removeLiveListener(listener);
            os.close();
        }
    }

    private static void writeSseEvent(OutputStream os, StationStopTracker.StopEvent event) throws IOException {
        String payload = "data: " + StationStopTracker.eventToJson(event) + "\n\n";
        os.write(payload.getBytes(StandardCharsets.UTF_8));
        os.flush();
    }

    private static void writeSseEventQuiet(OutputStream os, StationStopTracker.StopEvent event) {
        try {
            writeSseEvent(os, event);
        } catch (IOException ignored) {
        }
    }

    private static void writeSseComment(OutputStream os) throws IOException {
        os.write(": keepalive\n\n".getBytes(StandardCharsets.UTF_8));
        os.flush();
    }

    private static long parseSince(String query) {
        if (query == null || query.isEmpty()) {
            return 0L;
        }
        for (String part : query.split("&")) {
            if (part.startsWith("since=")) {
                try {
                    return Long.parseLong(part.substring(6));
                } catch (NumberFormatException ignored) {
                    return 0L;
                }
            }
        }
        return 0L;
    }

    private static void sendNotModified(HttpExchange exchange, String etag) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.set("ETag", etag);
        addCors(headers);
        exchange.sendResponseHeaders(304, -1);
        exchange.close();
    }

    private static void sendText(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "text/plain; charset=utf-8");
        addCors(headers);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendCors(HttpExchange exchange, int code, int length) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        addCors(headers);
        exchange.sendResponseHeaders(code, length);
        exchange.close();
    }

    private static void addCors(Headers headers) {
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Accept, If-None-Match");
    }
}
