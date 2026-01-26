
package de.mrjulsen.crn.data.train;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import java.util.Set;

import com.simibubi.create.content.trains.entity.Train;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.data.schedule.INavigationExtension;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.event.CRNEventsManager;
import de.mrjulsen.crn.event.ModCommonEvents;
import de.mrjulsen.crn.event.events.GlobalTrainDisplayDataRefreshEventPost;
import de.mrjulsen.crn.event.events.GlobalTrainDisplayDataRefreshEventPre;
import de.mrjulsen.crn.event.events.ScheduleResetEvent;
import de.mrjulsen.crn.event.events.TotalDurationTimeChangedEvent;
import de.mrjulsen.crn.event.events.TrainArrivalAndDepartureEvent;
import de.mrjulsen.mcdragonlib.DragonLib;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.storage.LevelResource;

/** Monitors all trains in the world and processes their data and information to make it available for use. */
public final class TrainListener {

    private static final String FILENAME = CreateRailwaysNavigator.MOD_ID + "_train_data.nbt";
    private static final String NBT_TRAIN_DATA = "TrainData";
    private static final String NBT_DEPARTURE_HISTORY = "DepartureHistory";

    private static final ConcurrentHashMap<UUID /* train id */, TrainData> data = new ConcurrentHashMap<>();
	public static final Map<String, Collection<TrainPrediction>> statusByDestination = new HashMap<>();

    private static boolean trainDataListenerActive = false;
    private static long currentTrainDataListenerId = 0L;
    private static final Queue<Runnable> trainDataHookTasks = new ConcurrentLinkedQueue<>();


    public static Optional<TrainData> getTrainData(Train train) {
        return getTrainData(train.id);
    }
    
    public static Optional<TrainData> getTrainData(UUID trainId) {
        return hasTrainData(trainId) ? Optional.ofNullable(data.get(trainId)) : Optional.empty();
    }

    public static boolean hasTrainData(Train train) {
        return hasTrainData(train.id);
    }

    public static boolean hasTrainData(UUID trainId) {
        return data.containsKey(trainId);
    }

    public static Collection<TrainData> getAllTrainData() {
        return data.values();
    }

    public static void resetTrainData() {
        data.clear();
    }

    public static void resetTrainData(Train train) {
        resetTrainData(train.id);
    }

    public static void resetTrainData(UUID trainId) {
        data.remove(trainId);
    }


    public static void init() {
        // Register Event Listeners
        CRNEventsManager.getEvent(GlobalTrainDisplayDataRefreshEventPre.class).register(CreateRailwaysNavigator.MOD_ID, () -> {
            queueTrainListenerTask(() -> {
                try {
                    DepartureHistory.validate();
                    TrainListener.refreshPre();
                } catch (Exception e) {
                    DragonLib.LOGGER.error("Cannot run train listener task 'TrainListener#GlobalTrainDisplayDataRefreshEventPre': {}", e.getMessage(), e);
                }
            });
        });

        CRNEventsManager.getEvent(GlobalTrainDisplayDataRefreshEventPost.class).register(CreateRailwaysNavigator.MOD_ID, () -> {
            queueTrainListenerTask(() -> {
                try {
                    TrainUtils.refreshCache();
                    TrainListener.refreshPost();
                } catch (Exception e) {
                    DragonLib.LOGGER.error("Cannot run train listener task 'TrainListener#GlobalTrainDisplayDataRefreshEventPost': {}", e.getMessage(), e);
                }
            });
        });
        
        CRNEventsManager.getEvent(TotalDurationTimeChangedEvent.class).register(CreateRailwaysNavigator.MOD_ID, (train, old, newDuration) -> {
            if (ModCommonConfig.ADVANCED_LOGGING.get())
                CreateRailwaysNavigator.LOGGER.info("The total duration of the train {} ({}) has changed from {} Ticks to {} Ticks. This will result in changes to the scheduled departure times!", train.name.getString(), train.id, old, newDuration);
        });

        CRNEventsManager.getEvent(TrainArrivalAndDepartureEvent.class).register(CreateRailwaysNavigator.MOD_ID, (train, station, isArrival) -> {
            queueTrainListenerTask(() -> {
                try {                    
                    if (TrainUtils.canReadTrainNavigation(train)) {
                        if (!isArrival && station.isPresent() && !((INavigationExtension)(Object)train.navigation).isDelayedWaitConditionPending()) {
                            // If not checking whether a delayed condition is pending, the train would block itself.
                            DepartureHistory.updateDepartures(station.get().name, train);
                        }
                    } else {
                        if (ModCommonConfig.ADVANCED_LOGGING.get())
                            DragonLib.LOGGER.warn("Cannot run train listener task 'TrainListener#TrainArrivalAndDepartureEvent:2'. Unable to read the train navigation of train {}.", train == null ? "null" : train.id);
                    }                    
                } catch (Exception e) {
                    DragonLib.LOGGER.error("Cannot run train listener task 'TrainListener#TrainArrivalAndDepartureEvent': {}", e.getMessage(), e);
                }
            });
        });
        
        CRNEventsManager.getEvent(ScheduleResetEvent.class).register(CreateRailwaysNavigator.MOD_ID, (train, soft) -> {
            queueTrainListenerTask(() -> {
                try {
                    if (soft && data.containsKey(train.id)) {
                        TrainData trainData = data.get(train.id);
                        trainData.softResetPredictions();
                    } else {
                        resetTrainData(train);
                    }
                } catch (Exception e) {
                    DragonLib.LOGGER.error("Cannot run train listener task 'TrainListener#ScheduleResetEvent': {}", e.getMessage(), e);
                }
            });
        });
    }

