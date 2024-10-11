package de.mrjulsen.crn.data.train;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.Set;

import com.simibubi.create.content.trains.entity.Train;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.event.CRNEventsManager;
import de.mrjulsen.crn.event.ModCommonEvents;
import de.mrjulsen.crn.event.events.CreateTrainPredictionEvent;
import de.mrjulsen.crn.event.events.GlobalTrainDisplayDataRefreshEventPost;
import de.mrjulsen.crn.event.events.GlobalTrainDisplayDataRefreshEventPre;
import de.mrjulsen.crn.event.events.ScheduleResetEvent;
import de.mrjulsen.crn.event.events.SubmitTrainPredictionsEvent;
import de.mrjulsen.crn.event.events.TotalDurationTimeChangedEvent;
import de.mrjulsen.crn.event.events.TrainArrivalAndDepartureEvent;
import de.mrjulsen.crn.event.events.TrainDestinationChangedEvent;
import de.mrjulsen.crn.mixin.ScheduleRuntimeAccessor;
import de.mrjulsen.mcdragonlib.DragonLib;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.storage.LevelResource;

/** Monitors all trains in the world and processes their data and information to make it available for use. */
public final class TrainListener {
    
    private transient static final String FILENAME = CreateRailwaysNavigator.MOD_ID + "_train_data.nbt";

    public static final ConcurrentHashMap<UUID /* train id */, TrainData> data = new ConcurrentHashMap<>();

    private transient static boolean trainDataListenerActive = false;
    private transient static long currentTrainDataListenerId = 0L;
    private transient static final Queue<Runnable> trainDataHookTasks = new ConcurrentLinkedQueue<>();


    public static void init() {
        // Register Event Listeners
        CRNEventsManager.getEvent(GlobalTrainDisplayDataRefreshEventPre.class).register(CreateRailwaysNavigator.MOD_ID, () -> {
            queueTrainListenerTask(TrainListener::refreshPre);
        });

        CRNEventsManager.getEvent(GlobalTrainDisplayDataRefreshEventPost.class).register(CreateRailwaysNavigator.MOD_ID, () -> {
            queueTrainListenerTask(TrainListener::refreshPost);
        });        
        
        CRNEventsManager.getEvent(TrainDestinationChangedEvent.class).register(CreateRailwaysNavigator.MOD_ID, (train, current, next, nextIndex) -> {
        });
        
        CRNEventsManager.getEvent(TotalDurationTimeChangedEvent.class).register(CreateRailwaysNavigator.MOD_ID, (train, old, newDuration) -> {
            CreateRailwaysNavigator.LOGGER.warn("The total duration of the train " + train.name.getString() + " (" + train.id + ") has changed from " + old + " Ticks to " + newDuration + " Ticks. This will result in changes to the scheduled departure times!");
        });

        CRNEventsManager.getEvent(TrainArrivalAndDepartureEvent.class).register(CreateRailwaysNavigator.MOD_ID, (train, station, isArrival) -> {
            queueTrainListenerTask(() -> {
                if (data.containsKey(train.id)) {
                    if (isArrival) {
                        data.get(train.id).reachDestination(DragonLib.getCurrentWorldTime(), ((ScheduleRuntimeAccessor)train.runtime).crn$getTicksInTransit());
                    } else {
                        data.get(train.id).leaveDestination();
                    }
                }
            });
        });
        
        CRNEventsManager.getEvent(ScheduleResetEvent.class).register(CreateRailwaysNavigator.MOD_ID, (train, soft) -> {
            queueTrainListenerTask(() -> {
                if (data.containsKey(train.id)) {
                    TrainData trainData = data.get(train.id);
                    if (soft) {
                        trainData.resetPredictions();
                    } else {
                        trainData.hardResetPredictions();
                    }
                }
            });
        });
        
        CRNEventsManager.getEvent(SubmitTrainPredictionsEvent.class).register(CreateRailwaysNavigator.MOD_ID, (train, predictions, entryCount, accumulatedTime, current) -> {
            
        });
        
        CRNEventsManager.getEvent(CreateTrainPredictionEvent.class).register(CreateRailwaysNavigator.MOD_ID, (train, schedule, predictables, index, stayDuration, minStayDuration, prediction) -> {
            queueTrainListenerTask(() -> {                
                ScheduleRuntimeAccessor accessor = (ScheduleRuntimeAccessor)(Object)schedule;
                UUID trainId = accessor.crn$getTrain().id;
                if (data.containsKey(trainId) && prediction != null) {
                    TrainData trainData = data.get(trainId);
                    TrainPrediction pred = trainData.setPredictionData(index, schedule.currentEntry, schedule.getSchedule().entries.size(), stayDuration, minStayDuration, accessor.crn$predictionTicks().get(index), prediction);
                    predictables.values().forEach(x -> x.predictForStation(trainData, pred, schedule, index, accessor.crn$getTrain()));
                }
            });
        });
    }

    public static Set<Train> getAllTrains() {
        return data.values().stream().map(x -> x.getTrain()).collect(Collectors.toSet());
    }

    public static boolean allTrainsInitialized() {
        return data.values().stream().filter(x -> 
            !GlobalSettings.getInstance().isTrainBlacklisted(x.getTrain()) &&
            !x.getPredictionsRaw().isEmpty() &&
            !x.getTrain().runtime.paused &&
            !x.getTrain().derailed &&
            !x.getTrain().runtime.completed &&
            TrainUtils.isTrainValid(x.getTrain())
        ).allMatch(x -> x.isInitialized() && !x.isPreparing());
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

        CompoundTag nbt = new CompoundTag();
        data.entrySet().forEach(x -> nbt.put(x.getKey().toString(), x.getValue().toNbt()));
    
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
        for (String key : nbt.getAllKeys()) {
            try {
                UUID id = UUID.fromString(key);
                data.put(id, TrainData.fromNbt(nbt.getCompound(key)));
            } catch (Exception e) {
                CreateRailwaysNavigator.LOGGER.warn("Unable to read train listener train data with ID '" + key + "'.", e);
            }
        }
    }

    private static void queueTrainListenerTask(Runnable task) {
        trainDataHookTasks.add(task);
    }
    
    public synchronized static void refreshPre() {
        if (!trainDataListenerActive) return;
        Set<Train> trains = TrainUtils.getTrains(true);
        data.keySet().retainAll(trains.stream().filter(x -> !GlobalSettings.getInstance().isTrainBlacklisted(x)).map(x -> x.id).toList());
        trains.forEach(x -> {
            data.computeIfAbsent(x.id, a -> TrainData.of(x)).refreshPre();
        });
    }

    public synchronized static void refreshPost() {
        if (!trainDataListenerActive) return;
        data.values().forEach(x -> x.refreshPost());
    }

    public synchronized static void tick() {
        if (!trainDataListenerActive) return;
        data.values().forEach(x -> x.tick());
    }
}
