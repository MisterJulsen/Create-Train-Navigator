package de.mrjulsen.crn.core.debug;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.core.RailwayBackend;
import de.mrjulsen.crn.core.timing.LegKinematics;
import de.mrjulsen.crn.core.train.TrackedTrain;
import de.mrjulsen.crn.core.delay.DelayInstance;
import de.mrjulsen.crn.core.realtime.RealtimeTracker;
import de.mrjulsen.crn.core.schedule.JourneyStop;
import de.mrjulsen.crn.core.timing.StopTimes;
import de.mrjulsen.crn.core.timing.StopTimings;
import de.mrjulsen.crn.config.ModCommonConfig;

public final class BackendDiagnosticsRecorder {

    private static final String FILE_PREFIX = "createrailwaysnavigator_diagnostics_";
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private static volatile boolean active = false;
    private static BufferedWriter writer;
    private static Path currentFile;

    private BackendDiagnosticsRecorder() {}

    public static boolean isActive() {
        return active;
    }

    public static Optional<Path> getCurrentFile() {
        return Optional.ofNullable(currentFile);
    }

    public static synchronized Optional<Path> start() {
        stop();

        File dir = RailwayBackend.getDataDirectory();
        if (dir == null) {
            return Optional.empty();
        }

        try {
            Files.createDirectories(dir.toPath());
            currentFile = dir.toPath().resolve(FILE_PREFIX + LocalDateTime.now().format(FILE_TIMESTAMP) + ".jsonl");
            writer = Files.newBufferedWriter(currentFile, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            active = true;
            writeLine(configHeader());
            CreateRailwaysNavigator.LOGGER.info("[Backend] Diagnostics recording started: {}", currentFile);
            return Optional.of(currentFile);
        } catch (IOException e) {
            CreateRailwaysNavigator.LOGGER.error("[Backend] Unable to start diagnostics recording.", e);
            active = false;
            writer = null;
            currentFile = null;
            return Optional.empty();
        }
    }

    public static synchronized void stop() {
        if (!active && writer == null) {
            return;
        }
        active = false;
        try {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
        } catch (IOException e) {
            CreateRailwaysNavigator.LOGGER.error("[Backend] Unable to close diagnostics recording.", e);
        }
        writer = null;
        CreateRailwaysNavigator.LOGGER.info("[Backend] Diagnostics recording stopped.");
    }

    public static void recordSnapshot(Collection<TrackedTrain> trains, long now) {
        if (!active) {
            return;
        }
        for (TrackedTrain train : trains) {
            writeLine(buildSnapshot(train, now));
        }
    }

    private static JsonObject buildSnapshot(TrackedTrain train, long now) {
        JsonObject root = new JsonObject();
        root.addProperty("event", "snapshot");
        root.addProperty("now", now);
        addTrainIdentity(root, train);

        root.addProperty("lifecycle", train.getLifecycleState().name());
        root.addProperty("liveState", train.getLiveState().name());
        root.addProperty("cancelled", train.isCancelled());
        root.addProperty("totalDuration", train.getTotalDuration());
        root.addProperty("delayOffset", train.getDelayOffset());
        root.addProperty("maxDeviation", train.getMaxDeviation());
        root.addProperty("delayed", train.isDelayed());
        root.addProperty("currentStopEntryIndex", train.getCurrentStop().map(JourneyStop::entryIndex).orElse(-1));

        RealtimeTracker rt = train.getRealtime();
        JsonObject live = new JsonObject();
        live.addProperty("transitTicks", rt.getTransitTicks());
        live.addProperty("dwellTicks", rt.getDwellTicks());
        live.addProperty("stalledTicks", rt.getStalledTicks());
        live.addProperty("noPathTicks", rt.getNoPathTicks());
        live.addProperty("signalWaitTicks", rt.getTotalSignalWaitTicks());
        UUID signalId = rt.getCurrentSignalId();
        live.addProperty("currentSignalId", signalId == null ? null : signalId.toString());
        LegKinematics kin = train.getLegKinematics();
        if (kin != null && train.getTrain().navigation != null && train.getTrain().navigation.destination != null) {
            double dtd = train.getTrain().navigation.distanceToDestination;
            live.addProperty("distanceToDestination", dtd);
            live.addProperty("physicsLegLength", kin.length());
            live.addProperty("physicsTotalTicks", kin.totalTicks());
            live.addProperty("physicsRemainingTicks", kin.remainingTicks(dtd));
        }
        root.add("live", live);

        JsonArray delays = new JsonArray();
        for (DelayInstance delay : train.getActiveDelays()) {
            JsonObject d = new JsonObject();
            d.addProperty("cause", delay.causeId().getPath());
            d.addProperty("severity", delay.severity().name());
            d.addProperty("since", delay.since());
            if (delay.hasArgs()) {
                d.addProperty("args", String.join(", ", delay.argValues()));
            }
            delays.add(d);
        }
        root.add("delays", delays);

        JsonArray stops = new JsonArray();
        for (JourneyStop stop : train.getJourney().getStops()) {
            stops.add(buildStop(train, stop));
        }
        root.add("stops", stops);

        return root;
    }

    private static JsonObject buildStop(TrackedTrain train, JourneyStop stop) {
        StopTimings timing = train.getTimings(stop);

        JsonObject json = new JsonObject();
        json.addProperty("entryIndex", stop.entryIndex());
        json.addProperty("station", stop.getStationName());
        if (timing == null) {
            return json;
        }

        json.add("scheduled", buildTimes(timing.getScheduled()));
        json.add("realtime", buildTimes(timing.getRealtime()));
        json.addProperty("arrivalDeviation", timing.getArrivalDeviation());
        json.addProperty("departureDeviation", timing.getDepartureDeviation());
        json.addProperty("legReference", timing.legDuration().get());
        json.addProperty("legLastMeasurement", timing.legDuration().lastMeasurement());
        json.addProperty("legWaitReference", timing.legWait().get());
        json.addProperty("legWaitLastMeasurement", timing.legWait().lastMeasurement());
        json.addProperty("scheduledWaitTicks", timing.scheduledWaitTicks());

        JsonArray history = new JsonArray();
        for (int value : timing.legDuration().getHistory()) {
            history.add(value);
        }
        json.add("legHistory", history);

        json.addProperty("dwellDuration", timing.dwellDuration());
        json.addProperty("dwellResidual", timing.dwellResidualTicks());
        json.addProperty("dwellResidualLastMeasurement", timing.dwellResidual().lastMeasurement());
        json.addProperty("completedVisits", timing.getCompletedVisits());
        json.addProperty("lastActualArrival", timing.getLastActualArrival());
        json.addProperty("lastActualDeparture", timing.getLastActualDeparture());
        return json;
    }

    private static JsonObject buildTimes(StopTimes times) {
        JsonObject json = new JsonObject();
        json.addProperty("arrival", times.arrival());
        json.addProperty("departure", times.departure());
        json.addProperty("minDeparture", times.minDeparture());
        return json;
    }

    public static void recordArrival(TrackedTrain train, long now, int entryIndex, int transitTicks, boolean traveled) {
        if (!active) {
            return;
        }
        JsonObject json = eventBase(train, now, "arrival");
        json.addProperty("entryIndex", entryIndex);
        json.addProperty("transitTicks", transitTicks);
        json.addProperty("traveled", traveled);
        writeLine(json);
    }

    public static void recordDeparture(TrackedTrain train, long now, int entryIndex, int dwellTicks) {
        if (!active) {
            return;
        }
        JsonObject json = eventBase(train, now, "departure");
        json.addProperty("entryIndex", entryIndex);
        json.addProperty("dwellTicks", dwellTicks);
        train.getTimings(entryIndex).ifPresent(timing -> {
            json.addProperty("dwellResidual", timing.dwellResidualTicks());
            json.addProperty("dwellResidualLastMeasurement", timing.dwellResidual().lastMeasurement());
        });
        writeLine(json);
    }

    public static void recordReferenceChanged(TrackedTrain train, long now, int entryIndex, StopTimings timing) {
        if (!active) {
            return;
        }
        JsonObject json = eventBase(train, now, "reference_changed");
        json.addProperty("entryIndex", entryIndex);
        json.addProperty("newReference", timing.legDuration().get());
        json.addProperty("lastMeasurement", timing.legDuration().lastMeasurement());
        JsonArray history = new JsonArray();
        for (int value : timing.legDuration().getHistory()) {
            history.add(value);
        }
        json.add("history", history);
        writeLine(json);
    }

    public static void recordSoftReset(TrackedTrain train, long now, boolean wasDelayed, long deviationBeforeReset) {
        if (!active) {
            return;
        }
        JsonObject json = eventBase(train, now, "soft_reset");
        json.addProperty("wasDelayed", wasDelayed);
        json.addProperty("deviationBeforeReset", deviationBeforeReset);
        writeLine(json);
    }

    public static void recordRouteChoice(UUID trainId, String trainName, String chosenStation, int chosenIndex,
        boolean waiting, Collection<String> passed, Collection<String> notes) {
        if (!active) {
            return;
        }
        JsonObject json = new JsonObject();
        json.addProperty("event", "routeChoice");
        json.addProperty("trainId", trainId == null ? "" : trainId.toString());
        json.addProperty("trainName", trainName == null ? "" : trainName);
        json.addProperty("chosenIndex", chosenIndex);
        json.addProperty("chosenStation", chosenStation == null ? "" : chosenStation);
        json.addProperty("waiting", waiting);

        JsonArray skipped = new JsonArray();
        passed.forEach(skipped::add);
        json.add("passed", skipped);

        JsonArray detail = new JsonArray();
        notes.forEach(detail::add);
        json.add("notes", detail);
        writeLine(json);
    }

    private static JsonObject eventBase(TrackedTrain train, long now, String event) {
        JsonObject json = new JsonObject();
        json.addProperty("event", event);
        json.addProperty("now", now);
        addTrainIdentity(json, train);
        return json;
    }

    private static void addTrainIdentity(JsonObject json, TrackedTrain train) {
        json.addProperty("trainId", train.getTrainId().toString());
        json.addProperty("trainName", train.getTrainName());
    }

    private static JsonObject configHeader() {
        JsonObject json = new JsonObject();
        json.addProperty("event", "config");
        json.addProperty("totalDurationBufferSize", ModCommonConfig.TOTAL_DURATION_BUFFER_SIZE.get());
        json.addProperty("totalDurationDeviationThreshold", ModCommonConfig.TOTAL_DURATION_DEVIATION_THRESHOLD.get());
        json.addProperty("scheduleDeviationThreshold", ModCommonConfig.SCHEDULE_DEVIATION_THRESHOLD.get());
        json.addProperty("autoResetTimings", ModCommonConfig.AUTO_RESET_TIMINGS.get());
        return json;
    }

    private static synchronized void writeLine(JsonObject json) {
        if (!active || writer == null) {
            return;
        }
        try {
            writer.write(json.toString());
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            CreateRailwaysNavigator.LOGGER.error("[Backend] Unable to write diagnostics line.", e);
        }
    }
}
