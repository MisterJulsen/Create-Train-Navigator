package de.mrjulsen.crn.data.train;

import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.Map.Entry;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.schedule.ScheduleEntry;
import com.simibubi.create.content.trains.schedule.destination.ChangeTitleInstruction;
import com.simibubi.create.content.trains.schedule.destination.DestinationInstruction;
import com.simibubi.create.content.trains.station.GlobalStation;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.data.schedule.instruction.IPredictableInstruction;
import de.mrjulsen.crn.event.CRNEventsManager;
import de.mrjulsen.crn.event.events.TotalDurationTimeChangedEvent;
import de.mrjulsen.crn.mixin.ScheduleRuntimeAccessor;
import de.mrjulsen.crn.data.TrainInfo;
import de.mrjulsen.crn.data.schedule.condition.DynamicDelayCondition;
import de.mrjulsen.crn.data.train.TrainStatus.TrainStatusType;
import de.mrjulsen.crn.util.IListenable;
import de.mrjulsen.crn.util.LockedList;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.config.ECachingPriority;
import de.mrjulsen.mcdragonlib.util.Cache;
import de.mrjulsen.mcdragonlib.util.math.MathUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** Contains general data about a specific train (but not about the individual stations) */
public class TrainData implements IListenable<TrainData> {

    private transient static final int VERSION = 1;

    public transient static final String EVENT_TOTAL_DURATION_CHANGED = "total_duration_changed";
    public transient static final String EVENT_SECTION_CHANGED = "section_changed";
    public transient static final String EVENT_DESTINATION_CHANGED = "destination_changed";
    public transient static final String EVENT_STATION_REACHED = "station_reached";

    private transient static final String NBT_VERSION = "Version";
    private transient static final String NBT_ID = "SessionId";
    private transient static final String NBT_TRAIN_ID = "TrainId";
    private transient static final String NBT_PREDICTIONS = "Predictions";
    private transient static final String NBT_CURRENT_SCHEDULE_INDEX = "CurrentScheduleIndex";
    private transient static final String NBT_LINE_ID = "LineId";
    private transient static final String NBT_LAST_DELAY_OFFSET = "LastDelay";
    private transient static final String NBT_CANCELLED = "Cancelled";

    private transient static final int INVALID = -1;

    
    private transient final Map<String, IdentityHashMap<Object, Consumer<TrainData>>> listeners = new HashMap<>(); // Events

    private transient final Train train;
    private UUID sessionId;
    

    private final Map<Integer, TrainPrediction> predictionsByIndex = new ConcurrentHashMap<>();
    private transient final List<TrainPrediction> predictionsChronologically = new LockedList<>();
    private transient final Map<Integer, ScheduleSection> sectionsByIndex = new ConcurrentHashMap<>();
    
    private transient int currentTravelSectionIndex = INVALID;
    private transient int lastScheduleIndex = INVALID;
    private String lineId;

    private transient int totalDuration = INVALID;
    
    public transient int transitTime = 0;
    public transient int waitingAtStationTime = 0;
    private transient boolean wasAtStation = false;
    private transient int wasAtStationIndex = -1;
    private transient boolean wasWaitingForSignal = false;
    public transient UUID waitingForSignalId;
    public transient final Set<Train> occupyingTrains = new HashSet<>();
    public transient int waitingForSignalTicks;
    public transient boolean isManualControlled;

    // Delays
    private long lastSectionDelayOffset;
    private boolean cancelled = false;
    private transient final Map<UUID, Integer> delaysBySignal = new HashMap<>();
    private final Set<ResourceLocation> currentStatusInfos = new HashSet<>(); // Reasons for delays, etc.

    private int refreshTimingsCounter = 0;

    // Tasks
    private transient boolean hardResetPredictions = false;
    private transient boolean initializationCompleted = false;
    private transient boolean preInitialization = true;

    // Flags
    private boolean sectionChanged;
    private boolean destinationChanged;
    private boolean scheduleIndexChanged;

    // Caches   
    private transient final Cache<ScheduleSection> defaultSection = new Cache<>(() -> ScheduleSection.def(this), ECachingPriority.LOW);

