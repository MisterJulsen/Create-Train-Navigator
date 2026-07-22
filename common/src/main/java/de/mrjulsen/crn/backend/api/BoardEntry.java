package de.mrjulsen.crn.backend.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import de.mrjulsen.crn.backend.core.ServiceState;
import de.mrjulsen.crn.backend.core.TrackedTrain;
import de.mrjulsen.crn.backend.delay.DelayInstance;
import de.mrjulsen.crn.backend.schedule.JourneySection;
import de.mrjulsen.crn.backend.schedule.JourneyStop;
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
 * @param trainId      The id of the calling train.
 * @param sessionId    The train's tracking session. See {@link TrainSnapshot#sessionId()}.
 * @param trainName    The train's own name. What to actually show is {@link #displayName()}.
 * @param line         The train line serving this call, or {@link LineRef#NONE} if it carries none.
 * @param category     The train category of this call, or {@link CategoryRef#NONE}.
 * @param station      The station this row belongs to.
 * @param title        The schedule title the train carries towards this call. May be empty.
 * @param destination  The terminus advertised for this call. A train carrying a schedule title
 *                     advertises that instead - see {@link #destinationText()}.
 * @param scheduled    The timetable times of this call.
 * @param realtime     The projected times of this call.
 * @param serviceState Whether the calling train can run at all. {@link #isCancelled()} answers the
 *                     common question about it.
 * @param entryIndex   The schedule entry index of this call, identifying it within the journey.
 * @param sectionIndex The position of the section this call belongs to, or {@code -1}.
 * @param stopovers    The stations served after this call within the same section.
 * @param delays       The status reasons currently applying to the train, most important first.
 */
public record BoardEntry(
    UUID trainId,
    UUID sessionId,
    String trainName,
    LineRef line,
    CategoryRef category,
    StationRef station,
    String title,
    StationRef destination,
    StopTimes scheduled,
    StopTimes realtime,
    ServiceState serviceState,
    int entryIndex,
    int sectionIndex,
    List<StationRef> stopovers,
    List<DelayInstance> delays
) {

    private static final String NBT_TRAIN_ID = "TrainId";
    private static final String NBT_SESSION_ID = "SessionId";
    private static final String NBT_TRAIN_NAME = "TrainName";
    private static final String NBT_LINE = "Line";
    private static final String NBT_CATEGORY = "Category";
    private static final String NBT_STATION = "Station";
    private static final String NBT_TITLE = "Title";
    private static final String NBT_DESTINATION = "Destination";
    private static final String NBT_SCHEDULED = "Scheduled";
    private static final String NBT_REALTIME = "Realtime";
    private static final String NBT_SERVICE_STATE = "ServiceState";
    private static final String NBT_ENTRY_INDEX = "EntryIndex";
    private static final String NBT_SECTION_INDEX = "SectionIndex";
    private static final String NBT_STOPOVERS = "Stopovers";
    private static final String NBT_DELAYS = "Delays";

    public BoardEntry {
        stopovers = stopovers == null ? List.of() : List.copyOf(stopovers);
        delays = delays == null ? List.of() : List.copyOf(delays);
        trainName = trainName == null ? "" : trainName;
        line = line == null ? LineRef.NONE : line;
        category = category == null ? CategoryRef.NONE : category;
        station = station == null ? StationRef.NONE : station;
        title = title == null ? "" : title;
        destination = destination == null ? StationRef.NONE : destination;
        scheduled = scheduled == null ? StopTimes.UNKNOWN : scheduled;
        realtime = realtime == null ? StopTimes.UNKNOWN : realtime;
        serviceState = serviceState == null ? ServiceState.IN_SERVICE : serviceState;
    }

    /** Captures the given call of the given train as a board row. */
    public static BoardEntry of(TrackedTrain train, JourneyStop stop) {
        StopSnapshot snapshot = StopSnapshot.of(train, stop);
        JourneySection section = stop.getSection();

        return new BoardEntry(
            train.getTrainId(),
            train.getSessionId(),
            train.getTrainName(),
            LineRef.of(section == null ? null : section.getTrainLine().orElse(null)),
            CategoryRef.of(section == null ? null : section.getTrainCategory().orElse(null)),
            snapshot.realtimeStation(),
            stop.getTitle() == null ? "" : stop.getTitle(),
            resolveDestination(train, stop, snapshot),
            snapshot.scheduled(),
            snapshot.realtime(),
            train.getServiceState(),
            stop.entryIndex(),
            snapshot.sectionIndex(),
            collectStopovers(train, stop, section),
            train.getActiveDelays()
        );
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

    /** The stations served after this call within the same section, blacklisted ones omitted. */
    private static List<StationRef> collectStopovers(TrackedTrain train, JourneyStop stop, JourneySection section) {
        if (section == null) {
            return List.of();
        }
        List<StationRef> stopovers = new ArrayList<>();
        boolean reached = false;
        for (JourneyStop other : section.getStops()) {
            if (other == stop) {
                reached = true;
                continue;
            }
            if (!reached || GlobalSettings.getInstance().isStationBlacklisted(other.getStationName())) {
                continue;
            }
            stopovers.add(StationRef.of(train.getDisplayStationName(other)));
        }
        return stopovers;
    }

    /**
     * What to advertise as this call's destination: an explicit schedule title takes precedence over
     * the terminus, which is why this may name no station at all.
     */
    public String destinationText() {
        return title.isBlank() ? destination.name() : title;
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

    /** Whether this call is served by a train line at all. */
    public boolean hasLine() {
        return line.isKnown();
    }

    /** Whether this call carries a train category at all. */
    public boolean hasCategory() {
        return category.isKnown();
    }

    /**
     * What to show as the operator of this call: the name of the line serving it, or the train's own
     * name if it carries no line or the line is unnamed.
     * <p>
     * Derived from this call's own line rather than from wherever the train happens to be, so a row
     * for a later section of the journey is labelled with the line that will actually run it.
     */
    public String displayName() {
        return line.hasName() ? line.name() : trainName;
    }

    /**
     * What to paint this row in: the colour of the line serving it, the colour of its category if
     * the line carries none, and a neutral default if neither does.
     */
    public DLColor displayColor() {
        return ServiceColor.of(line, category);
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
        nbt.put(NBT_LINE, line.toNbt());
        nbt.put(NBT_CATEGORY, category.toNbt());
        nbt.put(NBT_STATION, station.toNbt());
        nbt.putString(NBT_TITLE, title);
        nbt.put(NBT_DESTINATION, destination.toNbt());
        nbt.put(NBT_SCHEDULED, scheduled.toNbt());
        nbt.put(NBT_REALTIME, realtime.toNbt());
        nbt.putString(NBT_SERVICE_STATE, serviceState.name());
        nbt.putInt(NBT_ENTRY_INDEX, entryIndex);
        nbt.putInt(NBT_SECTION_INDEX, sectionIndex);
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
            LineRef.fromNbt(nbt.getCompound(NBT_LINE)),
            CategoryRef.fromNbt(nbt.getCompound(NBT_CATEGORY)),
            StationRef.fromNbt(nbt.getCompound(NBT_STATION)),
            nbt.getString(NBT_TITLE),
            StationRef.fromNbt(nbt.getCompound(NBT_DESTINATION)),
            StopTimes.fromNbt(nbt.getCompound(NBT_SCHEDULED)),
            StopTimes.fromNbt(nbt.getCompound(NBT_REALTIME)),
            NbtHelper.readEnum(nbt.getString(NBT_SERVICE_STATE), ServiceState.class, ServiceState.IN_SERVICE),
            nbt.getInt(NBT_ENTRY_INDEX),
            nbt.getInt(NBT_SECTION_INDEX),
            NbtHelper.readList(nbt, NBT_STOPOVERS, StationRef::fromNbt),
            NbtHelper.readList(nbt, NBT_DELAYS, DelayInstance::fromNbt)
        );
    }
}
