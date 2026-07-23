package de.mrjulsen.crn.backend.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.crn.backend.core.LiveTrainState;
import de.mrjulsen.crn.backend.core.ServiceState;
import de.mrjulsen.crn.backend.core.TrackedTrain;
import de.mrjulsen.crn.backend.core.TrainLifecycleState;
import de.mrjulsen.crn.backend.delay.DelayInstance;
import de.mrjulsen.crn.backend.schedule.JourneySection;
import de.mrjulsen.crn.backend.schedule.JourneyStop;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.util.NbtHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * An immutable snapshot of a train's overall state: who it is, where it is going, how it is doing
 * and why it is late.
 * <p>
 * This is the overview record - enough to fill a train list, a status panel or a map marker without
 * further queries. The full stop-by-stop run is a {@link JourneySnapshot}, the physical make-up a
 * {@link TrainCompositionSnapshot}, and the delay situation in detail a {@link DelayReport}.
 *
 * @param trainId       The id of the train.
 * @param sessionId     Changes whenever tracking of this train restarts, e.g. after it was out of
 *                      service or received a new schedule. Consumers should not relate data across
 *                      a change of this value.
 * @param trainName     The train's own name. What to actually show is {@link #displayName()}.
 * @param ownerId       The id of the player who owns the train, or {@code null}.
 * @param iconId        The id of the train's icon.
 * @param mapColorIndex The train's colour index on a map.
 * @param line          The current section's train line, or {@link LineRef#NONE}.
 * @param category      The current section's train category, or {@link CategoryRef#NONE}.
 * @param lifecycle     How reliable this train's data currently is.
 * @param liveState     What the train is doing right now.
 * @param serviceState  Whether the train can run at all. {@link #isCancelled()} answers the common
 *                      question about it.
 * @param maxDeviation  The highest deviation from the timetable across the remaining stops, in ticks.
 * @param delayOffset   Deviation carried over from a previous section, in ticks.
 * @param totalDuration How long one full journey cycle takes in ticks, or {@code -1} while learning.
 * @param currentTitle  The schedule title the train currently carries. May be empty.
 * @param destination   The terminus this train is heading for. Note that a train carrying a schedule
 *                      title advertises that title instead - see {@link #destinationText()}.
 * @param currentStation The station the train is at or traveling towards.
 * @param nextStation   The station after the current one, or {@link StationRef#NONE}.
 * @param sectionIndex  The position of the current section in travel order, or {@code -1}.
 * @param stopCount     How many stops the train's journey has.
 * @param carriageCount How many carriages the train has.
 * @param position      Where the train is and how fast it is moving.
 * @param delays        The status reasons currently applying, most important first.
 */
public record TrainSnapshot(
    UUID trainId,
    UUID sessionId,
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

    /** Captures the current state of the given train. */
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

    /** The terminus of the current section, falling back to the train's next call. */
    private static StationRef resolveDestination(TrackedTrain tracked, JourneyStop currentStop, StationRef nextStation) {
        if (currentStop == null) {
            return StationRef.NONE;
        }
        String sectionDestination = tracked.getSectionDestination(currentStop);
        return sectionDestination.isBlank() ? nextStation : StationRef.of(sectionDestination);
    }

    /**
     * What to advertise as this train's destination: an explicit schedule title takes precedence
     * over the terminus, which is why this may name no station at all.
     * <p>
     * The terminus is named the way a traveller knows it - by its station tag - and only falls back
     * to the raw track station name where no tag covers it.
     */
    public String destinationText() {
        return currentTitle.isBlank() ? destination.displayName() : currentTitle;
    }

    /**
     * What to show as this train's name: the name of the line it currently runs on, or its own name
     * if it carries no line or the line is unnamed.
     */
    public String displayName() {
        return line.hasName() ? line.name() : trainName;
    }

    /** Whether the train is late beyond the given threshold in ticks. */
    public boolean isDelayed(long thresholdTicks) {
        return currentDelay() >= thresholdTicks;
    }

    /** Whether the train is late beyond the configured threshold. */
    public boolean isDelayed() {
        return isDelayed(ModCommonConfig.SCHEDULE_DEVIATION_THRESHOLD.get());
    }

    /** Whether the train is out of service because of a disruption. */
    public boolean isCancelled() {
        return serviceState == ServiceState.DISRUPTED;
    }

    /** Whether the train's times are reliable enough to publish. */
    public boolean isUsable() {
        return lifecycle.isUsable();
    }

    /** Whether the train is running its schedule right now. */
    public boolean isOperating() {
        return liveState.isOperating();
    }

    /** The delay in ticks, i.e. the deviation not carried over from a previous section. */
    public long currentDelay() {
        return Math.max(0, maxDeviation - delayOffset);
    }

    /** Whether this train carries a train line at all. */
    public boolean hasLine() {
        return line.isKnown();
    }

    /** Whether this train carries a train category at all. */
    public boolean hasCategory() {
        return category.isKnown();
    }

    /** The most important status reason currently applying, if there is any. */
    public Optional<DelayInstance> primaryDelay() {
        return delays.isEmpty() ? Optional.empty() : Optional.of(delays.get(0));
    }

    /** Serializes this train. */
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

    /** Deserializes a train written by {@link #toNbt()}. */
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