    private transient final Cache<Boolean> isDynamic = new Cache<>(() -> 
        getTrain() != null && getTrain().runtime != null && getTrain().runtime.getSchedule() != null &&
        getTrain().runtime.getSchedule().entries.stream().anyMatch(x -> x.conditions.stream().flatMap(y -> y.stream()).anyMatch(y -> y instanceof DynamicDelayCondition c && c.minWaitTicks() < c.totalWaitTicks()))
    );
     
    private transient final Cache<Boolean> isDelayedCache = new Cache<>(() -> {
        for (TrainPrediction pred : predictionsByIndex.values()) {
            if (pred.isAnyDelayed()) {
                return true;
            }
        }
        return false;
    });    
    private transient final Cache<Long> highestDeviationCache = new Cache<>(() -> {
        long max = 0;
        for (TrainPrediction pred : predictionsByIndex.values()) {
            max = Math.max(Math.max(pred.getArrivalTimeDeviation(), pred.getDepartureTimeDeviation()), max);
        }
        return max;
    });    
    private transient final Cache<ScheduleSection> currentSectionCache = new Cache<>(() -> {
        return currentTravelSectionIndex < 0 || !hasCustomScheduleSections() || !sectionsByIndex.containsKey(currentTravelSectionIndex) ?
            defaultSection.get() :
            sectionsByIndex.get(currentTravelSectionIndex)
        ;
    });
    private final Cache<List<ScheduleSection>> sectionsCache = new Cache<>(() -> {
        return sectionsByIndex.isEmpty() ?
            List.of(defaultSection.get()) :
            sectionsByIndex.values().stream().sorted((a, b) -> Integer.compare(a.getScheduleIndex(), b.getScheduleIndex())).toList()
        ;
    });
    private final Cache<Boolean> isInitializedCache = new Cache<>(() -> {
        for (TrainPrediction pred : getPredictions()) {
            if (!pred.isInitialized()) {
                return false;
            }
        }
        return true;
    });
    
    /* 
     * Chronological update order (once every ~5 seconds):
     *  refreshPre()           (once)
     *  setPredictionData()    (x times)
     *  refreshPost()          (once)
     * 
     *  tick()                 (every tick)
     */

    private TrainData(Train train, UUID sessionId) {
        this.train = train;
        this.sessionId = sessionId;
        this.totalDuration = INVALID;

        this.transitTime = ((ScheduleRuntimeAccessor)train.runtime).crn$getTicksInPreviousTransit();

        createEvent(EVENT_TOTAL_DURATION_CHANGED);
        createEvent(EVENT_DESTINATION_CHANGED);
        createEvent(EVENT_SECTION_CHANGED);
        createEvent(EVENT_STATION_REACHED);
    }

    public static Optional<TrainData> of(UUID trainId) {
        Optional<Train> train = TrainUtils.getTrain(trainId);
        if (train.isPresent()) {
            return Optional.of(new TrainData(train.get(), UUID.randomUUID()));
        }
        return Optional.empty();
    }

    public static TrainData of(Train train) {
        return new TrainData(train, UUID.randomUUID());
    }
    
    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getTrainId() {
        return getTrain().id;
    }

    public Train getTrain() {
        return train;
    }

    public TrainInfo getTrainInfo(int scheduleIndex) {
        ScheduleSection currentSection = getSectionForIndex(scheduleIndex);
        return new TrainInfo(currentSection.getTrainLine().orElse(null), currentSection.getTrainCategory().orElse(null));
    }

    public TrainInfo getTrainInfoWithArrivalContext(int scheduleIndex, boolean beforeArrival) {
        ScheduleSection currentSection = getSectionForIndex(scheduleIndex);
        ScheduleSection prevSection = currentSection.previousSection();
        ScheduleSection selectedSection = currentSection;
        boolean isFirstStationInSection = currentSection.getFirstStop().map(x -> x.getEntryIndex() == getCurrentScheduleIndex()).orElse(false);
        if (isFirstStationInSection && ((beforeArrival && prevSection.shouldIncludeNextStationOfNextSection()) || !currentSection.isUsable())) {
            selectedSection = prevSection;
        }
        return new TrainInfo(selectedSection.getTrainLine().orElse(null), selectedSection.getTrainCategory().orElse(null));
    }

