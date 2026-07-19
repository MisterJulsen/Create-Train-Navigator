package de.mrjulsen.crn.backend;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.backend.api.event.RailwayBackendEvents;
import de.mrjulsen.crn.backend.core.ServiceState;
import de.mrjulsen.crn.backend.core.TrackedTrain;
import de.mrjulsen.crn.backend.history.DepartureLog;
import de.mrjulsen.crn.backend.history.DepartureLogEntry;
import de.mrjulsen.crn.backend.index.StationCallIndex;
import de.mrjulsen.crn.backend.schedule.JourneyStop;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.data.train.TrainUtils;
import de.mrjulsen.crn.config.ModCommonConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/**
 * The registry of all trains observed by the backend. Keeps one {@link TrackedTrain} per valid,
 * non-blacklisted train in the world and drives their update cycle.
 * <p>
 * The cycle is split across two threads (see {@link RailwayBackend}): {@link #tickLive(long)} and
 * {@link #prepareFullUpdate(long)} run on the server thread, {@link #runFullUpdate(long)} on the
 * worker. All data structures here are concurrent, so queries are safe from any thread.
 */
public final class TrainManager {

    private TrainManager() {}

    private static volatile TrainManager instance = new TrainManager();

    private final Map<UUID, TrackedTrain> trains = new ConcurrentHashMap<>();
    private final DepartureLog departureLog = new DepartureLog();
    private final StationCallIndex callIndex = new StationCallIndex();

    /** Persisted data of trains that have not been discovered in this session yet. */
    private final Map<UUID, CompoundTag> stagedTrainData = new ConcurrentHashMap<>();

    /**
     * Trains taken out of service deliberately, whose data has been discarded. Kept so they are not
     * rediscovered on the next pass, and persisted so a restart does not put every parked train
     * back on the boards for another display duration.
     */
    private final Set<UUID> retired = ConcurrentHashMap.newKeySet();

    /** The current instance, creating one if the backend has none. */
    public static synchronized TrainManager getInstance() {
        if (instance == null) instance = new TrainManager();
        return instance;
    }

    /** Drops the current instance. The next {@link #getInstance()} creates an empty one. */
    public static synchronized void closeInstance() {
        instance = null;
    }

    /** All currently tracked trains. */
    public Collection<TrackedTrain> getAllTrains() {
        return trains.values();
    }

    /** The tracked train with the given id, if it is being tracked. */
    public Optional<TrackedTrain> getTrain(UUID trainId) {
        return Optional.ofNullable(trainId == null ? null : trains.get(trainId));
    }

    /** Whether a train with the given id is being tracked. */
    public boolean hasTrain(UUID trainId) {
        return trainId != null && trains.containsKey(trainId);
    }

    /** The recorded past departures per station. */
    public DepartureLog getDepartureLog() {
        return departureLog;
    }

    /** Which trains call at which station. Rebuilt once per full update. */
    public StationCallIndex getCallIndex() {
        return callIndex;
    }

    /** Resets the timetables of all trains to their projected times. Learned durations are kept. */
    public void softResetAll() {
        trains.values().forEach(TrackedTrain::requestSoftReset);
    }

    /**
     * Discards everything the backend has stored, leaving no state that could resurrect a train
     * with its old data on the next discovery pass.
     */
    public void hardResetAll() {
        clear();
    }

    /**
     * Observes every train. Runs on the server thread, since it reads the live train state
     * (navigation, schedule runtime, carriages) every tick.
     */
    public void tickLive(long now) {
        for (TrackedTrain train : trains.values()) {
            try {
                train.tickLive(now);
            } catch (Exception e) {
                CreateRailwaysNavigator.LOGGER.error("[Backend] Tick failed for train '{}' ({}).", train.getTrainName(), train.getTrainId(), e);
            }
        }
    }

