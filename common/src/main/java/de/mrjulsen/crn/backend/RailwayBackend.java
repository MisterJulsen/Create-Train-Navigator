package de.mrjulsen.crn.backend;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.backend.debug.BackendDiagnosticsRecorder;
import de.mrjulsen.crn.backend.api.event.RailwayBackendEvents;
import de.mrjulsen.crn.backend.delay.ExternalDelayReports;
import de.mrjulsen.crn.backend.util.StationLookup;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.mcdragonlib.DragonLib;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Lifecycle of the train data backend: starts and stops the {@link TrainManager} with the server,
 * drives the update cycle, persists the learned data with the world and corrects all timestamps
 * after a world time jump.
 *
 * <h2>Threading</h2>
 * The backend is split across two threads so the prediction math does not run inside the server
 * tick, which would be far too expensive on a large network:
 * <ul>
 *   <li><b>Server thread</b> - the per-tick observation of every train
 *       ({@link TrainManager#tickLive(long)}) and the
 *       {@linkplain TrainManager#prepareFullUpdate(long) preparation pass} that reads what the
 *       calculation needs from the live train objects.</li>
 *   <li><b>Worker thread</b> - the full update ({@link TrainManager#runFullUpdate(long)}):
 *       projection, timetable maintenance and delay detection, all on the backend's own data.</li>
 * </ul>
 */
public final class RailwayBackend {

    private static final String FILENAME = CreateRailwaysNavigator.MOD_ID + "_backend.nbt";

    private static final String WORKER_THREAD_NAME = "CRN Railway Backend";

    /** How often, in ticks, the full update runs. */
    private static final int FULL_UPDATE_INTERVAL = 100;

    /** How long the server waits for a running full update when shutting down. */
    private static final long WORKER_SHUTDOWN_TIMEOUT_SECONDS = 10;

    /** How far the raw world time advances during one regular server tick. */
    private static final long REGULAR_TIME_ADVANCE = 1;

    /** A jump of at least this many transformed ticks is logged as an actual event. */
    private static final long TIME_JUMP_LOG_THRESHOLD = 40;

    private static volatile boolean active = false;
    private static MinecraftServer server;
    private static int tickCounter = 0;
    private static long lastRawWorldTime = Long.MIN_VALUE;

    private static ExecutorService worker;

    /** Guards against dispatching a new full update while the previous one is still running. */
    private static final AtomicBoolean fullUpdateRunning = new AtomicBoolean(false);

    private RailwayBackend() {}

    /** Registers all lifecycle hooks. Call once during mod initialization. */
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

    /** Whether the backend is currently running, i.e. a server is active. */
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

    /** Drives the update cycle. Called every tick on the server thread. */
    private static void tick() {
        if (!active) {
            return;
        }

        handleTimeJump();

        long now = ModUtils.getTransformedWorldTime();
        TrainManager manager = TrainManager.getInstance();
        manager.tickLive(now);

        // Dispatched here so listeners only ever run on the server thread, never inside an update.
        RailwayBackendEvents.dispatchPending();

        tickCounter++;
        if (tickCounter >= FULL_UPDATE_INTERVAL) {
            tickCounter = 0;
            dispatchFullUpdate(manager, now);
        }
    }

    /**
     * Runs the preparation pass on the server thread and hands the calculation to the worker. If
     * the previous full update is still running, this cycle is skipped rather than queueing up
     * work faster than it is processed.
     */
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

    /**
     * Detects world time jumps and shifts all stored timestamps accordingly.
     * <p>
     * Detection runs on the raw world time, where exactly one tick passes per server tick, so any
     * other difference is a jump and is corrected down to a single tick. On the transformed scale
     * this would need a tolerance, since a time system may advance that clock at a non-constant
     * rate, and every jump hidden inside the tolerance would offset all timestamps permanently.
     * The correction itself is the difference between the transformed time actually observed and
     * the one a regular tick would have produced, which stays exact at any rate.
     * <p>
     * A raw time that does not advance at all means the day-night cycle is frozen, which is not a
     * jump and is deliberately left uncorrected.
     */
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

    /** Persists the backend data, provided the backend is running. */
    public static synchronized void save() {
        if (!active) {
            return;
        }
        writeData();
    }

    /**
     * Writes the data to disk regardless of whether the backend is still active. Used by
     * {@link #stop()}, which shuts the backend down before saving so no update is in flight.
     */
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

    /** The world's data directory, or {@code null} while the backend is inactive. */
    public static File getDataDirectory() {
        return active && server != null ? new File(server.getWorldPath(new LevelResource("data")).toString()) : null;
    }
}
