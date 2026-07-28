package de.mrjulsen.crn.core.train;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.ScheduleEntry;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import com.simibubi.create.content.trains.station.GlobalStation;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.core.TrainManager;
import de.mrjulsen.crn.api.event.RailwayBackendEvents;
import de.mrjulsen.crn.core.debug.BackendDiagnosticsRecorder;
import de.mrjulsen.crn.core.delay.DelayInstance;
import de.mrjulsen.crn.core.delay.DelayTracker;
import de.mrjulsen.crn.core.realtime.RealtimeTracker;
import de.mrjulsen.crn.core.schedule.JourneyDisplayNames;
import de.mrjulsen.crn.core.schedule.JourneyParser;
import de.mrjulsen.crn.core.schedule.JourneySection;
import de.mrjulsen.crn.core.schedule.JourneyStop;
import de.mrjulsen.crn.core.schedule.TrainJourney;
import de.mrjulsen.crn.core.timing.DepartureEstimator;
import de.mrjulsen.crn.core.timing.StopTimes;
import de.mrjulsen.crn.core.timing.StopTimings;
import de.mrjulsen.crn.core.timing.TimetableCalculator;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.data.schedule.condition.TrainSeparationCondition;
import de.mrjulsen.crn.data.settings.GlobalSettings;
import de.mrjulsen.crn.data.TrainExitSide;
import de.mrjulsen.crn.util.TrainUtils;
import de.mrjulsen.crn.mixin.ScheduleRuntimeAccessor;
import de.mrjulsen.crn.util.ModUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public final class TrackedTrain implements RealtimeTracker.Listener {

    private static final String NBT_SESSION_ID = "SessionId";
    private static final String NBT_TRAIN_ID = "TrainId";
    private static final String NBT_SERVICE = "ServiceState";
    private static final String NBT_CANCELLED = "Cancelled";
    private static final String NBT_DELAY_OFFSET = "DelayOffset";
    private static final String NBT_SECTIONS_SINCE_RESET = "SectionsSinceReset";
    private static final String NBT_TOTAL_DURATION = "TotalDuration";
    private static final String NBT_STOPS = "Stops";
    private static final String NBT_ACTIVE_DELAYS = "ActiveDelays";

    private final transient Train train;
    private final transient TrainManager owner;
    private volatile UUID sessionId;

    private final Object updateLock = new Object();

    private volatile TrainJourney journey;
    private final Map<Integer, StopTimings> timingsByEntry = new ConcurrentHashMap<>();
    private final RealtimeTracker realtime = new RealtimeTracker();

    private volatile LiveUpdateState liveUpdateState = LiveUpdateState.UNAVAILABLE;

    private volatile TrainLifecycleState lifecycle = TrainLifecycleState.PREPARING;
    private final DelayTracker delays = new DelayTracker(this);
    private volatile boolean reportable = true;
    private volatile boolean discardable = false;
    private volatile boolean wasInService = false;

    private volatile long totalDuration = -1;
    private volatile long delayOffset = 0;
    private volatile ServiceState service = ServiceState.IN_SERVICE;
    private volatile int sectionsSinceReset = 0;

    private static final int EXIT_SIDE_RETRY_TICKS = 20;
    private static final int PLATFORM_WAIT_GRACE = 100;

    private volatile long platformWaitUntil = -1;
    private volatile String platformWaitOccupant = "";

    private volatile TrainExitSide exitSide = TrainExitSide.UNKNOWN;
    private UUID exitSideStation;
    private int exitSideRetryTicks;

    private final AtomicBoolean pendingSoftReset = new AtomicBoolean(false);
    private volatile boolean timetableDirty = false;
    private volatile boolean hasArrivedOnce = false;

    public TrackedTrain(Train train, TrainManager owner) {
        this.train = train;
        this.owner = owner;
        this.sessionId = UUID.randomUUID();
        this.journey = TrainJourney.empty(train.id);
        this.realtime.sync(train);
    }

    public Train getTrain() {
        return train;
    }

    public UUID getTrainId() {
        return train.id;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public String getTrainName() {
        return train.name.getString();
    }

    public TrainJourney getJourney() {
        return journey;
    }

    public RealtimeTracker getRealtime() {
        return realtime;
    }

    public TrainLifecycleState getLifecycleState() {
        return lifecycle;
    }

    public LiveTrainState getLiveState() {
        return realtime.getLiveState();
    }

    public List<DelayInstance> getActiveDelays() {
        return delays.getActive();
    }

    public boolean isCancelled() {
        return service == ServiceState.DISRUPTED;
    }

    public boolean isBlacklisted() {
        return GlobalSettings.getInstance().isTrainBlacklisted(train);
    }

    public boolean isReportable() {
        return reportable;
    }

    public boolean shouldBeForgotten() {
        return discardable && !reportable;
    }

    public ServiceState getServiceState() {
        return service;
    }

    public long getTotalDuration() {
        return totalDuration;
    }

    public long getDelayOffset() {
        return delayOffset;
    }

    public StopTimings getTimings(JourneyStop stop) {
        return timingsByEntry.get(stop.entryIndex());
    }

    public <T> T readCoherently(Supplier<T> read) {
        synchronized (updateLock) {
            return read.get();
        }
    }

    public Optional<StopTimings> getTimings(int entryIndex) {
        return Optional.ofNullable(timingsByEntry.get(entryIndex));
    }

    public Optional<JourneyStop> getCurrentStop() {
        return train.runtime == null ? Optional.empty() : journey.getCurrentStop(train.runtime.currentEntry);
    }

    public Optional<JourneySection> getCurrentSection() {
        Optional<JourneyStop> currentStop = getCurrentStop();
        if (currentStop.isEmpty()) {
            return Optional.empty();
        }
        JourneySection section = currentStop.get().getSection();
        if (section != null && getLiveState() != LiveTrainState.AT_STATION && section.isFirstStop(currentStop.get())) {
            Optional<JourneySection> previous = journey.previousSectionOf(section)
                .filter(x -> x != section && x.includesNextSectionStart());
            if (previous.isPresent()) {
                return previous;
            }
        }
        return Optional.ofNullable(section);
    }

    public List<JourneyStop> getUpcomingStops() {
        return getCurrentStop().map(journey::getStopsInTravelOrder).orElse(List.of());
    }

    public String getDisplayStationName(JourneyStop stop) {
        return JourneyDisplayNames.realtimeStationName(stop, getTimings(stop));
    }

    public String getScheduledStationName(JourneyStop stop) {
        return JourneyDisplayNames.scheduledStationName(stop, getTimings(stop));
    }

    public String getSectionDestination(JourneyStop stop) {
        return JourneyDisplayNames.sectionDestination(journey, stop, this::getTimings);
    }

    public String getDisplayName() {
        return JourneyDisplayNames.displayName(getCurrentSection().orElse(null), getTrainName());
    }

    public long getMaxDeviation() {
        long max = 0;
        for (JourneyStop stop : getUpcomingStops()) {
            StopTimings timing = getTimings(stop);
            if (timing != null) {
                max = Math.max(max, timing.getMaxDeviation());
            }
        }
        return max;
    }

    public boolean isDelayed() {
        StopTimings timing = getCurrentStop().map(this::getTimings).orElse(null);
        if (timing == null) {
            return false;
        }
        long threshold = ModCommonConfig.SCHEDULE_DEVIATION_THRESHOLD.get();
        return getLiveState() == LiveTrainState.AT_STATION
            ? timing.isDepartureDelayed(threshold)
            : timing.isArrivalDelayed(threshold);
    }

    public void tickLive(long now) {
        realtime.tick(train, this);
        updateExitSide();
    }

    public TrainExitSide getExitSide() {
        return exitSide;
    }

    private void updateExitSide() {
        GlobalStation station = getDoorStation();
        UUID stationId = station == null ? null : station.getId();
        boolean sameStation = Objects.equals(stationId, exitSideStation);

        if (sameStation && (exitSide != TrainExitSide.UNKNOWN || ++exitSideRetryTicks < EXIT_SIDE_RETRY_TICKS)) {
            return;
        }

        exitSideStation = stationId;
        exitSideRetryTicks = 0;
        exitSide = TrainUtils.getExitSide(station);
    }

    private GlobalStation getDoorStation() {
        GlobalStation destination = train.navigation == null ? null : train.navigation.destination;
        return destination != null ? destination : train.getCurrentStation();
    }

    public void prepareFullUpdate(long now) {
        synchronized (updateLock) {
            ServiceState previousService = this.service;
            this.service = determineServiceState();
            if (previousService != this.service) {
                RailwayBackendEvents.fireServiceStateChanged(this, previousService, this.service);
            }

            if (!service.isActive()) {
                this.liveUpdateState = LiveUpdateState.outOfService(service);
                return;
            }
            if (!previousService.isActive()) {
                resumeService();
            }
            this.wasInService = true;

            refreshJourney();
            if (journey.isEmpty()) {
                this.liveUpdateState = LiveUpdateState.UNAVAILABLE;
                return;
            }

            ensureTimings();
            updateResolvedStationName();
            this.liveUpdateState = captureLiveState();
        }
    }

    private ServiceState determineServiceState() {
        if (train.runtime == null || train.derailed || train.runtime.paused || !TrainUtils.isTrainValid(train)) {
            return ServiceState.DISRUPTED;
        }
        return train.runtime.completed ? ServiceState.IDLE : ServiceState.IN_SERVICE;
    }

    private void resumeService() {
        this.sessionId = UUID.randomUUID();
        this.delayOffset = 0;
        this.sectionsSinceReset = 0;
        this.delays.clear();
        this.reportable = true;
        this.totalDuration = -1;
        timingsByEntry.values().forEach(StopTimings::resetRuntimeData);
        realtime.sync(train);
        requestSoftReset();

        if (CreateRailwaysNavigator.isDebug() || ModCommonConfig.ADVANCED_LOGGING.get()) {
            CreateRailwaysNavigator.LOGGER.info("[Backend] Train '{}' is back in service.", getTrainName());
        }
    }

    private LiveUpdateState captureLiveState() {
        boolean atStation = train.navigation != null && train.navigation.destination == null
            && train.runtime.state == ScheduleRuntime.State.POST_TRANSIT;

        int remainingTransit = 0;
        if (!atStation) {
            StopTimings currentTiming = journey.getCurrentStop(train.runtime.currentEntry).map(this::getTimings).orElse(null);
            if (currentTiming != null) {
                int nonMovingTicks = realtime.getTotalSignalWaitTicks() + realtime.getStalledTicks() + realtime.getNoPathTicks();
                remainingTransit = TimetableCalculator.estimateRemainingTransit(train, currentTiming, realtime.getTransitTicks(), nonMovingTicks);
            }
        }

        return new LiveUpdateState(
            true,
            ServiceState.IN_SERVICE,
            train.runtime.currentEntry,
            atStation,
            remainingTransit
        );
    }

    public void fullUpdate(long now) {
        LiveUpdateState live = liveUpdateState;
        if (!live.available()) {
            this.lifecycle = TrainLifecycleState.PREPARING;
            this.delays.clear();
            this.reportable = true;
            return;
        }

        synchronized (updateLock) {
            if (!live.service().isActive()) {
                enterOutOfService(live.service(), now);
                return;
            }
            this.reportable = true;

            TimetableCalculator.projectRealtime(journey, this::getTimings, live.currentEntry(), live.atStation(), now, live.remainingTransitTicks());

            updateLifecycle();
            updateTimetable(now);
            delays.refresh(now);
        }
    }

    private void enterOutOfService(ServiceState reason, long now) {
        if (reason == ServiceState.IDLE) {
            this.lifecycle = TrainLifecycleState.IDLE;
            delays.clear();
            this.reportable = true;
            this.discardable = false;
            return;
        }

        this.lifecycle = TrainLifecycleState.CANCELLED;
        delays.refresh(now);

        if (!wasInService) {
            this.reportable = false;
            this.discardable = true;
            return;
        }

        DelayTracker.DisruptionOutcome outcome = delays.evaluateDisruption(now);
        this.reportable = outcome.visible();
        this.discardable = outcome.discardWhenExpired();
    }

    private void refreshJourney() {
        if (!JourneyParser.isOutdated(journey, train)) {
            return;
        }

        boolean scheduleReplaced = journey.getSchedule() != null;

        this.journey = JourneyParser.parse(train);
        if (scheduleReplaced) {
            discardLearnedData();
        } else {
            timingsByEntry.keySet().retainAll(journey.getStops().stream().map(JourneyStop::entryIndex).toList());
        }
        realtime.sync(train);

        if (ModCommonConfig.ADVANCED_LOGGING.get()) {
            CreateRailwaysNavigator.LOGGER.info("[Backend] Parsed journey of '{}': {} stops, {} sections, cyclic={}", getTrainName(), journey.getStopCount(), journey.getSections().size(), journey.isCyclic());
        }
    }

    private void discardLearnedData() {
        timingsByEntry.clear();
        this.sessionId = UUID.randomUUID();
        this.totalDuration = -1;
        this.delayOffset = 0;
        this.sectionsSinceReset = 0;
        this.delays.clear();
        this.hasArrivedOnce = false;
        this.timetableDirty = false;
        this.pendingSoftReset.set(false);
        RailwayBackendEvents.fireScheduleChanged(this);

        if (CreateRailwaysNavigator.isDebug() || ModCommonConfig.ADVANCED_LOGGING.get()) {
            CreateRailwaysNavigator.LOGGER.info("[Backend] Train '{}' received a new schedule. Learned data discarded.", getTrainName());
        }
    }

    private void ensureTimings() {
        List<Integer> createEstimates = ((ScheduleRuntimeAccessor)train.runtime).crn$getTransitTicks();
        for (JourneyStop stop : journey.getStops()) {
            StopTimings timing = timingsByEntry.computeIfAbsent(stop.entryIndex(), idx -> {
                StopTimings created = new StopTimings(idx);
                created.legDuration().setOnReferenceChanged(() -> {
                    timetableDirty = true;
                    BackendDiagnosticsRecorder.recordReferenceChanged(this, ModUtils.getTransformedWorldTime(), idx, created);
                });
                created.dwellResidual().setOnReferenceChanged(() -> timetableDirty = true);
                return created;
            });
            if (!timing.legDuration().isInitialized() && createEstimates != null && stop.entryIndex() < createEstimates.size()) {
                int estimate = createEstimates.get(stop.entryIndex());
                if (estimate > 0) {
                    timing.legDuration().seed(estimate);
                }
            }
        }
    }

    private void updateResolvedStationName() {
        getCurrentStop().ifPresent(stop -> {
            GlobalStation target = train.navigation != null && train.navigation.destination != null
                ? train.navigation.destination
                : train.getCurrentStation();
            if (target != null) {
                stop.updateStationName(target.name);
            }
        });
    }

    private void updateLifecycle() {
        if (!hasArrivedOnce) {
            this.lifecycle = TrainLifecycleState.PREPARING;
            return;
        }

        TrainLifecycleState previous = this.lifecycle;
        boolean allLegsKnown = journey.getStops().stream()
            .allMatch(stop -> getTimings(stop) != null && getTimings(stop).legDuration().isInitialized());
        this.lifecycle = allLegsKnown ? TrainLifecycleState.READY : TrainLifecycleState.LEARNING;

        if (this.lifecycle == TrainLifecycleState.READY && previous != TrainLifecycleState.READY) {
            requestSoftReset();
            if (CreateRailwaysNavigator.isDebug() || ModCommonConfig.ADVANCED_LOGGING.get()) {
                CreateRailwaysNavigator.LOGGER.info("[Backend] Train '{}' has learned all of its legs. Anchoring its timetable.", getTrainName());
            }
        }
    }

    private void updateTimetable(long now) {
        if (lifecycle != TrainLifecycleState.READY) {
            applyPendingSoftReset(now);
            return;
        }

        long newTotal = TimetableCalculator.computeTotalDuration(journey, this::getTimings);
        boolean firstAnchor = totalDuration <= 0 && newTotal > 0;

        if (firstAnchor || timetableDirty) {
            requestSoftReset();
            timetableDirty = false;
            if (ModCommonConfig.ADVANCED_LOGGING.get() && !firstAnchor) {
                CreateRailwaysNavigator.LOGGER.info("[Backend] Total duration of '{}' changed from {} to {} ticks. Timetable will be re-anchored.", getTrainName(), totalDuration, newTotal);
            }
        }
        this.totalDuration = newTotal;
        applyPendingSoftReset(now);
    }

    private void applyPendingSoftReset(long now) {
        if (!pendingSoftReset.compareAndSet(true, false)) {
            return;
        }

        boolean wasDelayed = isDelayed() || delayOffset > ModCommonConfig.SCHEDULE_DEVIATION_THRESHOLD.get();
        long deviationBeforeReset = getMaxDeviation();
        for (StopTimings timing : timingsByEntry.values()) {
            timing.anchorScheduleToRealtime();
        }
        BackendDiagnosticsRecorder.recordSoftReset(this, now, wasDelayed, deviationBeforeReset);
        this.delayOffset = 0;
        this.sectionsSinceReset = 0;
        RailwayBackendEvents.fireTimetableReset(this);

        if (CreateRailwaysNavigator.isDebug() || ModCommonConfig.ADVANCED_LOGGING.get()) {
            CreateRailwaysNavigator.LOGGER.info("[Backend] '{}' has reset its scheduled times.", getTrainName());
        }
    }

    public void requestSoftReset() {
        this.pendingSoftReset.set(true);
    }

    public void shiftTimes(long ticks) {
        synchronized (updateLock) {
            timingsByEntry.values().forEach(x -> x.shiftTimes(ticks));
        }
    }

    @Override
    public void onArrival(int entryIndex, int transitTicks, boolean traveled) {
        long now = ModUtils.getTransformedWorldTime();
        synchronized (updateLock) {
            journey.getStopAtEntry(entryIndex).ifPresent(stop -> {
                StopTimings timing = getTimings(stop);
                if (timing == null) {
                    return;
                }

                boolean countMeasurement = traveled && hasArrivedOnce && service.isActive();
                timing.recordArrival(now, transitTicks, countMeasurement);
                BackendDiagnosticsRecorder.recordArrival(this, now, entryIndex, transitTicks, traveled);

                StopTimes current = timing.getRealtime();
                timing.setRealtime(new StopTimes(now, Math.max(now, current.departure()), Math.max(now, current.minDeparture())));

                GlobalStation station = train.getCurrentStation();
                if (station != null) {
                    stop.updateStationName(station.name);
                    timing.recordVisitedStation(station.name);
                }

                JourneySection section = stop.getSection();
                if (section != null && section.isFirstStop(stop)
                        && journey.previousSectionOf(section).filter(x -> x != section && x.includesNextSectionStart()).isPresent()) {
                    onSectionChange();
                }

                RailwayBackendEvents.fireArrival(this, stop);
            });
            this.hasArrivedOnce = true;
        }
    }

    @Override
    public void onDeparture(int entryIndex, int dwellTicks) {
        long now = ModUtils.getTransformedWorldTime();
        synchronized (updateLock) {
            journey.getStopAtEntry(entryIndex).ifPresent(stop -> {
                StopTimings timing = getTimings(stop);
                if (timing == null) {
                    return;
                }

                recordDwellResidual(stop, timing, now);
                timing.recordDeparture(now, dwellTicks);
                owner.recordDeparture(this, stop);
                BackendDiagnosticsRecorder.recordDeparture(this, now, entryIndex, dwellTicks);

                if (journey.isCyclic() && totalDuration > 0) {
                    timing.advanceScheduledCycle(totalDuration);
                    timing.advanceRealtimeCycle(totalDuration);
                }

                journey.getNextStop(stop).ifPresent(next -> {
                    if (passesResetMarker(stop.entryIndex(), next.entryIndex())) {
                        requestSoftReset();
                    } else if (next.getSection() != stop.getSection() && !stop.getSection().includesNextSectionStart()) {
                        onSectionChange();
                    }
                });

                RailwayBackendEvents.fireDeparture(this, stop);
            });
        }
    }

    private void recordDwellResidual(JourneyStop stop, StopTimings timing, long departureTime) {
        ScheduleEntry entry = stop.getScheduleEntry();
        long arrival = timing.getLastActualArrival();
        if (arrival < 0 || !service.isActive() || !TrainSeparationCondition.isPresentIn(entry)) {
            return;
        }
        long baseline = Math.max(arrival, DepartureEstimator.estimate(entry, arrival).departure());
        timing.recordDwellResidual((int)Math.max(0, departureTime - baseline));
    }

    public void markWaitingForPlatform(String occupantName) {
        this.platformWaitUntil = ModUtils.getTransformedWorldTime() + PLATFORM_WAIT_GRACE;
        this.platformWaitOccupant = occupantName == null ? "" : occupantName;
    }

    public boolean isWaitingForPlatform() {
        return platformWaitUntil >= ModUtils.getTransformedWorldTime();
    }

    public String getPlatformWaitOccupant() {
        return platformWaitOccupant;
    }

    public long getSeparationHoldTicksRemaining() {
        if (train.runtime == null || getLiveState() != LiveTrainState.AT_STATION) {
            return 0;
        }
        return getCurrentStop()
            .map(stop -> TrainSeparationCondition.remainingHoldTicks(train, stop.getScheduleEntry()))
            .orElse(0L);
    }

    @Override
    public boolean isStopAtCurrentStation(int entryIndex) {
        GlobalStation station = train.getCurrentStation();
        if (station == null) {
            return false;
        }
        return journey.getStopAtEntry(entryIndex)
            .map(stop -> TrainUtils.stationMatches(station.name, stop.getStationFilter()))
            .orElse(false);
    }

    private boolean passesResetMarker(int fromEntry, int toEntry) {
        if (journey.getResetTimingEntries().isEmpty() || journey.getSchedule() == null) {
            return false;
        }
        int entryCount = journey.getSchedule().entries.size();
        if (entryCount <= 0) {
            return false;
        }
        for (int i = (fromEntry + 1) % entryCount, steps = 0; i != toEntry && steps < entryCount; i = (i + 1) % entryCount, steps++) {
            if (journey.getResetTimingEntries().contains(i)) {
                return true;
            }
        }
        return false;
    }

    private void onSectionChange() {
        this.delayOffset = Math.max(0, getMaxDeviation());
        this.sectionsSinceReset++;
        this.delays.clear();

        int autoReset = ModCommonConfig.AUTO_RESET_TIMINGS.get();
        if (!journey.hasFlexibleDwellTimes() || (autoReset > 0 && sectionsSinceReset >= autoReset)) {
            requestSoftReset();
        }
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putUUID(NBT_SESSION_ID, sessionId);
        nbt.putUUID(NBT_TRAIN_ID, getTrainId());
        nbt.putString(NBT_SERVICE, service.name());
        nbt.putLong(NBT_DELAY_OFFSET, delayOffset);
        nbt.putInt(NBT_SECTIONS_SINCE_RESET, sectionsSinceReset);
        nbt.putLong(NBT_TOTAL_DURATION, totalDuration);

        List<DelayInstance> activeDelays = delays.getActive();
        if (!service.isActive() && !activeDelays.isEmpty()) {
            ListTag delaysNbt = new ListTag();
            activeDelays.forEach(x -> delaysNbt.add(x.toNbt()));
            nbt.put(NBT_ACTIVE_DELAYS, delaysNbt);
        }

        CompoundTag stops = new CompoundTag();
        for (Map.Entry<Integer, StopTimings> entry : timingsByEntry.entrySet()) {
            String stationName = journey.getStopAtEntry(entry.getKey()).map(JourneyStop::getStationName).orElse(null);
            stops.put(String.valueOf(entry.getKey()), entry.getValue().toNbt(stationName));
        }
        nbt.put(NBT_STOPS, stops);
        return nbt;
    }

    public static ServiceState readPersistedServiceState(CompoundTag nbt) {
        if (nbt.contains(NBT_SERVICE)) {
            try {
                return ServiceState.valueOf(nbt.getString(NBT_SERVICE));
            } catch (IllegalArgumentException e) {
                return ServiceState.IN_SERVICE;
            }
        }
        return nbt.getBoolean(NBT_CANCELLED) ? ServiceState.DISRUPTED : ServiceState.IN_SERVICE;
    }

    public void loadNbt(CompoundTag nbt) {
        refreshJourney();
        ensureTimings();

        if (nbt.contains(NBT_SESSION_ID)) this.sessionId = nbt.getUUID(NBT_SESSION_ID);
        this.service = readPersistedServiceState(nbt);
        this.wasInService = service == ServiceState.DISRUPTED;
        this.delayOffset = nbt.getLong(NBT_DELAY_OFFSET);
        this.sectionsSinceReset = nbt.getInt(NBT_SECTIONS_SINCE_RESET);
        this.totalDuration = nbt.getLong(NBT_TOTAL_DURATION);

        List<DelayInstance> restoredDelays = new ArrayList<>();
        ListTag delaysNbt = nbt.getList(NBT_ACTIVE_DELAYS, Tag.TAG_COMPOUND);
        for (int i = 0; i < delaysNbt.size(); i++) {
            restoredDelays.add(DelayInstance.fromNbt(delaysNbt.getCompound(i)));
        }
        delays.restore(restoredDelays);

        CompoundTag stops = nbt.getCompound(NBT_STOPS);
        for (String key : stops.getAllKeys()) {
            try {
                int entryIndex = Integer.parseInt(key);
                StopTimings timing = timingsByEntry.get(entryIndex);
                if (timing == null) {
                    continue;
                }

                CompoundTag stopNbt = stops.getCompound(key);
                timing.loadNbt(stopNbt);
                journey.getStopAtEntry(entryIndex).ifPresent(stop -> stop.updateStationName(StopTimings.loadStationName(stopNbt)));
            } catch (NumberFormatException e) {
                CreateRailwaysNavigator.LOGGER.warn("[Backend] Skipping invalid stop data '{}' of train '{}'.", key, getTrainName());
            }
        }
        this.hasArrivedOnce = timingsByEntry.values().stream().anyMatch(x -> x.getCompletedVisits() > 0 || x.getLastActualArrival() >= 0);
        this.realtime.sync(train);
    }
}
