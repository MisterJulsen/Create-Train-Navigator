package de.mrjulsen.crn.backend.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import de.mrjulsen.crn.backend.core.LiveTrainState;
import de.mrjulsen.crn.backend.core.ServiceState;
import de.mrjulsen.crn.backend.core.TrackedTrain;
import de.mrjulsen.crn.backend.delay.DelayInstance;
import de.mrjulsen.crn.backend.schedule.JourneyDisplayNames;
import de.mrjulsen.crn.backend.schedule.JourneySection;
import de.mrjulsen.crn.backend.schedule.JourneyStop;
import de.mrjulsen.crn.backend.schedule.TrainJourney;
import de.mrjulsen.crn.backend.timing.CycleProjector;
import de.mrjulsen.crn.backend.timing.StopTimes;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.data.storage.GlobalSettings;
import de.mrjulsen.crn.util.NbtHelper;
import de.mrjulsen.mcdragonlib.util.DLColor;
import net.minecraft.nbt.CompoundTag;

/**
 * One row of a departure or arrival board: a specific train calling at a specific station, with
 * everything needed to render that row without further queries - including the platform, which the
 * {@linkplain StationRef#info() station's info} carries.
 * <p>
 * Only what the backend measures is stored. Deviations and lateness follow from the scheduled and
 * projected times and are offered as methods rather than fields.
 *
 * @param trainId       The id of the calling train.
 * @param sessionId     The train's tracking session. See {@link TrainSnapshot#sessionId()}.
 * @param trainName     The train's own name. What to actually show is {@link #displayName()}.
 * @param carriageCount How many carriages the calling train has.
 * @param line          The train line the call <em>departs</em> as, or {@link LineRef#NONE}.
 * @param category      The train category the call departs as, or {@link CategoryRef#NONE}.
 * @param arrivalLine   The train line the call <em>arrives</em> as. A train handing over to another
 *                      service at this station arrives as one line and leaves as the next, so a board
 *                      showing the arrival must name a different line than one showing the departure.
 *                      Equal to {@link #line()} wherever no handover happens.
 * @param arrivalCategory The train category the call arrives as.
 * @param station       The station this row belongs to, as the train is actually expected at it.
 * @param scheduledStation The station the timetable plans for. Differs from {@link #station()} when a
 *                      wildcard filter sends the train elsewhere - {@link #isDiverted()} answers it.
 * @param origin        Where the service calling here started, which is not the journey's first stop
 *                      whenever an earlier section handed its passengers over.
 * @param title         The schedule title the train carries towards this call. May be empty.
 * @param destination   The terminus advertised for this call. A train carrying a schedule title
 *                      advertises that instead - see {@link #destinationText()}.
 * @param scheduled     The timetable times of this call.
 * @param realtime      The projected times of this call.
 * @param serviceState  Whether the calling train can run at all. {@link #isCancelled()} answers the
 *                      common question about it.
 * @param visitState    Whether the train is standing here, still on its way, or already gone.
 * @param entryIndex    The schedule entry index of this call, identifying it within the journey.
 * @param sectionIndex  The position of the section this call belongs to, or {@code -1}.
 * @param terminus      Whether everybody has to get out here, i.e. whether the train carries nobody
 *                      beyond this call. See {@link de.mrjulsen.crn.backend.schedule.TrainJourney#isTerminus}.
 * @param originating   Whether the service starts here, i.e. whether nobody can already be aboard.
 * @param stopovers     The stations served after this call within the same section.
 * @param delays        The status reasons currently applying to the train, most important first.
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

    /** Captures the given call of the given train as a board row. */
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

    /**
     * The section whose service brings the train in, which is the one before it wherever that section
     * handed its passengers over - the point at which one line becomes another without anybody having
     * to get out. A stop of a section nobody may travel in belongs to the service that advertised it.
     */
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

    /**
     * Whether the train is standing at this call or still on its way to it.
     * <p>
     * A board only ever lists calls still to come, so there is no {@link StopVisitState#PASSED} to
     * report here - and guessing at one from the stop order would get a cyclic journey wrong, where a
     * call behind the train is the next cycle's rather than a call already made.
     */
    private static StopVisitState visitStateOf(TrackedTrain train, JourneyStop stop) {
        boolean here = train.getCurrentStop().map(x -> x.getOrderIndex() == stop.getOrderIndex()).orElse(false);
        return here && train.getLiveState() == LiveTrainState.AT_STATION
            ? StopVisitState.CURRENT
            : StopVisitState.UPCOMING;
    }

    /** The terminus of this call's section, falling back to the train's next call. */
    private static StationRef resolveDestination(TrackedTrain train, JourneyStop stop, StopSnapshot snapshot) {
        String sectionDestination = train.getSectionDestination(stop);
        if (!sectionDestination.isBlank()) {
            return StationRef.of(sectionDestination);
        }
        return train.getJourney().getNextStop(stop)
            .map(x -> StationRef.of(train.getDisplayStationName(x)))
            .orElse(snapshot.realtimeStation());
    }

    /**
     * The stations served on the way from this call to the terminus, blacklisted ones omitted.
     * <p>
     * The terminus itself is left out: it is what {@link #destinationText()} advertises, and a board
     * that named it here as well would list it twice. Where the section carries its start over into
     * the next one, the terminus is that next section's first stop and so is not among these stops
     * to begin with, which is why the whole section is listed in that case.
     */
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
     * What to advertise as this call's destination: an explicit schedule title takes precedence over
     * the terminus, which is why this may name no station at all.
     * <p>
     * The terminus is named the way a traveller knows it - by its station tag - and only falls back
     * to the raw track station name where no tag covers it.
     */
    public String destinationText() {
        return title.isBlank() ? destination.displayName() : title;
    }

    /** The station's name, as a shorthand for {@code station().name()}. */
    public String stationName() {
        return station.name();
    }

    /** How much later than the timetable the train gets here, in ticks. Negative when it is early. */
    public long arrivalDeviation() {
        return scheduled.isKnown() && realtime.isKnown() ? realtime.arrival() - scheduled.arrival() : 0;
    }

    /** How much later than the timetable the train leaves here, in ticks. */
    public long departureDeviation() {
        return scheduled.isKnown() && realtime.isKnown() ? realtime.departure() - scheduled.departure() : 0;
    }

    /** Whether either deviation reaches the given threshold in ticks. */
    public boolean isDelayed(long thresholdTicks) {
        return arrivalDeviation() >= thresholdTicks || departureDeviation() >= thresholdTicks;
    }

    /** Whether either deviation reaches the configured threshold. */
    public boolean isDelayed() {
        return isDelayed(ModCommonConfig.SCHEDULE_DEVIATION_THRESHOLD.get());
    }

    /**
     * How late this call is in the sense the given half of it is shown: an arrival is late when the
     * train gets in late, a departure when it leaves late. A train that arrives late and makes the
     * time back up while it stands here is late as an arrival and on time as a departure.
     */
    public long deviation(CallDirection direction) {
        return direction.isArrival() ? arrivalDeviation() : departureDeviation();
    }

    /** Whether the given half of this call is late by the configured threshold. */
    public boolean isDelayed(CallDirection direction) {
        return deviation(direction) >= ModCommonConfig.SCHEDULE_DEVIATION_THRESHOLD.get();
    }

    /** The timetable time of the given half of this call. */
    public long scheduledTime(CallDirection direction) {
        return direction.isArrival() ? scheduled.arrival() : scheduled.departure();
    }

    /** The projected time of the given half of this call. */
    public long realtimeTime(CallDirection direction) {
        return direction.isArrival() ? realtime.arrival() : realtime.departure();
    }

    /** Whether this call is served by a train line at all. */
    public boolean hasLine() {
        return line.isKnown();
    }

    /** Whether this call carries a train category at all. */
    public boolean hasCategory() {
        return category.isKnown();
    }

    /** The train line running the given half of this call. */
    public LineRef line(CallDirection direction) {
        return direction.isArrival() ? arrivalLine : line;
    }

    /** The train category of the given half of this call. */
    public CategoryRef category(CallDirection direction) {
        return direction.isArrival() ? arrivalCategory : category;
    }

    /**
     * What to show as the operator of this call: the name of the line serving it, or the train's own
     * name if it carries no line or the line is unnamed.
     * <p>
     * Derived from this call's own line rather than from wherever the train happens to be, so a row
     * for a later section of the journey is labelled with the line that will actually run it.
     */
    public String displayName() {
        return displayName(CallDirection.DEPARTURE);
    }

    /**
     * What to show as the operator of the given half of this call. A train handing over to another
     * service here arrives under one name and leaves under the next.
     */
    public String displayName(CallDirection direction) {
        LineRef serving = line(direction);
        return serving.hasName() ? serving.name() : trainName;
    }

    /**
     * What to paint this row in: the colour of the line serving it, the colour of its category if
     * the line carries none, and a neutral default if neither does.
     */
    public DLColor displayColor() {
        return displayColor(CallDirection.DEPARTURE);
    }

    /** What to paint the given half of this call in. */
    public DLColor displayColor(CallDirection direction) {
        return ServiceColor.of(line(direction), category(direction));
    }

    /** Whether the given half of this call carries a colour of its own at all. */
    public boolean hasColor(CallDirection direction) {
        return line(direction).isKnown() || category(direction).isKnown();
    }

    /**
     * Whether the train is heading somewhere other than the timetable plans, which happens when a
     * wildcard filter resolves to a different station - another platform, or another station entirely.
     */
    public boolean isDiverted() {
        return scheduledStation.isKnown() && station.isKnown()
            && !scheduledStation.name().equals(station.name());
    }

    /** Whether the station actually served sits in a different tag than the one planned for. */
    public boolean hasChangedTag() {
        return isDiverted() && !scheduledStation.displayName().equals(station.displayName());
    }

    /** Whether the train is standing at this station right now. */
    public boolean isWaiting() {
        return visitState == StopVisitState.CURRENT;
    }

    /**
     * This call as it recurs at or after the given time, i.e. the next occurrence a traveller arriving
     * then could still catch.
     * <p>
     * A board asked for departures from half an hour onwards is not asking which trains have already
     * gone - it is asking what will be leaving then, and on a cyclic line that is the same train on a
     * later lap. Returns this row unchanged where there is no later lap to project into.
     *
     * @see de.mrjulsen.crn.backend.timing.CycleProjector
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
     * Whether this row describes the same call as another, however far the times have moved since.
     * <p>
     * A board is rebuilt from scratch every refresh, so equality would say "different" every time the
     * projection shifts by a tick. What a display needs to know is something narrower: whether the
     * rows have changed identity or merely their contents, because only the former means the layout
     * has to be built again.
     */
    public boolean isSameCall(BoardEntry other) {
        return other != null
            && entryIndex == other.entryIndex
            && java.util.Objects.equals(trainId, other.trainId)
            && java.util.Objects.equals(sessionId, other.sessionId);
    }

    /** Whether the calling train is out of service because of a disruption. */
    public boolean isCancelled() {
        return serviceState == ServiceState.DISRUPTED;
    }

    /** The projected arrival time in transformed game ticks. */
    public long realtimeArrival() {
        return realtime.arrival();
    }

    /** The projected departure time in transformed game ticks. */
    public long realtimeDeparture() {
        return realtime.departure();
    }

    /** Ticks until the train arrives. Negative once the arrival time has passed. */
    public long arrivalIn(long now) {
        return realtime.arrivalIn(now);
    }

    /** Ticks until the train departs. Negative once the departure time has passed. */
    public long departureIn(long now) {
        return realtime.departureIn(now);
    }

    /** Whether this call serves any further stations within its section. */
    public boolean hasStopovers() {
        return !stopovers.isEmpty();
    }

    /** The most important status reason currently applying, if there is any. */
    public Optional<DelayInstance> primaryDelay() {
        return delays.isEmpty() ? Optional.empty() : Optional.of(delays.get(0));
    }

    /** Serializes this board row. */
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

    /** Deserializes a board row written by {@link #toNbt()}. */
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
