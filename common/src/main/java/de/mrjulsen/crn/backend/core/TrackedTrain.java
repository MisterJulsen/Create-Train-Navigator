package de.mrjulsen.crn.backend.core;

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
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import com.simibubi.create.content.trains.station.GlobalStation;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.backend.TrainManager;
import de.mrjulsen.crn.backend.api.event.RailwayBackendEvents;
import de.mrjulsen.crn.backend.debug.BackendDiagnosticsRecorder;
import de.mrjulsen.crn.backend.delay.DelayCause;
import de.mrjulsen.crn.backend.delay.DelayInstance;
import de.mrjulsen.crn.backend.delay.DelayTracker;
import de.mrjulsen.crn.backend.delay.DisruptionHandling;
import de.mrjulsen.crn.backend.realtime.RealtimeTracker;
import de.mrjulsen.crn.backend.schedule.JourneyDisplayNames;
import de.mrjulsen.crn.backend.schedule.JourneyParser;
import de.mrjulsen.crn.backend.schedule.JourneySection;
import de.mrjulsen.crn.backend.schedule.JourneyStop;
import de.mrjulsen.crn.backend.schedule.TrainJourney;
import de.mrjulsen.crn.backend.timing.StopTimes;
import de.mrjulsen.crn.backend.timing.StopTimings;
import de.mrjulsen.crn.backend.timing.TimetableCalculator;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.data.TrainExitSide;
import de.mrjulsen.crn.data.train.TrainUtils;
import de.mrjulsen.crn.mixin.ScheduleRuntimeAccessor;
import de.mrjulsen.crn.util.ModUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * All backend state of a single train: its parsed {@link TrainJourney}, the timing data of every
 * stop, the per-tick {@link RealtimeTracker} and the derived status information.
 * <p>
 * Update cycle (see {@link de.mrjulsen.crn.backend.RailwayBackend} for the threading model):
 * <ul>
 *   <li>{@link #tickLive(long)} - every tick, server thread: live observation.</li>
 *   <li>{@link #prepareFullUpdate(long)} - periodically, server thread: schedule parsing and
 *       capture of the live train state.</li>
 *   <li>{@link #fullUpdate(long)} - periodically, worker thread: projection, timetable
 *       maintenance and delay detection.</li>
 * </ul>
 * Arrivals are detected on the server thread while the projection runs on the worker, so both are
 * serialized per train via an update lock.
 */
public final class TrackedTrain implements RealtimeTracker.Listener {

    private static final String NBT_SESSION_ID = "SessionId";
    private static final String NBT_TRAIN_ID = "TrainId";
    private static final String NBT_SERVICE = "ServiceState";
    /** Superseded by {@link #NBT_SERVICE}; still read so existing worlds keep their state. */
    private static final String NBT_CANCELLED = "Cancelled";
    private static final String NBT_DELAY_OFFSET = "DelayOffset";
    private static final String NBT_SECTIONS_SINCE_RESET = "SectionsSinceReset";
    private static final String NBT_TOTAL_DURATION = "TotalDuration";
    private static final String NBT_STOPS = "Stops";
    private static final String NBT_ACTIVE_DELAYS = "ActiveDelays";

    private final transient Train train;
    private final transient TrainManager owner;
    private volatile UUID sessionId;

    /** Serializes the worker's full update against the server thread's arrival/departure handling. */
    private final Object updateLock = new Object();

    private volatile TrainJourney journey;
    private final Map<Integer, StopTimings> timingsByEntry = new ConcurrentHashMap<>();
    private final RealtimeTracker realtime = new RealtimeTracker();

    /** The live train state captured for the running full update. */
    private volatile LiveUpdateState liveUpdateState = LiveUpdateState.UNAVAILABLE;

    private volatile TrainLifecycleState lifecycle = TrainLifecycleState.PREPARING;
    private final DelayTracker delays = new DelayTracker(this);
    /** Whether the train is still worth showing. */
    private volatile boolean reportable = true;
    /** Whether this train's data may be discarded once it is no longer reported. */
    private volatile boolean discardable = false;
    /** Whether the train has been observed in service at all. */
    private volatile boolean wasInService = false;

    private volatile long totalDuration = -1;
    private volatile long delayOffset = 0;
    /** Whether the train can currently run at all. The source of truth for "cancelled". */
    private volatile ServiceState service = ServiceState.IN_SERVICE;
    private volatile int sectionsSinceReset = 0;

    /** How long to wait before asking again for an exit side that could not be determined, in ticks. */
    private static final int EXIT_SIDE_RETRY_TICKS = 20;

    private volatile TrainExitSide exitSide = TrainExitSide.UNKNOWN;
    /** Server thread only, alongside {@link #updateExitSide()}. */
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

    /** The underlying live train object. */
    public Train getTrain() {
        return train;
    }

    /** The id of the train this data belongs to. */
    public UUID getTrainId() {
        return train.id;
    }

    /** Changes whenever the tracking of this train restarts (e.g. after being cancelled). */
    public UUID getSessionId() {
        return sessionId;
    }

    /** The train's own name, as opposed to its line name. */
    public String getTrainName() {
        return train.name.getString();
    }

    /** The parsed journey of the train's current schedule. */
    public TrainJourney getJourney() {
        return journey;
    }

    /** The live measurements taken from the train every tick. */
    public RealtimeTracker getRealtime() {
        return realtime;
    }

    /** How reliable this train's data currently is. */
    public TrainLifecycleState getLifecycleState() {
        return lifecycle;
    }

    /** The train's current live activity. */
    public LiveTrainState getLiveState() {
        return realtime.getLiveState();
    }

    /** The status reasons currently applying to this train, most important first. */
    public List<DelayInstance> getActiveDelays() {
        return delays.getActive();
    }

    /** Whether the train is out of service because of a disruption (derailed, paused, unusable). */
    public boolean isCancelled() {
        return service == ServiceState.DISRUPTED;
    }

    /**
     * Whether this train is hidden from public displays and route searches.
     * <p>
     * A blacklisted train is still tracked and still learns, so taking it off the boards and putting
     * it back is instant rather than costing it a whole relearning cycle. Only the queries withhold
     * it - see {@link de.mrjulsen.crn.backend.api.RailwayBackendApi}.
     */
    public boolean isBlacklisted() {
        return GlobalSettings.getInstance().isTrainBlacklisted(train);
    }

    /**
     * Whether this train should still appear on public displays and in board queries. A train that
     * is out of service stays reportable for as long as its reason declares
     * ({@link DelayCause#displayDurationWhileOutOfService()}); afterwards it goes quiet but keeps
     * all of its data, so returning to service makes it immediately usable again.
     */
    public boolean isReportable() {
        return reportable;
    }

    /**
     * Whether the backend should stop tracking this train and discard its data. Only ever true for
     * a train taken out of service deliberately, and for one never seen running at all; a train
     * that suffered a fault keeps its data for inspection.
     *
     * @see de.mrjulsen.crn.backend.delay.DisruptionHandling
     */
    public boolean shouldBeForgotten() {
        return discardable && !reportable;
    }

    /** Why the train is or is not running. */
    public ServiceState getServiceState() {
        return service;
    }

    /** The duration of one full journey cycle in ticks, or {@code -1} while still learning. */
    public long getTotalDuration() {
        return totalDuration;
    }

    /** Deviation in ticks carried over from a previous section, not counted as current delay. */
    public long getDelayOffset() {
        return delayOffset;
    }

    /** The timing data of the given stop, or {@code null} if it has none yet. */
    public StopTimings getTimings(JourneyStop stop) {
        return timingsByEntry.get(stop.entryIndex());
    }

    /**
     * Runs a read while no update is in progress, so the result cannot mix values from two update
     * passes. Individual getters are safe to call without it, they just carry no such guarantee.
     * <p>
     * <b>The read must only touch this train.</b> It runs while holding this train's update lock, so
     * reaching into another train that is itself being read the same way can deadlock the pair.
     * Reads that need several trains take one snapshot per train instead, one after the other.
     * <p>
     * Meant for building snapshots inside the backend; consumers should ask
     * {@link de.mrjulsen.crn.backend.api.RailwayBackendApi} for a snapshot rather than call this.
     */
    public <T> T readCoherently(Supplier<T> read) {
        synchronized (updateLock) {
            return read.get();
        }
    }

    /** The timing data of the stop at the given schedule entry index. */
    public Optional<StopTimings> getTimings(int entryIndex) {
        return Optional.ofNullable(timingsByEntry.get(entryIndex));
    }

    /** The stop the train is currently at or traveling to. */
    public Optional<JourneyStop> getCurrentStop() {
        return train.runtime == null ? Optional.empty() : journey.getCurrentStop(train.runtime.currentEntry);
    }

    /**
     * The section the train is currently operating in. When the preceding section
     * {@linkplain JourneySection#includesNextSectionStart() carries its start over}, the train
     * stays part of that predecessor until it arrives at the stop; otherwise the section switches
     * on departure.
     */
    public Optional<JourneySection> getCurrentSection() {
        Optional<JourneyStop> currentStop = getCurrentStop();
        if (currentStop.isEmpty()) {
            return Optional.empty();
        }
        JourneySection section = currentStop.get().getSection();
        if (section != null && getLiveState() != LiveTrainState.AT_STATION && section.isFirstStop(currentStop.get())) {
            JourneySection previous = journey.getPreviousSection(section);
            if (previous != section && previous.includesNextSectionStart()) {
                return Optional.of(previous);
            }
        }
        return Optional.ofNullable(section);
    }

    /** All upcoming stops in travel order, starting with the current one. */
    public List<JourneyStop> getUpcomingStops() {
        return getCurrentStop().map(journey::getStopsInTravelOrder).orElse(List.of());
    }

    /** The station the given stop is actually heading to. */
    public String getDisplayStationName(JourneyStop stop) {
        return JourneyDisplayNames.realtimeStationName(stop, getTimings(stop));
    }

    /** The station the timetable plans for the given stop, which a diverted train may not serve. */
    public String getScheduledStationName(JourneyStop stop) {
        return JourneyDisplayNames.scheduledStationName(stop, getTimings(stop));
    }

    /** The terminus to display as the train's destination at the given stop. */
    public String getSectionDestination(JourneyStop stop) {
        return JourneyDisplayNames.sectionDestination(journey, stop, this::getTimings);
    }

    /** The line name of the current section, falling back to the train name. */
    public String getDisplayName() {
        return JourneyDisplayNames.displayName(getCurrentSection().orElse(null), getTrainName());
    }

    /**
     * The highest deviation from the timetable across the stops still to come, in ticks. Never
     * negative.
     * <p>
     * Deliberately restricted to the remaining run rather than every stop the train has: the
     * projection only rewrites stops from the current one onwards, so on a non-cyclic journey the
     * stops already served keep the projected times of when they were served. Taking the maximum
     * over those too would leave the train reporting the worst delay it ever had for the rest of its
     * life, long after it had caught up. On a cyclic journey the run wraps around and covers every
     * stop anyway, so nothing changes there.
     */
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

    /** Whether the train is delayed at its current stop, excluding carried-over deviation. */
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

    /** Observes the train. Called every tick on the server thread; must stay cheap. */
    public void tickLive(long now) {
        realtime.tick(train, this);
        updateExitSide();
    }

    /**
     * Which side of the train the platform will be on at the stop it is heading for, or standing at.
     * <p>
     * Measured rather than derived on demand, because working it out reads the station's block entity
     * and the track graph - both of which only answer truthfully on the server thread. Asking from a
     * packet handler, which is where a display's request arrives, returns nothing most of the time.
     */
    public TrainExitSide getExitSide() {
        return exitSide;
    }

    /**
     * Re-measures the exit side when the train starts heading somewhere else, and keeps trying while
     * the answer is unknown - a station whose chunk is not loaded yet has none to give, and the train
     * is usually on its way there.
     */
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

    /**
     * The station whose platform the doors will open onto: the one being navigated to, or - once the
     * train has pulled in and Create has cleared the destination - the one it is standing at.
     */
    private GlobalStation getDoorStation() {
        GlobalStation destination = train.navigation == null ? null : train.navigation.destination;
        return destination != null ? destination : train.getCurrentStation();
    }

    /**
     * Reads everything the calculation needs from the live train objects, so the worker does not
     * have to. Called on the server thread right before a full update is dispatched. A train that
     * cannot run returns early; the worker skips it anyway.
     */
    public void prepareFullUpdate(long now) {
        // Same lock as the projection, so a reader never sees the journey replaced halfway.
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

    /** Whether the train can produce data at all. Kept cheap: it gates all expensive work. */
    private ServiceState determineServiceState() {
        if (train.runtime == null || train.derailed || train.runtime.paused || !TrainUtils.isTrainValid(train)) {
            return ServiceState.DISRUPTED;
        }
        return train.runtime.completed ? ServiceState.IDLE : ServiceState.IN_SERVICE;
    }

    /**
     * Returns a train to operation with a new session and no times from the interrupted run, which
     * would otherwise surface as one enormous delay. Learned durations and station guesses are
     * kept, so the train is immediately usable again.
     */
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

    /** Captures the live train state for the upcoming full update. Server thread only. */
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

    /**
     * Performs the projection, timetable maintenance and delay detection. Called periodically on
     * the worker thread, working on the state captured by {@link #prepareFullUpdate(long)}.
     */
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
            // Must stay inside the lock: this builds on the previous reason list.
            delays.refresh(now);
        }
    }

    /**
     * Handles a train that cannot run. Everything already measured is left untouched, so displays
     * keep showing its last known state. No projection runs, which also stops those values from
     * drifting along with the world clock and reporting an ever-growing delay while standing still.
     */
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

    /** Re-parses the journey when the underlying schedule has changed. */
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

    /**
     * Discards everything this train has measured, for when it receives a new schedule: its stops,
     * their order and the distances between them may all have changed, so the learned durations
     * describe a service that no longer exists. The train starts over as a newly discovered one,
     * with a new session so consumers know not to relate its data to what they saw before.
     */
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

    /** Creates missing timing entries and seeds them with the train's own transit estimates. */
    private void ensureTimings() {
        List<Integer> createEstimates = ((ScheduleRuntimeAccessor)train.runtime).crn$getTransitTicks();
        for (JourneyStop stop : journey.getStops()) {
            StopTimings timing = timingsByEntry.computeIfAbsent(stop.entryIndex(), idx -> {
                StopTimings created = new StopTimings(idx);
                created.legDuration().setOnReferenceChanged(() -> {
                    timetableDirty = true;
                    BackendDiagnosticsRecorder.recordReferenceChanged(this, ModUtils.getTransformedWorldTime(), idx, created);
                });
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

    /** Keeps the resolved station name of the current stop up to date. */
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

    /** Classifies how reliable the data of a running train is. */
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

    /** Maintains the total duration and (re-)anchors the timetable when necessary. */
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
        // Claimed atomically so a reset requested mid-update survives to the next pass.
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

    /** Requests a reset of the timetable to the projected times. Learned durations are kept. */
    public void requestSoftReset() {
        this.pendingSoftReset.set(true);
    }

    /**
     * Shifts all absolute timestamps by the given amount, after a world time jump. Takes the update
     * lock: interleaved with a running projection the shift would be partly overwritten.
     */
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
                if (section != null && section.isFirstStop(stop)) {
                    JourneySection previous = journey.getPreviousSection(section);
                    if (previous != section && previous.includesNextSectionStart()) {
                        onSectionChange();
                    }
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

    /** Whether a timing reset marker lies between the two schedule entries. */
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

    /**
     * Closes the finished section: its accumulated deviation becomes the carried-over offset, so
     * from the new section's point of view it is delay from a previous journey rather than freshly
     * caused here. The section's own reasons collapse into that single carried-over one.
     */
    private void onSectionChange() {
        this.delayOffset = Math.max(0, getMaxDeviation());
        this.sectionsSinceReset++;
        this.delays.clear();

        int autoReset = ModCommonConfig.AUTO_RESET_TIMINGS.get();
        if (!journey.hasFlexibleDwellTimes() || (autoReset > 0 && sectionsSinceReset >= autoReset)) {
            requestSoftReset();
        }
    }

    /** Serializes this train's learned and current data. */
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

    /**
     * Reads the stored service state without instantiating a train, so a train that has not been
     * discovered in this session can be told apart from one that was never usable. Falls back to
     * the boolean flag older saves used.
     */
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

    /** Restores persisted data, re-parsing the journey first. */
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
