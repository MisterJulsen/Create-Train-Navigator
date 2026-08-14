package de.mrjulsen.crn.core;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.api.event.RailwayBackendEvents;
import de.mrjulsen.crn.core.train.ServiceState;
import de.mrjulsen.crn.core.train.TrackedTrain;
import de.mrjulsen.crn.core.history.DepartureLog;
import de.mrjulsen.crn.core.index.StationCallIndex;
import de.mrjulsen.crn.core.schedule.JourneyStop;
import de.mrjulsen.crn.util.TrainUtils;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.event.ModCommonEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public final class TrainManager {

    private TrainManager() {}

    private static volatile TrainManager instance = new TrainManager();

    private final Map<UUID, TrackedTrain> trains = new ConcurrentHashMap<>();
    private final DepartureLog departureLog = new DepartureLog();
    private final StationCallIndex callIndex = new StationCallIndex();

    private final Map<UUID, CompoundTag> stagedTrainData = new ConcurrentHashMap<>();

    private final Set<UUID> retired = ConcurrentHashMap.newKeySet();

    public static synchronized TrainManager getInstance() {
        if (instance == null) instance = new TrainManager();
        return instance;
    }

    public static synchronized void closeInstance() {
        instance = null;
    }

    public Collection<TrackedTrain> getAllTrains() {
        return trains.values();
    }

    public Optional<TrackedTrain> getTrain(UUID trainId) {
        return Optional.ofNullable(trainId == null ? null : trains.get(trainId));
    }

    public boolean hasTrain(UUID trainId) {
        return trainId != null && trains.containsKey(trainId);
    }

    public DepartureLog getDepartureLog() {
        return departureLog;
    }

    public StationCallIndex getCallIndex() {
        return callIndex;
    }

    public void softResetAll() {
        trains.values().forEach(TrackedTrain::requestSoftReset);
    }

    public boolean softReset(UUID trainId) {
        TrackedTrain train = trainId == null ? null : trains.get(trainId);
        if (train == null) {
            return false;
        }
        train.requestSoftReset();
        return true;
    }

    public void hardResetAll() {
        clear();
    }

    public boolean hardReset(UUID trainId) {
        if (trainId == null) {
            return false;
        }
        boolean known = trains.remove(trainId) != null;
        known |= stagedTrainData.remove(trainId) != null;
        known |= retired.remove(trainId);
        if (known) {
            RailwayBackendEvents.fireTrainForgotten(trainId);
        }
        return known;
    }

    public void tickLive(long now) {
        for (TrackedTrain train : trains.values()) {
            try {
                train.tickLive(now);
            } catch (Exception e) {
                CreateRailwaysNavigator.LOGGER.error("[Backend] Tick failed for train '{}' ({}).", train.getTrainName(), train.getTrainId(), e);
            }
        }
    }

    public void prepareFullUpdate(long now) {
        synchronizeWithWorld();
        for (TrackedTrain train : trains.values()) {
            try {
                train.prepareFullUpdate(now);
            } catch (Exception e) {
                CreateRailwaysNavigator.LOGGER.error("[Backend] Full update preparation failed for train '{}' ({}).", train.getTrainName(), train.getTrainId(), e);
            }
        }
        callIndex.rebuild(trains.values());
    }

    public void runFullUpdate(long now) {
        for (TrackedTrain train : trains.values()) {
            try {
                train.fullUpdate(now);
            } catch (Exception e) {
                CreateRailwaysNavigator.LOGGER.error("[Backend] Full update failed for train '{}' ({}).", train.getTrainName(), train.getTrainId(), e);
            }
        }
    }

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

    private static boolean isOperable(Train train) {
        return TrainUtils.isTrainValid(train) && !train.derailed && train.runtime != null && !train.runtime.paused;
    }

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
        RailwayBackendEvents.fireTrainTracked(tracked);
        return tracked;
    }

    private boolean wasDisruptedInSave(UUID trainId) {
        CompoundTag staged = stagedTrainData.get(trainId);
        return staged != null && TrackedTrain.readPersistedServiceState(staged) == ServiceState.DISRUPTED;
    }

    public void recordDeparture(TrackedTrain train, JourneyStop stop) {
        long now = ModCommonEvents.getCurrentServer().map(server -> server.overworld().getGameTime()).orElse(-1L);
        if (now < 0) {
            return;
        }
        departureLog.record(
            stop.getStationName(),
            now,
            stop.getSection() == null ? null : stop.getSection().getTrainLineId(),
            stop.getSection() == null ? null : stop.getSection().getTrainCategoryId(),
            train.getTrainName()
        );
    }

    public void shiftTimes(long ticks) {
        trains.values().forEach(x -> x.shiftTimes(ticks));
    }

    public void clear() {
        trains.clear();
        stagedTrainData.clear();
        retired.clear();
        departureLog.clear();
        callIndex.clear();
    }

    private static final String NBT_VERSION = "Version";
    private static final String NBT_TRAINS = "Trains";
    private static final String NBT_DEPARTURE_LOG = "DepartureLog";
    private static final String NBT_RETIRED = "RetiredTrains";
    private static final int VERSION = 1;

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt(NBT_VERSION, VERSION);

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

    public void loadNbt(CompoundTag nbt) {
        int version = nbt.getInt(NBT_VERSION);
        if (version > VERSION) {
            CreateRailwaysNavigator.LOGGER.warn("[Backend] Backend data was written by a newer format (version {} > {}); loading it may be incomplete.", version, VERSION);
        }

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