    /**
     * First half of the full update, on the server thread: discovers trains and lets each read what
     * the calculation needs from the live train objects. Everything that iterates a collection
     * owned by the game happens here, where no other thread is mutating it.
     *
     * @see TrackedTrain#prepareFullUpdate(long)
     */
    public void prepareFullUpdate(long now) {
        synchronizeWithWorld();
        for (TrackedTrain train : trains.values()) {
            try {
                train.prepareFullUpdate(now);
            } catch (Exception e) {
                CreateRailwaysNavigator.LOGGER.error("[Backend] Full update preparation failed for train '{}' ({}).", train.getTrainName(), train.getTrainId(), e);
            }
        }
        // After the journeys, so the index never points at a replaced journey.
        callIndex.rebuild(trains.values());
    }

    /**
     * Second half of the full update, on the worker thread: projection, timetable maintenance and
     * delay detection, working on the data captured by {@link #prepareFullUpdate(long)}.
     */
    public void runFullUpdate(long now) {
        for (TrackedTrain train : trains.values()) {
            try {
                train.fullUpdate(now);
            } catch (Exception e) {
                CreateRailwaysNavigator.LOGGER.error("[Backend] Full update failed for train '{}' ({}).", train.getTrainName(), train.getTrainId(), e);
            }
        }
    }

    /**
     * Starts tracking newly discovered trains and drops the ones that no longer exist. Part of the
     * periodic full update, but also called once at server start: a station transition happening
     * before the first update would go unobserved and desync that stop's scheduled time by a full
     * cycle, self-correcting only on the train's next visit.
     */
    public void synchronizeWithWorld() {
        TrainUtils.refreshCache();

        forgetExpiredTrains();

        for (Train train : TrainUtils.getRailwayManager().trains.values()) {
            if (retired.contains(train.id)) {
                if (!isOperable(train)) {
                    continue;
                }
                retired.remove(train.id);
            }
            if (!TrainUtils.isTrainValid(train)) {
                if (!trains.containsKey(train.id) && wasDisruptedInSave(train.id)) {
                    trains.put(train.id, track(train));
                }
                continue;
            }
            trains.computeIfAbsent(train.id, id -> track(train));
        }

        trains.keySet().removeIf(id -> !TrainUtils.getRailwayManager().trains.containsKey(id));
        retired.removeIf(id -> !TrainUtils.getRailwayManager().trains.containsKey(id));
        departureLog.retainStations(TrainUtils.getAllStationNames());
    }

    /**
     * Drops trains whose data is no longer worth keeping, remembering them as retired so they are
     * not immediately rediscovered. They are picked up again from scratch as soon as they run.
     *
     * @see TrackedTrain#shouldBeForgotten()
     */
    private void forgetExpiredTrains() {
        for (TrackedTrain train : trains.values()) {
            if (!train.shouldBeForgotten()) {
                continue;
            }
            UUID id = train.getTrainId();
            trains.remove(id);
            stagedTrainData.remove(id);
            retired.add(id);
            RailwayBackendEvents.fireTrainForgotten(id);
            if (CreateRailwaysNavigator.isDebug() || ModCommonConfig.ADVANCED_LOGGING.get()) {
                CreateRailwaysNavigator.LOGGER.info("[Backend] Train '{}' is no longer in service. Its data has been discarded.", train.getTrainName());
            }
        }
    }

    /** Whether a train is in a state in which the backend has anything to track. */
    private static boolean isOperable(Train train) {
        return TrainUtils.isTrainValid(train) && !train.derailed && train.runtime != null && !train.runtime.paused;
    }

    /** Starts tracking a train, restoring its persisted data if this session has not seen it yet. */
    private TrackedTrain track(Train train) {
        TrackedTrain tracked = new TrackedTrain(train, this);
        CompoundTag staged = stagedTrainData.remove(train.id);
        if (staged != null) {
            try {
                tracked.loadNbt(staged);
            } catch (Exception e) {
                CreateRailwaysNavigator.LOGGER.warn("[Backend] Unable to restore persisted data of train '{}'.", train.name.getString(), e);
            }
        }
        // After the restore, so a listener sees the train with whatever it already knows.
        RailwayBackendEvents.fireTrainTracked(tracked);
        return tracked;
    }

