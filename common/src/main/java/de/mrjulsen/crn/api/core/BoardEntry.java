package de.mrjulsen.crn.api.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import de.mrjulsen.crn.core.train.LiveTrainState;
import de.mrjulsen.crn.core.train.ServiceState;
import de.mrjulsen.crn.core.train.TrackedTrain;
import de.mrjulsen.crn.core.delay.DelayInstance;
import de.mrjulsen.crn.core.schedule.JourneyDisplayNames;
import de.mrjulsen.crn.core.schedule.JourneySection;
import de.mrjulsen.crn.core.schedule.JourneyStop;
import de.mrjulsen.crn.core.schedule.TrainJourney;
import de.mrjulsen.crn.core.timing.CycleProjector;
import de.mrjulsen.crn.core.timing.StopTimes;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.data.settings.GlobalSettings;
import de.mrjulsen.crn.util.NbtHelper;
import de.mrjulsen.mcdragonlib.util.DLColor;
import net.minecraft.nbt.CompoundTag;

/**
 * One call of one train at one station, as a departure or arrival board would show it.
 * <p>
 * A call has two sides, and they need not describe the same service: a train that changes section
 * at this station arrives as one line and departs as another. The record therefore carries both,
 * and the methods taking a {@link CallDirection} pick the side wanted. The plain
 * {@code line}/{@code category} components and the no-argument methods describe the departure.
 * <p>
 * Times are in the unit described by {@link RailwayBackendApi#currentTime()} and may be unknown; see
 * {@link StopTimes#isKnown()}.
 *
 * @param trainId          The calling train.
 * @param sessionId        The train's tracking session, which together with {@code entryIndex}
 *                         identifies this particular call.
 * @param trainName        The train's own name.
 * @param carriageCount    How many carriages the train has.
 * @param line             The line the train departs as.
 * @param category         The category the train departs as.
 * @param arrivalLine      The line the train arrives as, which differs where it changes section
 *                         here.
 * @param arrivalCategory  The category the train arrives as.
 * @param station          The station actually being called at.
 * @param scheduledStation The station the timetable expected, which differs when the train is
 *                         diverted.
 * @param origin           Where the service the train departs as began.
 * @param title            The schedule title in force here, or empty.
 * @param destination      Where the train is bound for after this call.
 * @param scheduled        The timetable times for this call.
 * @param realtime         The currently projected times.
 * @param serviceState     Whether the train is able to run at all.
 * @param visitState       Whether the train has passed this call, stands here now, or is still to
 *                         come.
 * @param entryIndex       The call's position among the schedule's entries.
 * @param sectionIndex     The section this call belongs to, or {@code -1}.
 * @param terminus         Whether the service ends here, so travellers must leave the train.
 * @param originating      Whether the service begins here.
 * @param stopovers        The stations the train will serve onward from here, in order.
 * @param delays           Why the train is late or disrupted, most important first.
 */
