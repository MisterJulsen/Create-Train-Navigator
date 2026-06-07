package de.mrjulsen.crn.api.web;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import de.mrjulsen.crn.data.train.TrainListener;
import de.mrjulsen.crn.data.train.TrainUtils;

/** Cached JSON snapshot of all train schedules for the web API. */
public final class TrainScheduleCache {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final AtomicReference<CachedSnapshot> snapshot = new AtomicReference<>(CachedSnapshot.empty());

    private TrainScheduleCache() {}

    public static CachedSnapshot getSnapshot() {
        return snapshot.get();
    }

    public static int getTrackedTrainCount() {
        return snapshot.get().trainCountFromBody();
    }

    public static void rebuild() {
        long tick = TrainScheduleJsonExporter.currentWorldTick();
        JsonObject root = TrainScheduleJsonExporter.buildAllTrainsRoot(tick);
        byte[] body = GSON.toJson(root).getBytes(StandardCharsets.UTF_8);
        String etag = Integer.toHexString(java.util.Arrays.hashCode(body));
        snapshot.set(new CachedSnapshot(body, etag, tick));
    }

    public static byte[] buildSingleTrainBody(UUID trainId) {
        long tick = TrainScheduleJsonExporter.currentWorldTick();
        return TrainListener.getTrainData(trainId)
            .filter(data -> data.getTrain() != null && data.getTrain().runtime != null)
            .map(data -> GSON.toJson(TrainScheduleJsonExporter.buildTrainRoot(data, tick)).getBytes(StandardCharsets.UTF_8))
            .or(() -> TrainUtils.getTrain(trainId)
                .map(train -> GSON.toJson(TrainScheduleJsonExporter.buildTrainRootFromCreate(train, tick)).getBytes(StandardCharsets.UTF_8)))
            .orElse(null);
    }

    public record CachedSnapshot(byte[] body, String etag, long worldTick) {
        static CachedSnapshot empty() {
            return new CachedSnapshot("{\"v\":1,\"t\":0,\"trains\":[],\"trainCount\":0}".getBytes(StandardCharsets.UTF_8), "0", 0L);
        }

        /** Parse trainCount from cached JSON without a full deserialize. */
        int trainCountFromBody() {
            String json = new String(body, StandardCharsets.UTF_8);
            int idx = json.indexOf("\"trainCount\":");
            if (idx < 0) {
                return 0;
            }
            int start = idx + 13;
            int end = start;
            while (end < json.length() && Character.isDigit(json.charAt(end))) {
                end++;
            }
            try {
                return Integer.parseInt(json.substring(start, end));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }
}
