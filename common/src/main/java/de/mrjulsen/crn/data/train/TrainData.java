package de.mrjulsen.crn.data.train;

import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.Map.Entry;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;

import com.google.common.collect.ImmutableMap;
import com.simibubi.create.content.trains.display.GlobalTrainDisplayData.TrainDeparturePrediction;
import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.crn.event.CRNEventsManager;
import de.mrjulsen.crn.event.events.TotalDurationTimeChangedEvent;
import de.mrjulsen.crn.data.TrainInfo;
import de.mrjulsen.crn.data.schedule.condition.DynamicDelayCondition;
import de.mrjulsen.crn.data.train.TrainStatus.CompiledTrainStatus;
import de.mrjulsen.crn.data.train.TrainStatus.TrainStatusType;
import de.mrjulsen.crn.util.IListenable;
import de.mrjulsen.crn.util.LockedList;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.data.Cache;
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
    private transient static final String NBT_TRANSIT_TIMES = "TransitTimes";

    private transient static final int INVALID = -1;

    
    private transient final Map<String, IdentityHashMap<Object, Consumer<TrainData>>> listeners = new HashMap<>(); // Events

    private transient final Train train;
    private UUID sessionId;
    private final ConcurrentHashMap<Integer, TrainPrediction> predictionsByIndex = new ConcurrentHashMap<>();
    private transient final ConcurrentHashMap<Integer, TrainTravelSection> sectionsByIndex = new ConcurrentHashMap<>();
    private transient final Cache<TrainTravelSection> defaultSection = new Cache<>(() -> TrainTravelSection.def(this));
    private transient final List<TrainPrediction> predictionsChronologically = new LockedList<>();
    private transient final Set<Integer> validPredictionEntries = new HashSet<>();
    private transient final Cache<Boolean> isDynamic = new Cache<>(() -> getTrain().runtime.getSchedule().entries.stream().anyMatch(x -> x.conditions.stream().flatMap(y -> y.stream()).anyMatch(y -> y instanceof DynamicDelayCondition c && c.minWaitTicks() < c.totalWaitTicks())));

    private int currentScheduleIndex = INVALID;
    private transient int currentTravelSectionIndex = INVALID;
    private transient int lastScheduleIndex = INVALID;
    private String lineId;

    private transient int totalDuration = INVALID;
    private transient long destinationReachTime;
    private transient boolean isAtStation = false;
    
    private transient boolean wasWaitingForSignal = false;
    public transient UUID waitingForSignalId;
    public transient final Set<Train> occupyingTrains = new HashSet<>();
    public transient int waitingForSignalTicks;
    public transient boolean isManualControlled;

    /** Last measured ransit time (mem) */
    public transient int transitTime = 0;
    /** Contains the last (single!) measured transit time that can be used for the calculation. */
    private transient final Map<Integer /* station index */, Integer /* transit time */> measuredTransitTimes = new HashMap<>();    
    /** Contains the x last measured transit times that can be used for the calculation. */
    public final Map<Integer /* schedule index */, PriorityQueue<Integer> /* last x transit times */> transitTimeHistory = new HashMap<>();
    /** The current valid and used transit time. */
    public final Map<Integer /* schedule index */, Integer /* current default transit */> currentTransitTime = new HashMap<>();

    // Delays
    private long lastSectionDelayOffset;
    private boolean cancelled = false;
    private transient final Map<UUID, Integer> delaysBySignal = new HashMap<>();
    private final Set<ResourceLocation> currentStatusInfos = new HashSet<>(); // Reasons for delays, etc.

    private int refreshTimingsCounter = 0;

    // Queued Tasks
    /** whether all predictions should be deleted */
    private transient boolean hardResetPredictions = false;
    /** whether the initialization should now be completed */
    private transient boolean initializationFinishTask = false;
    /** whether the initialization has already been completed */
    private transient boolean initializationCompleted = false;
    private boolean hasStarted = false;

    // temp mem
    private boolean sectionChanged;
    private boolean destinationChanged;
    
    /* PLEASE NOTE!
     * Chronologically update order (once every ~5 seconds):
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
        return new TrainInfo(getSectionForIndex(scheduleIndex).getTrainLine(), getSectionForIndex(scheduleIndex).getTrainGroup());
    }

    /**
     * Checks if this train uses dynamic wait times to catch up for delays.
     * If there are no dynamic waiting times, a train cannot catch up for delays, resulting in permanent delays.
     * Even if everything goes according to plan, a train tends to be delayed due to calculation inaccuracies.
     */
    public boolean isDynamic() {
        return isDynamic.get();
    }

    private int getHistoryBufferSize() {
        return ModCommonConfig.TOTAL_DURATION_BUFFER_SIZE.get() * 2 + 1;
    }

    /** {@code true} when the train is currently waiting at a station. */
    public boolean isAtStation() {
        return train.navigation.destination == null;
    }

    /** The time in ticks the train is waiting at the current station. */
    public long waitingAtStationTicks() {
        return isAtStation() ? DragonLib.getCurrentWorldTime() - destinationReachTime : 0;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public int getTotalDuration() {
        return totalDuration;
    }

    public int getTransitTicks() {
        return transitTime;
    }

    public int getTransitTimeOf(int scheduleIndex) {
        return currentTransitTime.containsKey(scheduleIndex) ? currentTransitTime.get(scheduleIndex) : INVALID;
    }

    public boolean isWaitingAtStation() {
        return isAtStation;
    }

    public TrainTravelSection getCurrentTravelSection() {
        return currentTravelSectionIndex < 0 || !sectionsByIndex.containsKey(currentTravelSectionIndex) ? defaultSection.get() : sectionsByIndex.get(currentTravelSectionIndex);
    }

    public TrainTravelSection getSectionByIndex(int scheduleIndex) {
        return sectionsByIndex.isEmpty() ? defaultSection.get() : sectionsByIndex.get(scheduleIndex);
    }

    public void addTravelSection(TrainTravelSection section) {
        this.sectionsByIndex.put(section.getScheduleIndex(), section);
    }

    public String getCurrentTitle() {
        return this.predictionsByIndex.containsKey(currentScheduleIndex) ? this.predictionsByIndex.get(currentScheduleIndex).getTitle() : "";
    }

    public String getTrainName() {
        return train.name.getString();
    }

    public int getCurrentScheduleIndex() {
        return currentScheduleIndex;
    }

    public boolean hasCustomTravelSections() {
        return !sectionsByIndex.isEmpty();
    }

    public boolean isSingleSection() {
        return sectionsByIndex.size() <= 1;
    }

    public List<TrainTravelSection> getSections() {
        return sectionsByIndex.isEmpty() ? List.of(defaultSection.get()) : sectionsByIndex.values().stream().sorted((a, b) -> Integer.compare(a.getScheduleIndex(), b.getScheduleIndex())).toList();
    }

    public TrainTravelSection getSectionForIndex(int anyIndex) {
        if (isSingleSection()) {
            return getSections().get(0);
        }
        TrainTravelSection selectedSection = getSections().get(getSections().size() - 1);
        for (TrainTravelSection section : getSections()) {
            if (section.getScheduleIndex() > anyIndex) {
                break;
            }
            selectedSection = section;
        }
        return selectedSection;
    }
    
    public synchronized List<TrainPrediction> getPredictions() {
        return new ArrayList<>(predictionsByIndex.values());
    }    
    
    public synchronized Map<Integer, TrainPrediction> getPredictionsRaw() {
        return new HashMap<>(predictionsByIndex);
    }

    public synchronized List<TrainPrediction> getPredictionsChronologically() {
        return new ArrayList<>(predictionsChronologically);
    }

    public synchronized Optional<TrainPrediction> getNextStopPrediction() {
        return predictionsChronologically.isEmpty() ? Optional.empty() : Optional.ofNullable(predictionsChronologically.get(0));
    }

    public void resetPredictions() {
        predictionsByIndex.values().stream().forEach(x -> x.reset());
        lastSectionDelayOffset = 0;
        refreshTimingsCounter = 0;
        resetStatus(true);
        isDynamic.clear();
        if (CreateRailwaysNavigator.isDebug()) CreateRailwaysNavigator.LOGGER.info(getTrainName() + " has reset their scheduled times.");
    }

    public void hardResetPredictions() {
        hardResetPredictions = true;
    }

    public synchronized boolean isDelayed() {
        return predictionsChronologically.stream().anyMatch(TrainPrediction::isAnyDelayed);
    }

    public boolean isCurrentSectionDelayed() {
        return isDelayed() && getHighestDeviation() - lastSectionDelayOffset > ModCommonConfig.SCHEDULE_DEVIATION_THRESHOLD.get();
    }

    public long getHighestDeviation() {
        return predictionsByIndex.values().stream().mapToLong(x -> Math.max(x.getArrivalTimeDeviation(), x.getDepartureTimeDeviation())).max().orElse(0);
    }

    public long getDeviationDelayOffset() {
        return lastSectionDelayOffset;
    }

    public TrainTravelSection getCurrentSection() {
        return currentTravelSectionIndex < 0 || !hasCustomTravelSections() || !sectionsByIndex.containsKey(currentTravelSectionIndex) ? TrainTravelSection.def(this) : sectionsByIndex.get(currentTravelSectionIndex);
    }

    public Map<UUID, Integer> getWaitingForSignalsTime() {
        return ImmutableMap.copyOf(delaysBySignal);
    }

    public Set<CompiledTrainStatus> getStatus() {
        return currentStatusInfos.stream().map(x -> TrainStatus.Registry.getRegisteredStatus().get(x).compile(this)).collect(Collectors.toSet());
    }

    public int debug_statusInfoCount() {
        return currentStatusInfos.size();
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

        
        if (currentStatusInfos.stream().noneMatch(x -> TrainStatus.Registry.getRegisteredStatus().get(x).getImportance() == TrainStatusType.DELAY && !x.equals(TrainStatus.DEFAULT_DELAY.getLocation())) && isCurrentSectionDelayed()) {
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
        return !currentTransitTime.isEmpty() &&
            currentTransitTime.values().stream().noneMatch(x -> x < 0)
        ;
    }

    public int debug_initializedStationsCount() {
        return (int)currentTransitTime.values().stream().filter(x -> x > 0).count();
    }

    /**
     * Indicates whether any preparations need to be made before the initialization phase can begin.
     * This is especially the case after starting the world, when the train was still in the middle of its journey.
     */
    public boolean isPreparing() {
        return !hasStarted;
    }

    public synchronized TrainPrediction setPredictionData(int entryIndex, int currentIndex, int maxEntries, int stayDuration, int minStayDuration, int transitTime, TrainDeparturePrediction predictionData) {
        // keep track of current schedule index
        this.destinationChanged = destinationChanged || this.currentScheduleIndex != currentIndex;
        this.currentScheduleIndex = currentIndex;

        // Update CRN predictions with data from Create
        TrainPrediction pred = predictionsByIndex.computeIfAbsent(entryIndex, i -> new TrainPrediction(this, entryIndex, predictionData, stayDuration, minStayDuration));
        currentTransitTime.computeIfAbsent(entryIndex, x -> -1);
        validPredictionEntries.add(entryIndex);

        pred.updateRealTime(
            predictionData.destination,
            predictionData.ticks
        );
        predictionsChronologically.add(pred);
        return pred;
    }

    public void changeCurrentSection(int sectionEntryIndex) {
        this.currentTravelSectionIndex = sectionsByIndex.containsKey(sectionEntryIndex) ? sectionEntryIndex : INVALID;
        sectionChanged = true;
        lastSectionDelayOffset = Math.max(0, getHighestDeviation());
        this.refreshTimingsCounter++;
    }

    private void clearAll() {      
        predictionsByIndex.clear();
        sectionsByIndex.clear();
        defaultSection.clear();
        predictionsChronologically.clear();
        validPredictionEntries.clear();
        currentStatusInfos.clear();
        measuredTransitTimes.clear();
        transitTimeHistory.clear();
        currentTransitTime.clear();
        lastScheduleIndex = INVALID;
        hasStarted = false;
    }

    /** Called every ~5 seconds */
    public synchronized void refreshPre() {        
        if (train.runtime.paused) {
            return;
        }

        if (hardResetPredictions) {
            hardResetPredictions = false;
            clearAll();
        }

        validPredictionEntries.clear();
        predictionsChronologically.clear();
    }

    /** Called every ~5 seconds */
    public synchronized void refreshPost() {

        // [] Remove invalid prediction data
        if (hasStarted && !train.runtime.paused) {
            predictionsByIndex.keySet().retainAll(validPredictionEntries);
            measuredTransitTimes.keySet().retainAll(validPredictionEntries);
            transitTimeHistory.keySet().retainAll(validPredictionEntries);
            currentTransitTime.keySet().retainAll(validPredictionEntries);
        }

        // [] Called after the schedule index has been changed.
        if (lastScheduleIndex >= 0 && lastScheduleIndex != currentScheduleIndex && predictionsByIndex.containsKey(lastScheduleIndex)) {
            predictionsByIndex.get(lastScheduleIndex).nextCycle();
        }
        if (!hasCustomTravelSections() && lastScheduleIndex > currentScheduleIndex) { // Manually call section change event if there are no sections defined.
            changeCurrentSection(currentTravelSectionIndex);
        }
        lastScheduleIndex = currentScheduleIndex;

        // [] train cancelled manager
        boolean isNowCancelled = !(TrainUtils.isTrainValid(train) && isInitialized()) || train.runtime.paused;
        if (this.cancelled && !isNowCancelled) { // Train should no longer be cancelled -> restart
            hasStarted = false;
            initializationCompleted = false;
            initializationFinishTask = false;
            sessionId = UUID.randomUUID();
            resetPredictions();
        }
        this.cancelled = isNowCancelled;

        applyStatus();

        if (destinationChanged) {
            destinationChanged = false;
            notifyListeners(EVENT_DESTINATION_CHANGED, this);
        }

        if (initializationFinishTask) {
            initializationFinishTask = false;
            onInitialize();
        }
    }

    /** Called every tick */
    public void tick() {       
        if (train.runtime.paused) {
            return;
        }

        if (!isAtStation()) {
            transitTime++; // CRN transit time measurement
        }

        // Waiting for signal processor
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
        }

        if (isWaitingForSignal) {
            waitingForSignalTicks++;
        }

        this.wasWaitingForSignal = isWaitingForSignal;
    }

    public void updateTotalDuration() {
        // measuredTransitTimes
        int newDuration = currentTransitTime.values().stream().mapToInt(x -> x).sum() + getPredictions().stream().mapToInt(x -> x.getStayDuration()).sum();
        int oldTotalDuration = this.totalDuration;
        if (CRNEventsManager.isRegistered(TotalDurationTimeChangedEvent.class) && this.totalDuration > 0 && this.totalDuration != newDuration) {
            CRNEventsManager.getEvent(TotalDurationTimeChangedEvent.class).run(train, this.totalDuration, newDuration);
        }
        this.totalDuration = newDuration;
        if (oldTotalDuration != INVALID) {
            notifyListeners(EVENT_TOTAL_DURATION_CHANGED, this);
        }
        resetPredictions();
    }

    /**
     * Called when the train reaches a station.
     * @param createTicksInTransit Ticks measured by Create.
     */
    public void reachDestination(long destinationReachTime, int createTicksInTransit) {
        this.destinationReachTime = destinationReachTime;

        if (hasStarted) {
            processTransitHistory(transitTimeHistory.computeIfAbsent(currentScheduleIndex, x -> new PriorityQueue<>()));
            this.measuredTransitTimes.put(currentScheduleIndex, ModCommonConfig.CUSTOM_TRANSIT_TIME_CALCULATION.get() ? createTicksInTransit : transitTime);
        }
        this.transitTime = 0;
        this.waitingForSignalTicks = 0;
        this.waitingForSignalId = null;
        this.delaysBySignal.clear();
        this.hasStarted = true;
        this.isAtStation = true;

        if (!initializationCompleted && isInitialized()) {
            initializationCompleted = true;
            initializationFinishTask = true;
        }        

        if (sectionChanged) {            
            sectionChanged = false;
            if (!isDynamic() || (ModCommonConfig.AUTO_RESET_TIMINGS.get() > 0 && refreshTimingsCounter >= ModCommonConfig.AUTO_RESET_TIMINGS.get())) {
                resetPredictions();
            } else {
                resetStatus(true);
            }
            notifyListeners(EVENT_SECTION_CHANGED, this);
        }

        notifyListeners(EVENT_STATION_REACHED, this);
    }

    public void leaveDestination() {
        this.currentScheduleIndex = getTrain().runtime.currentEntry;
        this.isAtStation = false;
    }

    public void onInitialize() {
        updateTotalDuration();
    }
    
    /** Checks and calculates a new total duration time if necessary. */
    private void processTransitHistory(Queue<Integer> history) {
        // First initialization
        if (!currentTransitTime.containsKey(currentScheduleIndex) || currentTransitTime.get(currentScheduleIndex) < 0) {
            fillHistory(history, transitTime);
            currentTransitTime.put(currentScheduleIndex, transitTime); // Set initial reference transit time
        }

        // remove excess elements 
        while (history.size() >= getHistoryBufferSize()) {
            history.poll();
        }
        history.offer(transitTime); // add current transit time to the history

        int refCurrentTransitTime = currentTransitTime.get(currentScheduleIndex);
        double median = ModUtils.calculateMedian(history, ModCommonConfig.TOTAL_DURATION_DEVIATION_THRESHOLD.get(), x -> true);

        if (Math.abs(refCurrentTransitTime - median) > ModCommonConfig.TOTAL_DURATION_DEVIATION_THRESHOLD.get()) { // Deviation is too large -> change transit time for this section
            int newValue = ModUtils.calculateMedian(history, ModCommonConfig.TOTAL_DURATION_DEVIATION_THRESHOLD.get(), x -> Math.abs(refCurrentTransitTime - x) > ModCommonConfig.TOTAL_DURATION_DEVIATION_THRESHOLD.get());
            currentTransitTime.put(currentScheduleIndex, newValue); // save transit time for this section
            fillHistory(history, newValue); // Reset the history
            updateTotalDuration();
        } else if (Math.abs(refCurrentTransitTime - transitTime) < ModCommonConfig.TOTAL_DURATION_DEVIATION_THRESHOLD.get()) { // new value is smaller than current -> reset history (no changes needed)
            fillHistory(history, refCurrentTransitTime);
        }
    }

    private void fillHistory(Queue<Integer> history, int value) {
        history.clear();
        for (int i = 0; i < getHistoryBufferSize(); i++) {
            history.add(value);
        }
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

        CompoundTag transitTimes = new CompoundTag();
        for (Entry<Integer, Integer> entry : this.currentTransitTime.entrySet()) {
            transitTimes.putInt(String.valueOf(entry.getKey()), entry.getValue());
        }

        nbt.putUUID(NBT_ID, getSessionId());
        nbt.putUUID(NBT_TRAIN_ID, getTrainId());
        nbt.put(NBT_PREDICTIONS, predictions);
        nbt.put(NBT_TRANSIT_TIMES, transitTimes);
        nbt.putInt(NBT_CURRENT_SCHEDULE_INDEX, currentScheduleIndex);
        nbt.putLong(NBT_LAST_DELAY_OFFSET, lastSectionDelayOffset);
        nbt.putBoolean(NBT_CANCELLED, cancelled);
        nbt.putString(NBT_LINE_ID, lineId == null ? "" : lineId);
        return nbt;
    }
    
    public static TrainData fromNbt(CompoundTag nbt) {
        UUID trainId = nbt.getUUID(NBT_TRAIN_ID);
        UUID sessionId = nbt.getUUID(NBT_ID);
        TrainData data = new TrainData(TrainUtils.getTrain(trainId).get(), sessionId); // TODO
        data.deserializeNbt(nbt);
        return data;
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

        CompoundTag transitTimes = nbt.getCompound(NBT_TRANSIT_TIMES);
        for (String key : transitTimes.getAllKeys()) {
            try {
                int idx = Integer.parseInt(key);
                int time = transitTimes.getInt(key);
                if (time > 0) {
                    fillHistory(transitTimeHistory.computeIfAbsent(idx, x -> new PriorityQueue<>()), time);
                    this.measuredTransitTimes.put(idx, time);
                    this.currentTransitTime.put(idx, time);
                }
            } catch (Exception e) {
                CreateRailwaysNavigator.LOGGER.warn("Unable to load transit time with index '" + key + "': The value is not an integer.", e);
            }
        }

        this.currentScheduleIndex = nbt.getInt(NBT_CURRENT_SCHEDULE_INDEX);
        this.lastScheduleIndex = currentScheduleIndex;
        this.currentTravelSectionIndex = getSectionForIndex(currentScheduleIndex).getScheduleIndex();
        this.lineId = nbt.getString(NBT_LINE_ID);
        this.lastSectionDelayOffset = nbt.getLong(NBT_LAST_DELAY_OFFSET);
        this.cancelled = nbt.getBoolean(NBT_CANCELLED);
    }

    public synchronized void shiftTime(long l) {
        this.destinationReachTime += l;
        predictionsByIndex.values().forEach(x -> x.shiftTime(l));
    }    
}
