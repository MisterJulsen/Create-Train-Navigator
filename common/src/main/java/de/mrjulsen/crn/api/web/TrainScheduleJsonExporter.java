package de.mrjulsen.crn.api.web;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.schedule.destination.DestinationInstruction;

import de.mrjulsen.crn.data.train.TrainData;
import de.mrjulsen.crn.data.train.TrainListener;
import de.mrjulsen.crn.data.train.TrainPrediction;
import de.mrjulsen.crn.data.train.TrainUtils;

/** Builds compact JSON schedule payloads aligned with the gafata CRN export format. */
public final class TrainScheduleJsonExporter {

    private TrainScheduleJsonExporter() {}

    public static JsonObject buildAllTrainsRoot(long worldTick) {
        JsonObject root = new JsonObject();
        root.addProperty("v", 1);
        root.addProperty("t", worldTick);

        JsonArray trains = new JsonArray();
        for (TrainData data : TrainListener.getAllTrainData()) {
            if (!shouldExport(data)) {
                continue;
            }
            trains.add(buildTrain(data, worldTick));
        }
        root.add("trains", trains);
        root.addProperty("trainCount", trains.size());
        return root;
    }

    public static JsonObject buildTrainRoot(TrainData data, long worldTick) {
        JsonObject root = new JsonObject();
        root.addProperty("v", 1);
        root.addProperty("t", worldTick);
        root.add("train", buildTrain(data, worldTick));
        return root;
    }

    public static JsonObject buildTrainRootFromCreate(Train train, long worldTick) {
        JsonObject root = new JsonObject();
        root.addProperty("v", 1);
        root.addProperty("t", worldTick);
        root.add("train", buildTrainFromCreate(train, worldTick));
        return root;
    }

    public static JsonObject buildTrainFromCreate(Train train, long worldTick) {
        JsonObject obj = new JsonObject();
        obj.addProperty("trainId", train.id.toString());
        obj.addProperty("train", train.name.getString());
        obj.addProperty("atStation", train.getCurrentStation() != null);
        if (train.runtime != null) {
            obj.addProperty("currentIndex", train.runtime.currentEntry);
            obj.addProperty("hasSchedule", train.runtime.getSchedule() != null);
        }
        WebApiJsonHelper.addPosition(obj, train);
        TrainListener.getTrainData(train.id).ifPresent(data -> {
            WebApiJsonHelper.addLineFromSection(obj, data);
            if (data.hasPredictions()) {
                obj.add("schedule", buildScheduleOnly(data, worldTick));
            }
        });
        return obj;
    }

