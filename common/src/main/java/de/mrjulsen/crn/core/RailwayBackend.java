package de.mrjulsen.crn.core;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.core.debug.BackendDiagnosticsRecorder;
import de.mrjulsen.crn.api.event.RailwayBackendEvents;
import de.mrjulsen.crn.core.delay.ExternalDelayReports;
import de.mrjulsen.crn.core.util.StationLookup;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.mcdragonlib.DragonLib;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

public final class RailwayBackend {

    private static final String FILENAME = CreateRailwaysNavigator.MOD_ID + "_backend.nbt";
    private static final String WORKER_THREAD_NAME = "CRN Railway Backend";
    private static final int FULL_UPDATE_INTERVAL = 100;
    private static final long WORKER_SHUTDOWN_TIMEOUT_SECONDS = 10;
    private static final long REGULAR_TIME_ADVANCE = 1;
    private static final long TIME_JUMP_LOG_THRESHOLD = 40;

    private static volatile boolean active = false;
    private static MinecraftServer server;
    private static int tickCounter = 0;
    private static long lastRawWorldTime = Long.MIN_VALUE;

    private static ExecutorService worker;

    private static final AtomicBoolean fullUpdateRunning = new AtomicBoolean(false);

    private RailwayBackend() {}

    public static void init() {
        LifecycleEvent.SERVER_STARTED.register(RailwayBackend::start);
        LifecycleEvent.SERVER_STOPPING.register(s -> stop());
        LifecycleEvent.SERVER_LEVEL_SAVE.register(level -> {
            if (active && server != null && level == server.overworld()) {
                save();
            }
        });
        TickEvent.SERVER_POST.register(s -> tick());
    }

    public static boolean isActive() {
        return active;
    }

    private static void start(MinecraftServer currentServer) {
        server = currentServer;
        tickCounter = 0;
        lastRawWorldTime = Long.MIN_VALUE;

        TrainManager.closeInstance();
        TrainManager manager = TrainManager.getInstance();
        try {
            File file = getDataFile();
            if (file.exists()) {
                manager.loadNbt(NbtIo.readCompressed(file));
            }
        } catch (Exception e) {
            CreateRailwaysNavigator.LOGGER.error("[Backend] Unable to load backend data.", e);
        }

        shutdownWorker();
        worker = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, WORKER_THREAD_NAME);
            thread.setDaemon(true);
            return thread;
        });
        fullUpdateRunning.set(false);

        active = true;
        manager.synchronizeWithWorld();
        CreateRailwaysNavigator.LOGGER.info("[Backend] Train data backend started.");
    }

    private static void stop() {
        if (!active) {
            return;
        }
        active = false;
        shutdownWorker();

        writeData();
        BackendDiagnosticsRecorder.stop();
        ExternalDelayReports.clear();
        StationLookup.invalidate();
        RailwayBackendEvents.clear();
        server = null;
        TrainManager.closeInstance();
        CreateRailwaysNavigator.LOGGER.info("[Backend] Train data backend stopped.");
    }

    private static void shutdownWorker() {
        if (worker == null) {
            return;
        }
        worker.shutdown();
        try {
            if (!worker.awaitTermination(WORKER_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                CreateRailwaysNavigator.LOGGER.warn("[Backend] Full update did not finish in time and was abandoned.");
                worker.shutdownNow();
            }
        } catch (InterruptedException e) {
            worker.shutdownNow();
            Thread.currentThread().interrupt();
        }
        worker = null;
        fullUpdateRunning.set(false);
    }

    private static void tick() {
        if (!active) {
            return;
        }

        handleTimeJump();

        long now = ModUtils.getTransformedWorldTime();
        TrainManager manager = TrainManager.getInstance();
        manager.tickLive(now);

        RailwayBackendEvents.dispatchPending();

        tickCounter++;
        if (tickCounter >= FULL_UPDATE_INTERVAL) {
            tickCounter = 0;
            dispatchFullUpdate(manager, now);
        }
    }

    private static void dispatchFullUpdate(TrainManager manager, long now) {
        if (worker == null || !fullUpdateRunning.compareAndSet(false, true)) {
            return;
        }

        try {
            manager.prepareFullUpdate(now);
        } catch (Exception e) {
            fullUpdateRunning.set(false);
            CreateRailwaysNavigator.LOGGER.error("[Backend] Preparation of the full update failed.", e);
            return;
        }

        try {
            worker.execute(() -> {
                try {
                    manager.runFullUpdate(now);
                    BackendDiagnosticsRecorder.recordSnapshot(manager.getAllTrains(), now);
                } catch (Exception e) {
                    CreateRailwaysNavigator.LOGGER.error("[Backend] Full update failed.", e);
                } finally {
                    fullUpdateRunning.set(false);
                }
            });
        } catch (RuntimeException e) {
            fullUpdateRunning.set(false);
        }
    }

    private static void handleTimeJump() {
        long rawNow = DragonLib.getCurrentWorldTime();
        if (lastRawWorldTime != Long.MIN_VALUE) {
            long rawDiff = rawNow - lastRawWorldTime;
            if (rawDiff != REGULAR_TIME_ADVANCE && rawDiff != 0) {
                long expected = ModUtils.transformWorldTime(lastRawWorldTime + REGULAR_TIME_ADVANCE);
                long diff = ModUtils.transformWorldTime(rawNow) - expected;
                if (diff != 0) {
                    TrainManager.getInstance().shiftTimes(diff);
                    if (Math.abs(diff) >= TIME_JUMP_LOG_THRESHOLD) {
                        CreateRailwaysNavigator.LOGGER.info("[Backend] World time jumped by {} ticks. All timestamps have been corrected.", diff);
                    } else {
                        CreateRailwaysNavigator.LOGGER.debug("[Backend] World time jumped by {} ticks. All timestamps have been corrected.", diff);
                    }
                }
            }
        }
        lastRawWorldTime = rawNow;
    }

    public static synchronized void save() {
        if (!active) {
            return;
        }
        writeData();
    }

    private static synchronized void writeData() {
        if (server == null) {
            return;
        }
        try {
            NbtIo.writeCompressed(TrainManager.getInstance().toNbt(), getDataFile());
            CreateRailwaysNavigator.LOGGER.debug("[Backend] Saved backend data.");
        } catch (IOException e) {
            CreateRailwaysNavigator.LOGGER.error("[Backend] Unable to save backend data.", e);
        }
    }

    private static File getDataFile() {
        return new File(server.getWorldPath(new LevelResource("data/" + FILENAME)).toString());
    }

    public static File getDataDirectory() {
        return active && server != null ? new File(server.getWorldPath(new LevelResource("data")).toString()) : null;
    }
}
