package de.mrjulsen.crn.api.core.snapshot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.crn.api.core.ref.CategoryRef;
import de.mrjulsen.crn.api.core.ref.LineRef;
import de.mrjulsen.crn.api.core.RailwayBackendApi;
import de.mrjulsen.crn.api.core.ref.StationRef;
import de.mrjulsen.crn.core.train.LiveTrainState;
import de.mrjulsen.crn.core.train.ServiceState;
import de.mrjulsen.crn.core.train.TrackedTrain;
import de.mrjulsen.crn.core.train.TrainLifecycleState;
import de.mrjulsen.crn.core.delay.DelayInstance;
import de.mrjulsen.crn.core.schedule.JourneySection;
import de.mrjulsen.crn.core.schedule.JourneyStop;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.util.NbtHelper;
import de.mrjulsen.crn.web.annotation.RestAlwaysInclude;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * A train's overall state: what it is, where it is, where it is going and whether it is running to
 * time. Enough for a list, a status panel or a map marker; for the run stop by stop use
 * {@link JourneySnapshot}.
 * <p>
 * The line and category are those of the section the train is working <em>now</em>, so they can
 * change during a run. Times are in the unit described by {@link RailwayBackendApi#getCurrentTime()}.
 *
 * @param trainId       The train's id, stable for as long as the train exists.
 * @param sessionId     Changes whenever tracking of this train restarts, so a consumer can tell
 *                      that earlier data no longer relates to the current run.
 * @param trainName     The train's own name, as opposed to its line name.
 * @param ownerId       The player who owns the train, or {@code null}.
 * @param iconId        The train's icon, or {@code null} if it has none.
 * @param mapColorIndex The train's colour index as used on maps.
 * @param line          The line of the current section, or {@link LineRef#NONE}.
 * @param category      The category of the current section, or {@link CategoryRef#NONE}.
 * @param lifecycle     How far along the backend is in learning this train, and hence how far its
 *                      times can be trusted.
 * @param liveState     What the train is doing at this moment.
 * @param serviceState  Whether the train is able to run at all.
 * @param maxDeviation  How far behind its timetable the train is running, in ticks, before any
 *                      allowance is applied. Negative when it is early.
 * @param delayOffset   The part of the deviation that is deliberately not counted as delay.
 * @param totalDuration How long one full run of the schedule takes, or a negative value if that is
 *                      not yet known.
 * @param currentTitle  The schedule title in force at the current stop, or empty.
 * @param destination   Where the train is bound for within its current section.
 * @param currentStation The station the train is standing at, or {@link StationRef#NONE}.
 * @param nextStation   The next station the train will call at, or {@link StationRef#NONE}.
 * @param sectionIndex  The position of the current section within the run, or {@code -1} if none
 *                      is known.
 * @param stopCount     How many stops the whole run has.
 * @param carriageCount How many carriages the train has.
 * @param position      Where the train is and how fast it is going.
 * @param delays        Why the train is late or disrupted, most important first.
 */
public record TrainSnapshot(
    @RestAlwaysInclude UUID trainId,
    @RestAlwaysInclude UUID sessionId,
    String trainName,
    UUID ownerId,
    ResourceLocation iconId,
    int mapColorIndex,
    LineRef line,
    CategoryRef category,
    TrainLifecycleState lifecycle,
    LiveTrainState liveState,
    ServiceState serviceState,
    long maxDeviation,
    long delayOffset,
    long totalDuration,
    String currentTitle,
    StationRef destination,
    StationRef currentStation,
    StationRef nextStation,
    int sectionIndex,
    int stopCount,
    int carriageCount,
    TrainPositionSnapshot position,
    List<DelayInstance> delays
) {

    private static final String NBT_TRAIN_ID = "TrainId";
    private static final String NBT_SESSION_ID = "SessionId";
    private static final String NBT_TRAIN_NAME = "TrainName";
    private static final String NBT_OWNER_ID = "OwnerId";
    private static final String NBT_ICON_ID = "IconId";
    private static final String NBT_MAP_COLOR = "MapColorIndex";
    private static final String NBT_LINE = "Line";
    private static final String NBT_CATEGORY = "Category";
    private static final String NBT_LIFECYCLE = "Lifecycle";
    private static final String NBT_LIVE_STATE = "LiveState";
    private static final String NBT_SERVICE_STATE = "ServiceState";
    private static final String NBT_MAX_DEVIATION = "MaxDeviation";
    private static final String NBT_DELAY_OFFSET = "DelayOffset";
    private static final String NBT_TOTAL_DURATION = "TotalDuration";
    private static final String NBT_CURRENT_TITLE = "CurrentTitle";
    private static final String NBT_DESTINATION = "Destination";
    private static final String NBT_CURRENT_STATION = "CurrentStation";
    private static final String NBT_NEXT_STATION = "NextStation";
    private static final String NBT_SECTION_INDEX = "SectionIndex";
    private static final String NBT_STOP_COUNT = "StopCount";
    private static final String NBT_CARRIAGE_COUNT = "CarriageCount";
    private static final String NBT_POSITION = "Position";
    private static final String NBT_DELAYS = "Delays";

    public TrainSnapshot {
        delays = delays == null ? List.of() : List.copyOf(delays);
        trainName = trainName == null ? "" : trainName;
        line = line == null ? LineRef.NONE : line;
        category = category == null ? CategoryRef.NONE : category;
        currentTitle = currentTitle == null ? "" : currentTitle;
        destination = destination == null ? StationRef.NONE : destination;
        currentStation = currentStation == null ? StationRef.NONE : currentStation;
        nextStation = nextStation == null ? StationRef.NONE : nextStation;
    }

    public static TrainSnapshot of(TrackedTrain tracked) {
        Train train = tracked.getTrain();
        JourneySection section = tracked.getCurrentSection().orElse(null);
        JourneyStop currentStop = tracked.getCurrentStop().orElse(null);

        StationRef nextStation = currentStop == null ? StationRef.NONE : tracked.getJourney().getNextStop(currentStop)
            .map(x -> StationRef.of(tracked.getDisplayStationName(x)))
            .orElse(StationRef.NONE);

        return new TrainSnapshot(
            tracked.getTrainId(),
            tracked.getSessionId(),
            tracked.getTrainName(),
            train.owner,
            train.icon == null ? null : train.icon.getId(),
            train.mapColorIndex,
            LineRef.of(section == null ? null : section.getTrainLine().orElse(null)),
            CategoryRef.of(section == null ? null : section.getTrainCategory().orElse(null)),
            tracked.getLifecycleState(),
            tracked.getLiveState(),
            tracked.getServiceState(),
            tracked.getMaxDeviation(),
            tracked.getDelayOffset(),
            tracked.getTotalDuration(),
            currentStop == null || currentStop.getTitle() == null ? "" : currentStop.getTitle(),
            resolveDestination(tracked, currentStop, nextStation),
            currentStop == null ? StationRef.NONE : StationRef.of(tracked.getDisplayStationName(currentStop)),
            nextStation,
            section == null ? -1 : section.getSectionIndex(),
            tracked.getJourney().getStopCount(),
            train.carriages == null ? 0 : train.carriages.size(),
            TrainPositionSnapshot.of(train, tracked.getExitSide()),
            tracked.getActiveDelays()
        );
    }

    private static StationRef resolveDestination(TrackedTrain tracked, JourneyStop currentStop, StationRef nextStation) {
        if (currentStop == null) {
            return StationRef.NONE;
        }
        String sectionDestination = tracked.getSectionDestination(currentStop);
        return sectionDestination.isBlank() ? nextStation : StationRef.of(sectionDestination);
    }

    /**
     * The destination to show for this train: the schedule title where one is set, otherwise the
     * destination station's display name.
     */
    public String destinationText() {
        return currentTitle.isBlank() ? destination.displayName() : currentTitle;
    }

    /** The name to show for this train: its line name where it has one, otherwise its own name. */
    public String displayName() {
        return line.nameOr(trainName);
    }

    /** Whether the train's delay reaches the given number of ticks. */
    public boolean isDelayed(long thresholdTicks) {
        return currentDelay() >= thresholdTicks;
    }

    /** Whether the train counts as late by the server's configured threshold. */
    public boolean isDelayed() {
        return isDelayed(ModCommonConfig.SCHEDULE_DEVIATION_THRESHOLD.get());
    }

    /** Whether the train is out of service because of a disruption. */
    public boolean isCancelled() {
        return serviceState == ServiceState.DISRUPTED;
    }

    /** Whether the backend has learned enough about this train for its times to be dependable. */
    public boolean isUsable() {
        return lifecycle.isUsable();
    }

    /** Whether the train is working its schedule, as opposed to standing idle or having none. */
    public boolean isOperating() {
        return liveState.isOperating();
    }

    /**
     * How late the train is, in ticks, after the allowance is applied. Never negative: a train
     * running early reports no delay.
     */
    public long currentDelay() {
        return Math.max(0, maxDeviation - delayOffset);
    }

    public boolean hasLine() {
        return line.isKnown();
    }

    public boolean hasCategory() {
        return category.isKnown();
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
        NbtHelper.putNullableUUID(nbt, NBT_OWNER_ID, ownerId);
        if (iconId != null) {
            nbt.putString(NBT_ICON_ID, iconId.toString());
        }
        nbt.putInt(NBT_MAP_COLOR, mapColorIndex);
        nbt.put(NBT_LINE, line.toNbt());
        nbt.put(NBT_CATEGORY, category.toNbt());
        nbt.putString(NBT_LIFECYCLE, lifecycle.name());
        nbt.putString(NBT_LIVE_STATE, liveState.name());
        nbt.putString(NBT_SERVICE_STATE, serviceState.name());
        nbt.putLong(NBT_MAX_DEVIATION, maxDeviation);
        nbt.putLong(NBT_DELAY_OFFSET, delayOffset);
        nbt.putLong(NBT_TOTAL_DURATION, totalDuration);
        nbt.putString(NBT_CURRENT_TITLE, currentTitle);
        nbt.put(NBT_DESTINATION, destination.toNbt());
        nbt.put(NBT_CURRENT_STATION, currentStation.toNbt());
        nbt.put(NBT_NEXT_STATION, nextStation.toNbt());
        nbt.putInt(NBT_SECTION_INDEX, sectionIndex);
        nbt.putInt(NBT_STOP_COUNT, stopCount);
        nbt.putInt(NBT_CARRIAGE_COUNT, carriageCount);
        nbt.put(NBT_POSITION, position.toNbt());
        nbt.put(NBT_DELAYS, NbtHelper.writeList(delays, DelayInstance::toNbt));
        return nbt;
    }

    public static TrainSnapshot fromNbt(CompoundTag nbt) {
        return new TrainSnapshot(
            NbtHelper.readNullableUUID(nbt, NBT_TRAIN_ID),
            NbtHelper.readNullableUUID(nbt, NBT_SESSION_ID),
            nbt.getString(NBT_TRAIN_NAME),
            NbtHelper.readNullableUUID(nbt, NBT_OWNER_ID),
            nbt.contains(NBT_ICON_ID) ? new ResourceLocation(nbt.getString(NBT_ICON_ID)) : null,
            nbt.getInt(NBT_MAP_COLOR),
            LineRef.fromNbt(nbt.getCompound(NBT_LINE)),
            CategoryRef.fromNbt(nbt.getCompound(NBT_CATEGORY)),
            NbtHelper.readEnum(nbt.getString(NBT_LIFECYCLE), TrainLifecycleState.class, TrainLifecycleState.PREPARING),
            NbtHelper.readEnum(nbt.getString(NBT_LIVE_STATE), LiveTrainState.class, LiveTrainState.NO_SCHEDULE),
            NbtHelper.readEnum(nbt.getString(NBT_SERVICE_STATE), ServiceState.class, ServiceState.IN_SERVICE),
            nbt.getLong(NBT_MAX_DEVIATION),
            nbt.getLong(NBT_DELAY_OFFSET),
            nbt.getLong(NBT_TOTAL_DURATION),
            nbt.getString(NBT_CURRENT_TITLE),
            StationRef.fromNbt(nbt.getCompound(NBT_DESTINATION)),
            StationRef.fromNbt(nbt.getCompound(NBT_CURRENT_STATION)),
            StationRef.fromNbt(nbt.getCompound(NBT_NEXT_STATION)),
            nbt.getInt(NBT_SECTION_INDEX),
            nbt.getInt(NBT_STOP_COUNT),
            nbt.getInt(NBT_CARRIAGE_COUNT),
            TrainPositionSnapshot.fromNbt(nbt.getCompound(NBT_POSITION)),
            NbtHelper.readList(nbt, NBT_DELAYS, DelayInstance::fromNbt)
        );
    }
}