    public static JsonObject buildTrain(TrainData data, long worldTick) {
        JsonObject train = new JsonObject();
        train.addProperty("trainId", data.getTrainId().toString());
        train.addProperty("train", data.getTrain().name.getString());
        train.addProperty("currentIndex", data.getCurrentScheduleIndex());
        train.addProperty("atStation", data.isAtStation());
        train.addProperty("cancelled", data.isCancelled());

        WebApiJsonHelper.addLineFromSection(train, data);
        WebApiJsonHelper.addPosition(train, data.getTrain());

        data.getCurrentSection().getTrainLine().ifPresent(line -> {
            if (!train.has("lineId")) {
                train.addProperty("lineId", line.getId().toString());
            }
        });

        List<Integer> destinationIndices = destinationIndices(data);
        int currentIndex = data.getCurrentScheduleIndex();
        boolean atStation = data.isAtStation();

        Optional<TrainPrediction> currentPred = atStation ? data.getPredictionByIndex(currentIndex) : Optional.empty();
        Optional<TrainPrediction> nextPred = resolveNextPrediction(data, destinationIndices, currentIndex, atStation);
        Optional<TrainPrediction> lastPred = resolveLastPrediction(data, destinationIndices, currentIndex, atStation);

        currentPred.ifPresent(pred -> train.add("current", stepRef(pred, worldTick, "current")));
        nextPred.ifPresent(pred -> train.add("next", stepRef(pred, worldTick, "next")));
        lastPred.ifPresent(pred -> train.add("last", stepRef(pred, worldTick, "last")));

        long ticksUntilDeparture = 0;
        long ticksUntilNextStop = 0;

        if (atStation) {
            ticksUntilDeparture = currentPred
                .map(p -> ticksUntil(p.realTime().departureTime(), worldTick))
                .orElse(0L);
            ticksUntilNextStop = nextPred
                .map(p -> ticksUntil(p.realTime().arrivalTime(), worldTick))
                .orElse(ticksUntilDeparture);
        } else {
            Optional<TrainPrediction> enRouteTarget = data.getPredictionByIndex(currentIndex);
            ticksUntilNextStop = enRouteTarget
                .map(p -> Math.max((long) data.ticksToNextStop, ticksUntil(p.realTime().arrivalTime(), worldTick)))
                .orElse((long) Math.max(0, data.ticksToNextStop));
            enRouteTarget.ifPresent(pred -> {
                if (!train.has("next")) {
                    train.add("next", stepRef(pred, worldTick, "next"));
                }
            });
        }

        if (ticksUntilDeparture > 0) {
            train.addProperty("ticksUntilDeparture", ticksUntilDeparture);
            train.addProperty("secondsUntilDeparture", ticksToSeconds(ticksUntilDeparture));
        }
        if (ticksUntilNextStop > 0) {
            train.addProperty("ticksUntilNextStop", ticksUntilNextStop);
            train.addProperty("secondsUntilNextStop", ticksToSeconds(ticksUntilNextStop));
        }

        nextPred.ifPresent(pred -> train.addProperty("nextStop", pred.getRealTimeStationName()));
        WebApiJsonHelper.addNextStopDistance(train, data, atStation);

        JsonArray schedule = new JsonArray();
        List<TrainPrediction> steps = data.getPredictionsChronologically().stream()
            .sorted(Comparator.comparingInt(TrainPrediction::getEntryIndex))
            .toList();

        for (TrainPrediction pred : steps) {
            String stepState = stepState(pred.getEntryIndex(), currentIndex, atStation, currentPred, nextPred, lastPred);
            schedule.add(stepObject(pred, worldTick, stepState));
        }

        for (TrainPrediction pred : data.getPredictions().stream()
            .sorted(Comparator.comparingInt(TrainPrediction::getEntryIndex))
            .filter(p -> steps.stream().noneMatch(s -> s.getEntryIndex() == p.getEntryIndex()))
            .toList()) {
            String stepState = stepState(pred.getEntryIndex(), currentIndex, atStation, currentPred, nextPred, lastPred);
            schedule.add(stepObject(pred, worldTick, stepState));
        }

        train.add("schedule", schedule);
        train.addProperty("stepCount", schedule.size());
        return train;
    }

    public static JsonObject stepRef(TrainPrediction pred, long worldTick, String role) {
        JsonObject ref = new JsonObject();
        ref.addProperty("index", pred.getEntryIndex());
        ref.addProperty("station", pred.getRealTimeStationName());
        if (pred.getTitle() != null && !pred.getTitle().isEmpty()) {
            ref.addProperty("title", pred.getTitle());
        }
        ref.addProperty("role", role);
        ref.addProperty("realArrival", pred.realTime().arrivalTime());
        ref.addProperty("realDeparture", pred.realTime().departureTime());
        ref.addProperty("ticksUntilArrival", ticksUntil(pred.realTime().arrivalTime(), worldTick));
        ref.addProperty("ticksUntilDeparture", ticksUntil(pred.realTime().departureTime(), worldTick));
        return ref;
    }

    public static JsonObject stepObject(TrainPrediction pred, long worldTick, String state) {
        JsonObject step = new JsonObject();
        step.addProperty("index", pred.getEntryIndex());
        step.addProperty("station", pred.getRealTimeStationName());
        if (pred.getStationFilter() != null && !pred.getStationFilter().isEmpty()) {
            step.addProperty("filter", pred.getStationFilter());
        }
        if (pred.getTitle() != null && !pred.getTitle().isEmpty()) {
            step.addProperty("title", pred.getTitle());
        }
        step.addProperty("transitTime", pred.transitTime().value());
        step.addProperty("stayDuration", pred.getAverageStayDuration());
        step.addProperty("scheduledArrival", pred.scheduled().arrivalTime());
        step.addProperty("scheduledDeparture", pred.scheduled().departureTime());
        step.addProperty("realArrival", pred.realTime().arrivalTime());
        step.addProperty("realDeparture", pred.realTime().departureTime());
        step.addProperty("ticksUntilArrival", ticksUntil(pred.realTime().arrivalTime(), worldTick));
        step.addProperty("ticksUntilDeparture", ticksUntil(pred.realTime().departureTime(), worldTick));

        long delay = pred.getDepartureTimeDeviation();
        if (delay == 0) {
            delay = pred.getArrivalTimeDeviation();
        }
        if (delay != 0) {
            step.addProperty("delayTicks", delay);
        }
        if (state != null && !state.isEmpty()) {
            step.addProperty("state", state);
        }
        return step;
    }