    /** Whether the save file knows this train as one that broke down while it was in service. */
    private boolean wasDisruptedInSave(UUID trainId) {
        CompoundTag staged = stagedTrainData.get(trainId);
        return staged != null && TrackedTrain.readPersistedServiceState(staged) == ServiceState.DISRUPTED;
    }

    /** Records a departure in the departure log. Called by tracked trains. */
    public void recordDeparture(TrackedTrain train, JourneyStop stop, long now) {
        departureLog.record(stop.getStationName(), new DepartureLogEntry(
            now,
            train.getTrainId(),
            train.getTrainName(),
            stop.getSection() == null ? null : stop.getSection().getTrainLineId(),
            stop.getSection() == null ? null : stop.getSection().getTrainCategoryId(),
            train.getSectionDestination(stop)
        ));
    }

    /** Shifts all absolute timestamps by the given amount, after a world time jump. */
    public void shiftTimes(long ticks) {
        trains.values().forEach(x -> x.shiftTimes(ticks));
        departureLog.shiftTimes(ticks);
    }

    /** Removes all tracked trains, staged data, retired ids and the departure log. */
    public void clear() {
        trains.clear();
        stagedTrainData.clear();
        retired.clear();
        departureLog.clear();
        callIndex.clear();
    }

    private static final String NBT_TRAINS = "Trains";
    private static final String NBT_DEPARTURE_LOG = "DepartureLog";
    private static final String NBT_RETIRED = "RetiredTrains";

    /** Serializes all tracked and staged train data. */
    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();

        CompoundTag trainsNbt = new CompoundTag();
        for (Map.Entry<UUID, TrackedTrain> entry : trains.entrySet()) {
            trainsNbt.put(entry.getKey().toString(), entry.getValue().toNbt());
        }
        for (Map.Entry<UUID, CompoundTag> entry : stagedTrainData.entrySet()) {
            if (!trainsNbt.contains(entry.getKey().toString())) {
                trainsNbt.put(entry.getKey().toString(), entry.getValue());
            }
        }
        nbt.put(NBT_TRAINS, trainsNbt);

        ListTag retiredNbt = new ListTag();
        retired.forEach(id -> retiredNbt.add(StringTag.valueOf(id.toString())));
        nbt.put(NBT_RETIRED, retiredNbt);

        nbt.put(NBT_DEPARTURE_LOG, departureLog.toNbt());
        return nbt;
    }

    /** Restores persisted data. Trains are only instantiated once they are discovered. */
    public void loadNbt(CompoundTag nbt) {
        stagedTrainData.clear();
        retired.clear();

        ListTag retiredNbt = nbt.getList(NBT_RETIRED, Tag.TAG_STRING);
        for (int i = 0; i < retiredNbt.size(); i++) {
            try {
                retired.add(UUID.fromString(retiredNbt.getString(i)));
            } catch (IllegalArgumentException e) {
                CreateRailwaysNavigator.LOGGER.warn("[Backend] Skipping retired train with invalid id '{}'.", retiredNbt.getString(i));
            }
        }

        CompoundTag trainsNbt = nbt.getCompound(NBT_TRAINS);
        for (String key : trainsNbt.getAllKeys()) {
            try {
                stagedTrainData.put(UUID.fromString(key), trainsNbt.getCompound(key));
            } catch (IllegalArgumentException e) {
                CreateRailwaysNavigator.LOGGER.warn("[Backend] Skipping persisted data with invalid train id '{}'.", key);
            }
        }
        departureLog.loadNbt(nbt.getCompound(NBT_DEPARTURE_LOG));
    }
}
