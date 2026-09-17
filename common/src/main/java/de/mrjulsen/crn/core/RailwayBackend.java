package de.mrjulsen.crn.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.core.debug.BackendDiagnosticsRecorder;
import de.mrjulsen.crn.api.event.RailwayBackendEvents;
import de.mrjulsen.crn.core.delay.ExternalDelayReports;
import de.mrjulsen.crn.core.navigator.index.TimetableIndex;
import de.mrjulsen.crn.core.util.StationLookup;
import de.mrjulsen.crn.util.ModUtils;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

public final class RailwayBackend {

    private static final String FILENAME = CreateRailwaysNavigator.MOD_ID + "_train_data.nbt";
    private static final String WORKER_THREAD_NAME = "CRN Railway Backend";
    private static final String SAVE_THREAD_NAME = "CRN Railway Backend Save";
    private static final int FULL_UPDATE_INTERVAL = 100;
    private static final long WORKER_SHUTDOWN_TIMEOUT_SECONDS = 10;
    private static final long SAVE_SHUTDOWN_TIMEOUT_SECONDS = 30;
    private static final long REGULAR_TIME_ADVANCE = 1;
    private static final long TIME_JUMP_MIN_DELTA = 200;
    private static final long TIME_JUMP_LOG_THRESHOLD = 1200;

    private static volatile boolean active = false;
    private static MinecraftServer server;
    private static int tickCounter = 0;
    private static long expectedTransformedTime = Long.MIN_VALUE;

    private static ExecutorService worker;
    private static ExecutorService saveExecutor;

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
        expectedTransformedTime = Long.MIN_VALUE;

        TrainManager.closeInstance();
        TrainManager manager = TrainManager.getInstance();
        try {
            File file = getDataFile();
            if (file.exists()) {
                manager.loadNbt(NbtIo.readCompressed(file));
            }
        } catch (Exception e) {
            CreateRailwaysNavigator.LOGGER.error("[{}] Unable to load backend data.", WORKER_THREAD_NAME, e);
        }

        shutdownWorker();
        worker = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, WORKER_THREAD_NAME);
            thread.setDaemon(true);
            return thread;
        });
        fullUpdateRunning.set(false);

        shutdownSaveExecutor();
        saveExecutor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, SAVE_THREAD_NAME);
            thread.setDaemon(true);
            return thread;
        });

        active = true;
        manager.synchronizeWithWorld();
        CreateRailwaysNavigator.LOGGER.info("[{}] Train data backend started.", WORKER_THREAD_NAME);
    }

    private static void stop() {
        if (!active) {
            return;
        }
        active = false;
        shutdownWorker();
        shutdownSaveExecutor();

        writeFinal();
        BackendDiagnosticsRecorder.stop();
        ExternalDelayReports.clear();
        StationLookup.invalidate();
        RailwayBackendEvents.clear();
        server = null;
        TrainManager.closeInstance();
        CreateRailwaysNavigator.LOGGER.info("[{}] Train data backend stopped.", WORKER_THREAD_NAME);
    }

    private static void shutdownWorker() {
        if (worker == null) {
            return;
        }
        worker.shutdown();
        try {
            if (!worker.awaitTermination(WORKER_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                CreateRailwaysNavigator.LOGGER.warn("[{}] Full update did not finish in time and was abandoned.", WORKER_THREAD_NAME);
                worker.shutdownNow();
            }
        } catch (InterruptedException e) {
            worker.shutdownNow();
            Thread.currentThread().interrupt();
        }
        worker = null;
        fullUpdateRunning.set(false);
    }

    private static void shutdownSaveExecutor() {
        if (saveExecutor == null) {
            return;
        }
        saveExecutor.shutdown();
        try {
            if (!saveExecutor.awaitTermination(SAVE_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                CreateRailwaysNavigator.LOGGER.warn("[{}] A background save did not finish in time.", WORKER_THREAD_NAME);
                saveExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            saveExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        saveExecutor = null;
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
            CreateRailwaysNavigator.LOGGER.error("[{}] Preparation of the full update failed.", WORKER_THREAD_NAME, e);
            return;
        }

        try {
            worker.execute(() -> {
                try {
                    manager.runFullUpdate(now);
                    TimetableIndex.refreshIfWarm(now);
                    BackendDiagnosticsRecorder.recordSnapshot(manager.getAllTrains(), now);
                } catch (Exception e) {
                    CreateRailwaysNavigator.LOGGER.error("[{}] Full update failed.", WORKER_THREAD_NAME, e);
                } finally {
                    fullUpdateRunning.set(false);
                }
            });
        } catch (RuntimeException e) {
            fullUpdateRunning.set(false);
        }
    }

    private static void handleTimeJump() {
        long now = ModUtils.getTransformedWorldTime();
        if (expectedTransformedTime == Long.MIN_VALUE) {
            expectedTransformedTime = now;
            return;
        }

        expectedTransformedTime += REGULAR_TIME_ADVANCE;
        long diff = now - expectedTransformedTime;
        if (Math.abs(diff) >= TIME_JUMP_MIN_DELTA) {
            TrainManager.getInstance().shiftTimes(diff);
            expectedTransformedTime = now;
            if (Math.abs(diff) >= TIME_JUMP_LOG_THRESHOLD) {
                CreateRailwaysNavigator.LOGGER.info("[{}] World time jumped by {} ticks. All timestamps have been corrected.", WORKER_THREAD_NAME, diff);
            } else {
                CreateRailwaysNavigator.LOGGER.debug("[{}] World time jumped by {} ticks. All timestamps have been corrected.", WORKER_THREAD_NAME, diff);
            }
        }
    }

    public static void save() {
        if (!active || server == null) {
            return;
        }
        CompoundTag data = TrainManager.getInstance().toNbt();
        File file = getDataFile();
        ExecutorService executor = saveExecutor;
        if (executor == null || executor.isShutdown()) {
            writeData(data, file);
            return;
        }
        executor.execute(() -> writeData(data, file));
    }

    private static void writeFinal() {
        if (server == null) {
            return;
        }
        writeData(TrainManager.getInstance().toNbt(), getDataFile());
    }

    private static void writeData(CompoundTag data, File file) {
        long ms = System.currentTimeMillis();
        try {
            File temp = new File(file.getParentFile(), file.getName() + ".tmp");
            NbtIo.writeCompressed(data, temp);
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            CreateRailwaysNavigator.LOGGER.debug("[{}] Saved {} train backend data. Took {}ms.", WORKER_THREAD_NAME, CreateRailwaysNavigator.SHORT_MOD_ID, System.currentTimeMillis() - ms);
        } catch (IOException e) {
            CreateRailwaysNavigator.LOGGER.error("[{}] Unable to save {} train backend data. Took {}ms.", WORKER_THREAD_NAME, CreateRailwaysNavigator.SHORT_MOD_ID, System.currentTimeMillis() - ms, e);
        }
    }

    private static File getDataFile() {
        return new File(server.getWorldPath(new LevelResource("data/" + FILENAME)).toString());
    }

    public static File getDataDirectory() {
        return active && server != null ? new File(server.getWorldPath(new LevelResource("data")).toString()) : null;
    }
}