    public static long currentWorldTick() {
        return WebApiSupport.currentWorldTick();
    }

    private static JsonArray buildScheduleOnly(TrainData data, long worldTick) {
        JsonArray schedule = new JsonArray();
        for (TrainPrediction pred : data.getPredictionsChronologically()) {
            schedule.add(stepObject(pred, worldTick, null));
        }
        return schedule;
    }

    private static boolean shouldExport(TrainData data) {
        return data.getTrain() != null
            && data.getTrain().runtime != null
            && data.getTrain().runtime.getSchedule() != null
            && data.hasPredictions();
    }

    private static Optional<TrainPrediction> resolveNextPrediction(
        TrainData data,
        List<Integer> destinationIndices,
        int currentIndex,
        boolean atStation
    ) {
        if (atStation) {
            int nextIndex = nextDestinationIndex(data.getTrain().runtime.getSchedule(), destinationIndices, currentIndex);
            return nextIndex >= 0 ? data.getPredictionByIndex(nextIndex) : Optional.empty();
        }
        return data.getPredictionByIndex(currentIndex);
    }

    private static Optional<TrainPrediction> resolveLastPrediction(
        TrainData data,
        List<Integer> destinationIndices,
        int currentIndex,
        boolean atStation
    ) {
        int lastIndex = previousDestinationIndex(data.getTrain().runtime.getSchedule(), destinationIndices, currentIndex);
        if (lastIndex < 0) {
            return Optional.empty();
        }
        if (!atStation && lastIndex == currentIndex) {
            lastIndex = previousDestinationIndex(data.getTrain().runtime.getSchedule(), destinationIndices, lastIndex);
        }
        return lastIndex >= 0 ? data.getPredictionByIndex(lastIndex) : Optional.empty();
    }

    private static String stepState(
        int entryIndex,
        int currentIndex,
        boolean atStation,
        Optional<TrainPrediction> current,
        Optional<TrainPrediction> next,
        Optional<TrainPrediction> last
    ) {
        if (atStation && entryIndex == currentIndex) {
            return "current";
        }
        if (next.isPresent() && next.get().getEntryIndex() == entryIndex) {
            return "next";
        }
        if (last.isPresent() && last.get().getEntryIndex() == entryIndex) {
            return "last";
        }
        if (!atStation && entryIndex == currentIndex) {
            return "next";
        }
        if (entryIndex < currentIndex) {
            return "past";
        }
        return "upcoming";
    }

    private static List<Integer> destinationIndices(TrainData data) {
        Schedule schedule = data.getTrain().runtime.getSchedule();
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < schedule.entries.size(); i++) {
            if (schedule.entries.get(i).instruction instanceof DestinationInstruction) {
                indices.add(i);
            }
        }
        return indices;
    }

    private static int nextDestinationIndex(Schedule schedule, List<Integer> destinationIndices, int currentIndex) {
        if (destinationIndices.isEmpty()) {
            return -1;
        }
        int pos = destinationIndices.indexOf(currentIndex);
        if (pos < 0) {
            for (int idx : destinationIndices) {
                if (idx > currentIndex) {
                    return idx;
                }
            }
            return schedule.cyclic ? destinationIndices.get(0) : -1;
        }
        if (pos + 1 < destinationIndices.size()) {
            return destinationIndices.get(pos + 1);
        }
        return schedule.cyclic ? destinationIndices.get(0) : -1;
    }

    private static int previousDestinationIndex(Schedule schedule, List<Integer> destinationIndices, int currentIndex) {
        if (destinationIndices.isEmpty()) {
            return -1;
        }
        int pos = destinationIndices.indexOf(currentIndex);
        if (pos < 0) {
            int prev = -1;
            for (int idx : destinationIndices) {
                if (idx < currentIndex) {
                    prev = idx;
                }
            }
            return prev >= 0 ? prev : (schedule.cyclic ? destinationIndices.get(destinationIndices.size() - 1) : -1);
        }
        if (pos > 0) {
            return destinationIndices.get(pos - 1);
        }
        return schedule.cyclic ? destinationIndices.get(destinationIndices.size() - 1) : -1;
    }

    private static long ticksUntil(long targetTick, long worldTick) {
        return Math.max(0, targetTick - worldTick);
    }

    private static int ticksToSeconds(long ticks) {
        return (int) (ticks / 20L);
    }
}