    /**
     * Checks if this train uses dynamic wait times to catch up for delays.
     * If there are no dynamic waiting times, a train cannot catch up for delays, resulting in permanent delays.
     * Even if everything goes according to plan, a train tends to be delayed due to calculation inaccuracies.
     */
    public boolean isDynamic() {
        return isDynamic.get();
    }

    /** {@code true} when the train is currently waiting at a station. */
    public boolean isAtStation() {
        return train.navigation.destination == null;
    }

    /** The time in ticks the train is waiting at the current station. */
    public long waitingAtStationTicks() {
        return waitingAtStationTime;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public int getTotalDuration() {
        return totalDuration;
    }

    @Deprecated(forRemoval = true)
    public int getTransitTicks() {
        return transitTime;
    }

    @Deprecated(forRemoval = true)
    public int getTransitTimeOf(int scheduleIndex) {
        return predictionsByIndex.containsKey(scheduleIndex) ? predictionsByIndex.get(scheduleIndex).transitTime().value() : INVALID;
    }

    /**
     * The train prediction with all information about a specific stop at the index in the train schedule.
     * @param scheduleIndex The index of the desired entry in the train schedule.
     * @return
     */
    public Optional<TrainPrediction> getPredictionByIndex(int scheduleIndex) {
        return Optional.ofNullable(this.predictionsByIndex.get(scheduleIndex));
    }

    /**
     * The schedule section at the index in the train schedule.
     * @param scheduleIndex The index of the desired entry in the train schedule.
     * @return The {@code TrainTravelSection} or the default section if nothing is defined for this index.
     */
    public ScheduleSection getSectionByIndex(int scheduleIndex) {
        return sectionsByIndex.isEmpty() ? defaultSection.get() : sectionsByIndex.get(scheduleIndex);
    }

    public void addScheduleSection(ScheduleSection section) {
        this.sectionsByIndex.put(section.getScheduleIndex(), section);
        sectionsCache.clear();
        currentSectionCache.clear();
    }

    public String getCurrentTitle() {
        return getPredictionByIndex(getCurrentScheduleIndex()).map(TrainPrediction::getTitle).orElse("");
    }

    public String getTrainName() {
        return train.name.getString();
    }

    public String resolveTrainDisplayName() {
        return resolveTrainDisplayName(getCurrentSection());
    }
    
    public String resolveTrainDisplayName(ScheduleSection section) {
        if (section == null) {
            return getTrainName();
        }

        String lineName;
        if (section.getTrainLine().map(x -> x.getLineName().isEmpty()).orElse(true)) {
            lineName = getTrainName();
        } else {
            lineName = section.getTrainLine().get().getLineName();
        }
        return lineName;

        //return getCurrentSection() == null || getCurrentSection().getTrainLine().map(x -> x.getLineName().isEmpty()).orElse(true) ? getTrainName() : getCurrentSection().getTrainLine().get().getLineName();
    }

    public int getCurrentScheduleIndex() {
        return getTrain().runtime.currentEntry;
    }

    public boolean hasCustomScheduleSections() {
        return !sectionsByIndex.isEmpty();
    }

    public boolean isSingleSection() {
        return sectionsByIndex.size() <= 1;
    }

    public List<ScheduleSection> getSections() {
        return sectionsCache.get();
    }

    public ScheduleSection getSectionForIndex(int anyIndex) {
        if (isSingleSection()) {
            return getSections().get(0);
        }
        ScheduleSection selectedSection = getSections().get(getSections().size() - 1);
        for (ScheduleSection section : getSections()) {
            if (section.getScheduleIndex() > anyIndex) {
                break;
            }
            selectedSection = section;
        }
        return selectedSection;
    }
    
    public synchronized List<TrainPrediction> getPredictions() {
        return ImmutableList.copyOf(predictionsByIndex.values());
    }
    
    public synchronized boolean hasPredictions() {
        return !predictionsByIndex.isEmpty();
    }
    
    public synchronized Map<Integer, TrainPrediction> getPredictionsMap() {
        return ImmutableMap.copyOf(predictionsByIndex);
    }

    public synchronized List<TrainPrediction> getPredictionsChronologically() {
        return ImmutableList.copyOf(predictionsChronologically);
    }

    public synchronized Optional<TrainPrediction> getNextStopPrediction() {
        return predictionsChronologically.isEmpty() ? Optional.empty() : Optional.ofNullable(predictionsChronologically.get(0));
    }

    public synchronized boolean isDelayed() {
        return isDelayedCache.get();
    }

    public boolean isCurrentSectionDelayed() {
        return isDelayed() && getHighestDeviation() - lastSectionDelayOffset > ModCommonConfig.SCHEDULE_DEVIATION_THRESHOLD.get();
    }

    public long getHighestDeviation() {
        return highestDeviationCache.get();
    }

    public long getDeviationDelayOffset() {
        return lastSectionDelayOffset;
    }

    public ScheduleSection getCurrentSection() {
        return currentSectionCache.get();
    }

    public Map<UUID, Integer> getWaitingForSignalsTime() {
        return ImmutableMap.copyOf(delaysBySignal);
    }

    public Set<ResourceLocation> getStatus() {
        return currentStatusInfos;
    }

    public int debug_statusInfoCount() {
        return currentStatusInfos.size();
    }
    



    public void softResetPredictions() {
        for (TrainPrediction pred : predictionsByIndex.values()) {
            pred.queueReset();
        }
        lastSectionDelayOffset = 0;
        refreshTimingsCounter = 0;
        wasAtStationIndex = -1;
        resetStatus(true);
        isDynamic.clear();
        if (CreateRailwaysNavigator.isDebug() || ModCommonConfig.ADVANCED_LOGGING.get()) CreateRailwaysNavigator.LOGGER.info(getTrainName() + " has reset their scheduled times.");
    }

    public void hardResetPredictions() {
        preInitialization = true;
        hardResetPredictions = true;
    }

    private void resetStatus(boolean keepPreviousDelays) {
        currentStatusInfos.clear();
        if (keepPreviousDelays && isDelayed()) {
            currentStatusInfos.add(TrainStatus.DELAY_FROM_PREVIOUS_JOURNEY.getLocation());
        }
    }

    public void applyStatus() {
        if (isCancelled()) {
            currentStatusInfos.clear();
            currentStatusInfos.add(TrainStatus.CANCELLED.getLocation());
            return;
        }

        for (Entry<ResourceLocation,TrainStatus> x : TrainStatus.Registry.getRegisteredStatus().entrySet()) {
            if (x.getValue().isTriggerd(this)) {
                currentStatusInfos.add(x.getKey());
            }
        }

        boolean unknownDelayReason = isCurrentSectionDelayed();
        if (unknownDelayReason) {
            for (ResourceLocation loc : currentStatusInfos) {
                if (TrainStatus.Registry.getRegisteredStatus().get(loc).getImportance() == TrainStatusType.DELAY && 
                    !loc.equals(TrainStatus.DEFAULT_DELAY.getLocation())
                ) {
                    unknownDelayReason = false;
                    break;
                }
            }
        }

        if (unknownDelayReason) {
            currentStatusInfos.add(TrainStatus.DEFAULT_DELAY.getLocation());
        } else {
            currentStatusInfos.remove(TrainStatus.DEFAULT_DELAY.getLocation());
        }
    }

    public boolean hasSectionChanged() {
        return sectionChanged;
    }

    /**
     * Indicates whether there is enough data about this train and whether it has already been initialized.
     * Trains that have not yet been initialized do not yet contain any reliable data to make any predictions.
     */
    public boolean isInitialized() {
        return isInitializedCache.get();
    }    

    public boolean isPreInitializationPhase() {
        return preInitialization;
    }

    public int debug_initializedStationsCount() {
        return (int)getPredictions().stream().mapToInt(x -> x.transitTime().value()).filter(x -> x > 0).count();
    }

    public synchronized void shiftTime(long l) {
        if (!isPreInitializationPhase()) {
            predictionsByIndex.values().forEach(x -> x.shiftTime(l));
        }
    }

    public void changeCurrentSection(int sectionEntryIndex) {
        this.currentTravelSectionIndex = sectionsByIndex.containsKey(sectionEntryIndex) ? sectionEntryIndex : INVALID;
        sectionChanged = true;
        lastSectionDelayOffset = Math.max(0, getHighestDeviation());
        this.refreshTimingsCounter++;
        currentSectionCache.clear();
    }

    private int getTransitTimeAtStation(int index) {
        if (predictionsByIndex.containsKey(index)) {
            return predictionsByIndex.get(index).transitTime().value();
        }
        List<Integer> transitTimesFromCreate = ((ScheduleRuntimeAccessor)(Object)train.runtime).crn$getTransitTicks();
        int transitTime = transitTimesFromCreate.size() > index ? transitTimesFromCreate.get(index) : INVALID;
        return transitTime;
	}

    /**
     * Calculates how long the train will probably need to reach the next stop.
     * @return The time in ticks
     */
    public int predictTimeToNextStop() {
        GlobalStation destination = train.navigation.destination;
        int accumulatedTime = 0;
        if (destination != null) {
            List<Integer> transitTimesFromCreate = ((ScheduleRuntimeAccessor)(Object)train.runtime).crn$getTransitTicks();
            double speed = Math.min(train.throttle * train.maxSpeed(), (train.maxSpeed() + train.maxTurnSpeed()) / 2);
            int timeRemaining = (int)(train.navigation.distanceToDestination / speed) * 2;

            if (transitTimesFromCreate.size() > train.runtime.currentEntry && train.navigation.distanceStartedAt != 0) {
                float predictedTime = transitTimesFromCreate.get(train.runtime.currentEntry);
                if (predictedTime > 0) {
                    predictedTime *= MathUtils.clamp(train.navigation.distanceToDestination / train.navigation.distanceStartedAt, 0, 1);
                    timeRemaining = (timeRemaining + (int)predictedTime) / 2;
                }
            }

            accumulatedTime += timeRemaining;
        }
        return accumulatedTime;
    }

    private void clearAll() {
        preInitialization = true;
        predictionsByIndex.clear();
        sectionsByIndex.clear();
        defaultSection.clear();
        predictionsChronologically.clear();
        currentStatusInfos.clear();
        currentSectionCache.clear();
        sectionsCache.clear();
        lastScheduleIndex = INVALID;
        totalDuration = INVALID;
        wasAtStationIndex = INVALID;

        resetCaches();
    } 

    public int ticksToNextStop = 0;
    
    public int waitingAtStationIndex = INVALID;

    /** Called every ~5 seconds */
    public synchronized void refreshPre() {
        if (hardResetPredictions) {
            hardResetPredictions = false;
            clearAll();
        }

        if (train.runtime.paused) {
            return;
        }
        
        // Check index
        this.scheduleIndexChanged = lastScheduleIndex != getCurrentScheduleIndex();

        if (this.scheduleIndexChanged && lastScheduleIndex >= 0 && predictionsByIndex.containsKey(lastScheduleIndex)) {
            predictionsByIndex.get(lastScheduleIndex).nextCycle();            
        }
        if (!hasCustomScheduleSections() && lastScheduleIndex > getCurrentScheduleIndex()) { // Manually call section change event atthe end of the schedule if there are no sections defined.
            changeCurrentSection(currentTravelSectionIndex);
        }
        lastScheduleIndex = getCurrentScheduleIndex();

        // Calc predictions
        calcPredictions();
    }

    private void calcPredictions() {
        predictionsChronologically.clear();
        Schedule schedule = train.runtime.getSchedule();
        int entryCount = train.runtime.getSchedule().entries.size();
        AtomicReference<String> currentTitle = new AtomicReference<>("");

        // ##### PRE-ITERATION #####
        for (int i = 0; i < entryCount; i++) {
            final int cyclicIndex = (i + getCurrentScheduleIndex()) % entryCount;
            final ScheduleEntry entry = schedule.entries.get(cyclicIndex);
            if (entry.instruction instanceof ChangeTitleInstruction instruction) {
                currentTitle.set(instruction.getScheduleTitle());
            }
        }
        
        Set<Integer> validPredictionEntries = new HashSet<>();
        boolean hasCycled = false;

        final long now = DragonLib.getCurrentWorldTime() - waitingAtStationTicks();
        long time = now;

        for (int i = 0; i < entryCount; i++) {
            final int cyclicIndex = (i + getCurrentScheduleIndex()) % entryCount;
            final ScheduleEntry entry = schedule.entries.get(cyclicIndex);

            if (entry.instruction instanceof IPredictableInstruction instruction) {
                instruction.predict(this, train.runtime, cyclicIndex, train);
                continue;
            } else if (entry.instruction instanceof ChangeTitleInstruction instruction) {
                currentTitle.set(instruction.getScheduleTitle());
                continue;
            } else if (!(entry.instruction instanceof DestinationInstruction)) {
                continue;
            }

            validPredictionEntries.add(cyclicIndex);
            final DestinationInstruction destination = (DestinationInstruction)entry.instruction;
            AtomicReference<String> name = new AtomicReference<>(destination.getFilter());
            if (i <= 0) {
                time += this.ticksToNextStop = predictTimeToNextStop();
                GlobalStation destStation = train.navigation.destination != null ? train.navigation.destination : train.getCurrentStation();
                name.set(destStation != null ? destStation.name : name.get());
            } else {
                if (hasCycled || (cyclicIndex == 0 && !train.runtime.getSchedule().cyclic)) {
                    hasCycled = true;
                    continue;
                }
                time += getTransitTimeAtStation(cyclicIndex);
            }

            TrainPrediction pred = predictionsByIndex.computeIfAbsent(cyclicIndex, idx -> new TrainPrediction(this, idx, destination.getFilter(), name.get(), currentTitle.get()));
            if (!isPreInitializationPhase()) {
                pred.preInit();
            }
            predictionsChronologically.add(pred);
            pred.updateRealTime(destination.getFilter(), name.get(), now, time, currentTitle.get());
            time = pred.realTime().departureTime();
        }

        predictionsByIndex.keySet().retainAll(validPredictionEntries); // Remove all predictions that are no longer in the schedule (for whatever reason)
    }


    public static record SimulationResult(int entryIndex, int cycles, long arrivalTime, long departureTime) {}
    public SimulationResult simulate(int entryIndex, long duration) {
        Schedule schedule = train.runtime.getSchedule();
        int entryCount = train.runtime.getSchedule().entries.size();
        
        final long now = predictionsByIndex.get(entryIndex).scheduled().departureTime();
        long time = now;
        long lastTime = time;
        SimulationResult result = new SimulationResult(entryIndex, 0, now, now);

        int iteration = 0;
        while (duration - (now - DragonLib.getCurrentWorldTime()) > 0) {
            long arrival = 0;
            long departure = 0;
            for (int i = 0; i < entryCount; i++) {
                final int cyclicIndex = (i + (entryIndex + 1)) % entryCount;
                final ScheduleEntry entry = schedule.entries.get(cyclicIndex);
                
                if (!(entry.instruction instanceof DestinationInstruction)) {
                    continue;
                }
                
                if (cyclicIndex == 0 && !train.runtime.getSchedule().cyclic) {
                    return result;
                }
                time += getTransitTimeAtStation(cyclicIndex);
    
                final long newArrivalTime = time;
                time = TrainPrediction.estimateDepartures(getTrain(), cyclicIndex, time).defaultDepartureTime();

                if (cyclicIndex == entryIndex) {
                    arrival = newArrivalTime;
                    departure = time;
                }
            }
            iteration++;
            result = new SimulationResult(entryIndex, iteration, arrival, departure);
            duration -= (time - lastTime);
            lastTime = time;
        }
        return result;
    }



    /** Called every ~5 seconds */
    public synchronized void refreshPost() {
        // [] train cancelled manager
        boolean isNowCancelled = !(TrainUtils.isTrainValid(train) && isInitialized()) || train.runtime.paused;
        if (this.cancelled && !isNowCancelled) { // Train should no longer be cancelled -> restart
            initializationCompleted = false;
            sessionId = UUID.randomUUID();
            softResetPredictions();
        }
        this.cancelled = isNowCancelled;

        applyStatus();

        if (destinationChanged) {
            destinationChanged = false;
            notifyListeners(EVENT_DESTINATION_CHANGED, this);
        }

        resetCaches();

        // Finish
        this.scheduleIndexChanged = false;
        this.waitingAtStationIndex = isAtStation() ? getCurrentScheduleIndex() : INVALID;
    }

    private void resetCaches() {
        isDelayedCache.clear();
        highestDeviationCache.clear();
        isInitializedCache.clear();
    }

    private final Queue<Runnable> deferredTickQueue = new ConcurrentLinkedQueue<>();

    /** Called every tick */
    public void tick() {       
        if (train.runtime.paused) {
            return;
        }

        while (!deferredTickQueue.isEmpty()) {
            deferredTickQueue.poll().run();
        }

        boolean isAtStation = isAtStation();
        int isAtStationIndex = train.runtime.currentEntry;
        boolean stationChanged = wasAtStation != isAtStation;
        boolean stationIndexChanged = wasAtStationIndex != isAtStationIndex;
        if (stationChanged) {
            if (isAtStation) {
                onReachDestination();
            } else {
                onLeaveDestination();
            }
            this.wasAtStation = isAtStation;
        } else if (wasAtStationIndex > INVALID && isAtStation && stationIndexChanged && predictionsByIndex.containsKey(getCurrentScheduleIndex())) {
            deferredTickQueue.add(() -> {
                if (!isAtStation() || !predictionsByIndex.containsKey(getCurrentScheduleIndex())) return;
                this.transitTime = 0;
                this.waitingAtStationTime = 0;
                this.waitingForSignalTicks = 0;
                this.waitingForSignalId = null;
                this.delaysBySignal.clear();
                onReachDestination();
            });
        }
        this.wasAtStationIndex = isAtStationIndex;

        if (isAtStation) {
            waitingAtStationTime++;
        } else {
            transitTime++;
        }



        // Waiting for signal
        boolean isWaitingForSignal = train.navigation.waitingForSignal != null;
        if (wasWaitingForSignal != isWaitingForSignal) { // The moment in which the state has been changed
            if (isWaitingForSignal) { // currently waiting
                waitingForSignalId = train.navigation.waitingForSignal.getFirst();
                occupyingTrains.clear();
                occupyingTrains.addAll(TrainUtils.isSignalOccupied(waitingForSignalId, Set.of(train.id)));
            } else { // no longer waiting
                delaysBySignal.put(waitingForSignalId, waitingForSignalTicks);
                waitingForSignalTicks = 0;
                occupyingTrains.clear();
            }
            this.wasWaitingForSignal = isWaitingForSignal;
        }

        if (isWaitingForSignal) {
            waitingForSignalTicks++;
        }
    }

    public void updateTotalDuration() {
        int newDuration = getPredictions().stream().mapToInt(x -> x.transitTime().value() + (int)x.scheduled().stayDuration()).sum();
        int oldTotalDuration = this.totalDuration;
        if (CRNEventsManager.isRegistered(TotalDurationTimeChangedEvent.class) && this.totalDuration > 0 && this.totalDuration != newDuration) {
            CRNEventsManager.getEvent(TotalDurationTimeChangedEvent.class).run(train, this.totalDuration, newDuration);
        }
        this.totalDuration = newDuration;
        if (oldTotalDuration != INVALID) {
            notifyListeners(EVENT_TOTAL_DURATION_CHANGED, this);
        }
        softResetPredictions();
    }

    /**
     * Called when the train reaches a station.
     */
    public void onReachDestination() {
        if (!isPreInitializationPhase()) {
            this.getPredictionByIndex(getCurrentScheduleIndex()).ifPresent(x -> {
                x.transitTime().add(transitTime, false);
                x.onReachStation();
            });
        }

        this.transitTime = 0;
        this.waitingAtStationTime = 0;
        this.waitingForSignalTicks = 0;
        this.waitingForSignalId = null;
        this.delaysBySignal.clear();
        preInitialization = false;

        if (!initializationCompleted && isInitialized()) {
            completeInitialization();
        }
        notifyListeners(EVENT_STATION_REACHED, this);
    }

    /**
     * Called when the train leaves a station.
     */
    public void onLeaveDestination() {
        if (!isPreInitializationPhase()) {
            this.getPredictionByIndex(getCurrentScheduleIndex()).ifPresent(x -> {
                x.updateAverageStayDuration(waitingAtStationTime);
            });
        }

        if (sectionChanged) {            
            sectionChanged = false;
            if (!isDynamic() || (ModCommonConfig.AUTO_RESET_TIMINGS.get() > 0 && refreshTimingsCounter >= ModCommonConfig.AUTO_RESET_TIMINGS.get())) {
                softResetPredictions();
            } else {
                resetStatus(true);
            }
            notifyListeners(EVENT_SECTION_CHANGED, this);
        }


        this.transitTime = 0;
        this.waitingAtStationTime = 0;
        this.waitingForSignalTicks = 0;
        this.waitingForSignalId = null;
        this.delaysBySignal.clear();
    }

    public void completeInitialization() {
        updateTotalDuration();
        isDynamic.clear();
        initializationCompleted = true;
    }







    @Override
    public Map<String, IdentityHashMap<Object, Consumer<TrainData>>> getListeners() {
        return listeners;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt(NBT_VERSION, VERSION);

        CompoundTag predictions = new CompoundTag();
        for (Entry<Integer, TrainPrediction> entry : predictionsByIndex.entrySet()) {
            predictions.put(String.valueOf(entry.getKey()), entry.getValue().toNbt());
        }

        nbt.putUUID(NBT_ID, getSessionId());
        nbt.putUUID(NBT_TRAIN_ID, getTrainId());
        nbt.put(NBT_PREDICTIONS, predictions);
        nbt.putInt(NBT_CURRENT_SCHEDULE_INDEX, getCurrentScheduleIndex());
        nbt.putLong(NBT_LAST_DELAY_OFFSET, lastSectionDelayOffset);
        nbt.putBoolean(NBT_CANCELLED, cancelled);
        nbt.putString(NBT_LINE_ID, lineId == null ? "" : lineId);
        return nbt;
    }
    
    public static Optional<TrainData> fromNbt(CompoundTag nbt) {
        UUID trainId = nbt.getUUID(NBT_TRAIN_ID);
        UUID sessionId = nbt.getUUID(NBT_ID);
        Optional<Train> train = TrainUtils.getTrain(trainId);

        if (train.isPresent()) {
            TrainData data = new TrainData(train.get(), sessionId);
            data.deserializeNbt(nbt);
            return Optional.ofNullable(data);
        }
        CreateRailwaysNavigator.LOGGER.warn("Cannot load data for train with id " + trainId + ", because that train does not exist.");
        return Optional.empty();
    }

    protected void deserializeNbt(CompoundTag nbt) {
        CompoundTag predictions = nbt.getCompound(NBT_PREDICTIONS);
        for (String key : predictions.getAllKeys()) {
            try {
                int idx = Integer.parseInt(key);
                this.predictionsByIndex.put(idx, TrainPrediction.fromNbt(this, predictions.getCompound(key)));
            } catch (Exception e) {
                CreateRailwaysNavigator.LOGGER.warn("Unable to load prediction with index '" + key + "': The value is not an integer.", e);
            }
        }

        int currentScheduleIndex = nbt.getInt(NBT_CURRENT_SCHEDULE_INDEX);
        this.lastScheduleIndex = currentScheduleIndex;
        this.currentTravelSectionIndex = getSectionForIndex(currentScheduleIndex).getScheduleIndex();
        this.lineId = nbt.getString(NBT_LINE_ID);
        this.lastSectionDelayOffset = nbt.getLong(NBT_LAST_DELAY_OFFSET);
        this.cancelled = nbt.getBoolean(NBT_CANCELLED);
    }

}