    public static Set<Train> getAllTrains() {
        Set<Train> result = new HashSet<>(data.size());
        for (TrainData v : data.values()) {
            result.add(v.getTrain());
        }
        return result;
    }

    public static boolean allTrainsInitialized() {
        for (TrainData data : data.values()) {
            if (GlobalSettings.getInstance().isTrainBlacklisted(data.getTrain()) ||
                !data.hasPredictions() ||
                data.getTrain().runtime.paused ||
                data.getTrain().derailed ||
                data.getTrain().runtime.completed ||
                !TrainUtils.isTrainValid(data.getTrain())
            ) {
                continue;
            }

            if (!data.isInitialized() || data.isPreInitializationPhase()) {
                return false;
            }
        }
        return true;
    }

    public static void start() {
        new Thread(() -> {
            init();
            long id;
            do {
                id = System.nanoTime();
            } while (currentTrainDataListenerId == id);
    
            currentTrainDataListenerId = id;
            trainDataListenerActive = true;
            trainDataHookTasks.clear();
            TrainListener.data.clear();
            try {
                TrainListener.load();
            } catch (Exception e) {
                CreateRailwaysNavigator.LOGGER.error("Unable to load train listener data.", e);            
            }
            
            final long threadId = id;
            new Thread(() -> {
                try {
                    while (currentTrainDataListenerId == threadId && trainDataListenerActive) {
                        while (!trainDataHookTasks.isEmpty()) {
                            try {
                                trainDataHookTasks.poll().run();
                            } catch (Exception e) {
                                CreateRailwaysNavigator.LOGGER.error("Error while executing train listener task.", e);
                            }
                        }
        
                        try {
                            TimeUnit.SECONDS.sleep(1);
                        } catch (InterruptedException e) {
                            CreateRailwaysNavigator.LOGGER.error("Error while waiting for next task.", e);
                        }
                    }
                    save();
                    TrainListener.data.clear();
                    trainDataHookTasks.clear();
                    CreateRailwaysNavigator.LOGGER.info("Train listener has been stopped.");
                } catch (Exception e) {
                    CreateRailwaysNavigator.LOGGER.error("Error while executing Train Listener.", e);                
                }
            }, "CRN Train Listener").start();
            CreateRailwaysNavigator.LOGGER.info("Train listener has been started.");
        }, "CRN Train Listener Launcher").start();
    }

    public static void stop() {
        trainDataListenerActive = false;
        CreateRailwaysNavigator.LOGGER.info("Stopping train listener...");
    }

    public static synchronized void save() {
        if (!trainDataListenerActive) {
            return;
        }

        CompoundTag dataNbt = new CompoundTag();
        data.entrySet().forEach(x -> dataNbt.put(x.getKey().toString(), x.getValue().toNbt()));

        CompoundTag nbt = new CompoundTag();
        nbt.put(NBT_TRAIN_DATA, dataNbt);
        nbt.put(NBT_DEPARTURE_HISTORY, DepartureHistory.toNbt());
    
        try {
            NbtIo.writeCompressed(nbt, new File(ModCommonEvents.getCurrentServer().get().getWorldPath(new LevelResource("data/" + FILENAME)).toString()));
            CreateRailwaysNavigator.LOGGER.debug("Saved train listener data.");
        } catch (IOException e) {
            CreateRailwaysNavigator.LOGGER.error("Unable to save train listener data.", e);
        }    
    }
    
    private static void load() throws IOException {   
        File settingsFile = new File(ModCommonEvents.getCurrentServer().get().getWorldPath(new LevelResource("data/" + FILENAME)).toString());  
        if (!settingsFile.exists()) {
            return;
        }  
        CompoundTag nbt = NbtIo.readCompressed(settingsFile);

        CompoundTag dataNbt = nbt.getCompound(NBT_TRAIN_DATA);
        for (String key : dataNbt.getAllKeys()) {
            try {
                UUID id = UUID.fromString(key);
                TrainData.fromNbt(dataNbt.getCompound(key)).ifPresent(x -> data.put(id, x));                
            } catch (Exception e) {
                CreateRailwaysNavigator.LOGGER.warn("Unable to read train listener train data with ID '" + key + "'. " + e.getMessage(), e);
            }
        }

        DepartureHistory.fromNbt(nbt.getCompound(NBT_DEPARTURE_HISTORY));
    }

    private static void queueTrainListenerTask(Runnable task) {
        trainDataHookTasks.add(task);
    }
    
    public synchronized static void refreshPre() throws Exception {
        if (!trainDataListenerActive) return;
        statusByDestination.clear();
        Set<Train> trains = TrainUtils.getTrains(true);
        Iterator<Train> iterator = trains.iterator();
        while (iterator.hasNext()) {
            final Train train = iterator.next();
            try {
                if (GlobalSettings.getInstance().isTrainBlacklisted(train)) {
                    iterator.remove();
                    data.remove(train.id);
                    continue;
                }
                TrainData trainData = data.computeIfAbsent(train.id, x -> TrainData.of(train));
                trainData.refreshPre();
                for (TrainPrediction p : trainData.getPredictions()) {
                    statusByDestination.computeIfAbsent(p.getTargetedStationName(), $ -> new HashSet<>()).add(p);
                }
            } catch (Exception e) {
                throw new Exception("Unable to process train: " + train.name + " (" + train.id + ")", e);
            }
        }
    }

    public synchronized static void refreshPost() {
        if (!trainDataListenerActive) return;
        for (TrainData train : data.values()) {
            train.refreshPost();
        }
    }

    public synchronized static void tick() {
        if (!trainDataListenerActive) return;
        for (TrainData train : data.values()) {
            train.tick();
        }
    }
}