public record BoardEntry(
    UUID trainId,
    UUID sessionId,
    String trainName,
    int carriageCount,
    LineRef line,
    CategoryRef category,
    LineRef arrivalLine,
    CategoryRef arrivalCategory,
    StationRef station,
    StationRef scheduledStation,
    StationRef origin,
    String title,
    StationRef destination,
    StopTimes scheduled,
    StopTimes realtime,
    ServiceState serviceState,
    StopVisitState visitState,
    int entryIndex,
    int sectionIndex,
    boolean terminus,
    boolean originating,
    List<StationRef> stopovers,
    List<DelayInstance> delays
) {

    private static final String NBT_TRAIN_ID = "TrainId";
    private static final String NBT_SESSION_ID = "SessionId";
    private static final String NBT_TRAIN_NAME = "TrainName";
    private static final String NBT_CARRIAGE_COUNT = "CarriageCount";
    private static final String NBT_LINE = "Line";
    private static final String NBT_CATEGORY = "Category";
    private static final String NBT_ARRIVAL_LINE = "ArrivalLine";
    private static final String NBT_ARRIVAL_CATEGORY = "ArrivalCategory";
    private static final String NBT_STATION = "Station";
    private static final String NBT_SCHEDULED_STATION = "ScheduledStation";
    private static final String NBT_ORIGIN = "Origin";
    private static final String NBT_TITLE = "Title";
    private static final String NBT_DESTINATION = "Destination";
    private static final String NBT_SCHEDULED = "Scheduled";
    private static final String NBT_REALTIME = "Realtime";
    private static final String NBT_SERVICE_STATE = "ServiceState";
    private static final String NBT_VISIT_STATE = "VisitState";
    private static final String NBT_ENTRY_INDEX = "EntryIndex";
    private static final String NBT_SECTION_INDEX = "SectionIndex";
    private static final String NBT_TERMINUS = "Terminus";
    private static final String NBT_ORIGINATING = "Originating";
    private static final String NBT_STOPOVERS = "Stopovers";
    private static final String NBT_DELAYS = "Delays";

    public BoardEntry {
        stopovers = stopovers == null ? List.of() : List.copyOf(stopovers);
        delays = delays == null ? List.of() : List.copyOf(delays);
        trainName = trainName == null ? "" : trainName;
        line = line == null ? LineRef.NONE : line;
        category = category == null ? CategoryRef.NONE : category;
        arrivalLine = arrivalLine == null ? line : arrivalLine;
        arrivalCategory = arrivalCategory == null ? category : arrivalCategory;
        station = station == null ? StationRef.NONE : station;
        scheduledStation = scheduledStation == null ? station : scheduledStation;
        origin = origin == null ? StationRef.NONE : origin;
        title = title == null ? "" : title;
        destination = destination == null ? StationRef.NONE : destination;
        scheduled = scheduled == null ? StopTimes.UNKNOWN : scheduled;
        realtime = realtime == null ? StopTimes.UNKNOWN : realtime;
        serviceState = serviceState == null ? ServiceState.IN_SERVICE : serviceState;
        visitState = visitState == null ? StopVisitState.UPCOMING : visitState;
    }

    public static BoardEntry of(TrackedTrain train, JourneyStop stop) {
        StopSnapshot snapshot = StopSnapshot.of(train, stop);
        TrainJourney journey = train.getJourney();
        JourneySection section = stop.getSection();
        JourneySection arrivalSection = arrivalSectionOf(journey, stop, section);

        return new BoardEntry(
            train.getTrainId(),
            train.getSessionId(),
            train.getTrainName(),
            train.getTrain() == null || train.getTrain().carriages == null ? 0 : train.getTrain().carriages.size(),
            LineRef.of(section == null ? null : section.getTrainLine().orElse(null)),
            CategoryRef.of(section == null ? null : section.getTrainCategory().orElse(null)),
            LineRef.of(arrivalSection == null ? null : arrivalSection.getTrainLine().orElse(null)),
            CategoryRef.of(arrivalSection == null ? null : arrivalSection.getTrainCategory().orElse(null)),
            snapshot.realtimeStation(),
            snapshot.scheduledStation(),
            journey.getOriginOf(stop).map(x -> StationRef.of(train.getDisplayStationName(x))).orElse(StationRef.NONE),
            stop.getTitle() == null ? "" : stop.getTitle(),
            resolveDestination(train, stop, snapshot),
            snapshot.scheduled(),
            snapshot.realtime(),
            train.getServiceState(),
            visitStateOf(train, stop),
            stop.entryIndex(),
            snapshot.sectionIndex(),
            journey.isTerminus(stop),
            journey.isOrigin(stop),
            collectStopovers(train, stop, section),
            train.getActiveDelays()
        );
    }

    private static JourneySection arrivalSectionOf(TrainJourney journey, JourneyStop stop, JourneySection section) {
        if (section == null) {
            return null;
        }
        if (!section.isUsable()) {
            return journey.previousSectionOf(section).orElse(section);
        }
        if (!section.isFirstStop(stop)) {
            return section;
        }
        return journey.previousSectionOf(section)
            .filter(journey::carriesPassengersOnward)
            .orElse(section);
    }

    private static StopVisitState visitStateOf(TrackedTrain train, JourneyStop stop) {
        boolean here = train.getCurrentStop().map(x -> x.getOrderIndex() == stop.getOrderIndex()).orElse(false);
        return here && train.getLiveState() == LiveTrainState.AT_STATION
            ? StopVisitState.CURRENT
            : StopVisitState.UPCOMING;
    }

    private static StationRef resolveDestination(TrackedTrain train, JourneyStop stop, StopSnapshot snapshot) {
        String sectionDestination = train.getSectionDestination(stop);
        if (!sectionDestination.isBlank()) {
            return StationRef.of(sectionDestination);
        }
        return train.getJourney().getNextStop(stop)
            .map(x -> StationRef.of(train.getDisplayStationName(x)))
            .orElse(snapshot.realtimeStation());
    }

    private static List<StationRef> collectStopovers(TrackedTrain train, JourneyStop stop, JourneySection section) {
        if (section == null) {
            return List.of();
        }
        JourneyStop terminus = JourneyDisplayNames.terminusStop(train.getJourney(), section).orElse(null);
        List<StationRef> stopovers = new ArrayList<>();
        boolean reached = false;
        for (JourneyStop other : section.getStops()) {
            if (other == stop) {
                reached = true;
                continue;
            }
            if (!reached || other == terminus || GlobalSettings.getInstance().isStationBlacklisted(other.getStationName())) {
                continue;
            }
            stopovers.add(StationRef.of(train.getDisplayStationName(other)));
        }
        return stopovers;
    }

    /**
     * The destination to show: the schedule title where one is set, otherwise the destination
     * station's display name.
     */
    public String destinationText() {
        return title.isBlank() ? destination.displayName() : title;
    }

    /** The name of the station actually being called at. */
    public String stationName() {
        return station.name();
    }

    /**
     * How much later than scheduled the train arrives, in ticks. Negative when it is early, zero
     * where either time is unknown.
     */
    public long arrivalDeviation() {
        return scheduled.isKnown() && realtime.isKnown() ? realtime.arrival() - scheduled.arrival() : 0;
    }

    /** The same for the departure. */
    public long departureDeviation() {
        return scheduled.isKnown() && realtime.isKnown() ? realtime.departure() - scheduled.departure() : 0;
    }

    /** Whether either deviation reaches the given number of ticks. */
    public boolean isDelayed(long thresholdTicks) {
        return arrivalDeviation() >= thresholdTicks || departureDeviation() >= thresholdTicks;
    }

    /** Whether this call counts as late by the server's configured threshold. */
    public boolean isDelayed() {
        return isDelayed(ModCommonConfig.SCHEDULE_DEVIATION_THRESHOLD.get());
    }

    /** The deviation on the chosen side of the call, in ticks. */
    public long deviation(CallDirection direction) {
        return direction.isArrival() ? arrivalDeviation() : departureDeviation();
    }

    /** Whether the chosen side of the call is late by the configured threshold. */
    public boolean isDelayed(CallDirection direction) {
        return deviation(direction) >= ModCommonConfig.SCHEDULE_DEVIATION_THRESHOLD.get();
    }

    /** The timetable time for the chosen side of the call. */
    public long scheduledTime(CallDirection direction) {
        return direction.isArrival() ? scheduled.arrival() : scheduled.departure();
    }

    /** The projected time for the chosen side of the call. */
    public long realtimeTime(CallDirection direction) {
        return direction.isArrival() ? realtime.arrival() : realtime.departure();
    }

    public boolean hasLine() {
        return line.isKnown();
    }

    public boolean hasCategory() {
        return category.isKnown();
    }

    /** The line on the chosen side of the call. */
    public LineRef line(CallDirection direction) {
        return direction.isArrival() ? arrivalLine : line;
    }

    /** The category on the chosen side of the call. */
    public CategoryRef category(CallDirection direction) {
        return direction.isArrival() ? arrivalCategory : category;
    }

    /** The name to show for the departing service. */
    public String displayName() {
        return displayName(CallDirection.DEPARTURE);
    }

    /**
     * The name to show on the chosen side of the call: the line name where it has one, otherwise
     * the train's own name.
     */
    public String displayName(CallDirection direction) {
        LineRef serving = line(direction);
        return serving.hasName() ? serving.name() : trainName;
    }

    /** The colour to show the departing service in. */
    public DLColor displayColor() {
        return displayColor(CallDirection.DEPARTURE);
    }

    /** The colour to show the chosen side of the call in, taken from its line or category. */
    public DLColor displayColor(CallDirection direction) {
        return ServiceColor.of(line(direction), category(direction));
    }

    /**
     * Whether the chosen side has a line or category at all, and hence a colour of its own rather
     * than the neutral default.
     */
    public boolean hasColor(CallDirection direction) {
        return line(direction).isKnown() || category(direction).isKnown();
    }

    /** Whether the train is calling at a different station than the timetable expected. */
    public boolean isDiverted() {
        return scheduledStation.isKnown() && station.isKnown()
            && !scheduledStation.name().equals(station.name());
    }

    /**
     * Whether the diversion is one a traveller would notice, meaning the displayed name changes
     * rather than only the underlying station within the same tag.
     */
    public boolean hasChangedTag() {
        return isDiverted() && !scheduledStation.displayName().equals(station.displayName());
    }

    /** Whether the train is standing at this station now. */
    public boolean isWaiting() {
        return visitState == StopVisitState.CURRENT;
    }

    /**
     * The next occurrence of this call at or after the given time, projected forward by as many
     * whole cycles as that requires. Returns this entry unchanged where no projection is needed or
     * possible.
     */
    public BoardEntry atOrAfter(long notBefore, long cycleDuration) {
        int cycles = CycleProjector.cyclesUntil(realtime, cycleDuration, notBefore);
        if (cycles <= 0) {
            return this;
        }
        return new BoardEntry(trainId, sessionId, trainName, carriageCount, line, category,
            arrivalLine, arrivalCategory, station, scheduledStation, origin, title, destination,
            CycleProjector.advancedBy(scheduled, cycleDuration, cycles),
            CycleProjector.advancedBy(realtime, cycleDuration, cycles),
            serviceState, visitState, entryIndex, sectionIndex, terminus, originating, stopovers, delays);
    }

    /**
     * Whether both entries describe the same call of the same train, disregarding times. Useful for
     * recognising an entry again after the board has been queried afresh.
     */
    public boolean isSameCall(BoardEntry other) {
        return other != null
            && entryIndex == other.entryIndex
            && java.util.Objects.equals(trainId, other.trainId)
            && java.util.Objects.equals(sessionId, other.sessionId);
    }

    /** Whether the train is out of service because of a disruption. */
    public boolean isCancelled() {
        return serviceState == ServiceState.DISRUPTED;
    }

    /** The projected arrival time. */
    public long realtimeArrival() {
        return realtime.arrival();
    }

    /** The projected departure time. */
    public long realtimeDeparture() {
        return realtime.departure();
    }

    /** How long until the train arrives, in ticks, given the current time. */
    public long arrivalIn(long now) {
        return realtime.arrivalIn(now);
    }

    /** How long until the train departs, in ticks, given the current time. */
    public long departureIn(long now) {
        return realtime.departureIn(now);
    }

    /** Whether any stations are served onward from here. */
    public boolean hasStopovers() {
        return !stopovers.isEmpty();
    }

    /** The most important reason for the train's state, where one is known. */
    public Optional<DelayInstance> primaryDelay() {
        return delays.isEmpty() ? Optional.empty() : Optional.of(delays.get(0));
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        NbtHelper.putNullableUUID(nbt, NBT_TRAIN_ID, trainId);
        NbtHelper.putNullableUUID(nbt, NBT_SESSION_ID, sessionId);
        nbt.putString(NBT_TRAIN_NAME, trainName);
        nbt.putInt(NBT_CARRIAGE_COUNT, carriageCount);
        nbt.put(NBT_LINE, line.toNbt());
        nbt.put(NBT_CATEGORY, category.toNbt());
        nbt.put(NBT_ARRIVAL_LINE, arrivalLine.toNbt());
        nbt.put(NBT_ARRIVAL_CATEGORY, arrivalCategory.toNbt());
        nbt.put(NBT_STATION, station.toNbt());
        nbt.put(NBT_SCHEDULED_STATION, scheduledStation.toNbt());
        nbt.put(NBT_ORIGIN, origin.toNbt());
        nbt.putString(NBT_TITLE, title);
        nbt.put(NBT_DESTINATION, destination.toNbt());
        nbt.put(NBT_SCHEDULED, scheduled.toNbt());
        nbt.put(NBT_REALTIME, realtime.toNbt());
        nbt.putString(NBT_SERVICE_STATE, serviceState.name());
        nbt.putString(NBT_VISIT_STATE, visitState.name());
        nbt.putInt(NBT_ENTRY_INDEX, entryIndex);
        nbt.putInt(NBT_SECTION_INDEX, sectionIndex);
        nbt.putBoolean(NBT_TERMINUS, terminus);
        nbt.putBoolean(NBT_ORIGINATING, originating);
        nbt.put(NBT_STOPOVERS, NbtHelper.writeList(stopovers, StationRef::toNbt));
        nbt.put(NBT_DELAYS, NbtHelper.writeList(delays, DelayInstance::toNbt));
        return nbt;
    }

    public static BoardEntry fromNbt(CompoundTag nbt) {
        return new BoardEntry(
            NbtHelper.readNullableUUID(nbt, NBT_TRAIN_ID),
            NbtHelper.readNullableUUID(nbt, NBT_SESSION_ID),
            nbt.getString(NBT_TRAIN_NAME),
            nbt.getInt(NBT_CARRIAGE_COUNT),
            LineRef.fromNbt(nbt.getCompound(NBT_LINE)),
            CategoryRef.fromNbt(nbt.getCompound(NBT_CATEGORY)),
            LineRef.fromNbt(nbt.getCompound(NBT_ARRIVAL_LINE)),
            CategoryRef.fromNbt(nbt.getCompound(NBT_ARRIVAL_CATEGORY)),
            StationRef.fromNbt(nbt.getCompound(NBT_STATION)),
            StationRef.fromNbt(nbt.getCompound(NBT_SCHEDULED_STATION)),
            StationRef.fromNbt(nbt.getCompound(NBT_ORIGIN)),
            nbt.getString(NBT_TITLE),
            StationRef.fromNbt(nbt.getCompound(NBT_DESTINATION)),
            StopTimes.fromNbt(nbt.getCompound(NBT_SCHEDULED)),
            StopTimes.fromNbt(nbt.getCompound(NBT_REALTIME)),
            NbtHelper.readEnum(nbt.getString(NBT_SERVICE_STATE), ServiceState.class, ServiceState.IN_SERVICE),
            NbtHelper.readEnum(nbt.getString(NBT_VISIT_STATE), StopVisitState.class, StopVisitState.UPCOMING),
            nbt.getInt(NBT_ENTRY_INDEX),
            nbt.getInt(NBT_SECTION_INDEX),
            nbt.getBoolean(NBT_TERMINUS),
            nbt.getBoolean(NBT_ORIGINATING),
            NbtHelper.readList(nbt, NBT_STOPOVERS, StationRef::fromNbt),
            NbtHelper.readList(nbt, NBT_DELAYS, DelayInstance::fromNbt)
        );
    }
}
