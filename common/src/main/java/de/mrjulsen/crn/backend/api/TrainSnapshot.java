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
import de.mrjulsen.crn.data.TrainCategory;
import de.mrjulsen.crn.data.TrainLine;
import net.minecraft.resources.ResourceLocation;

/**
 * An immutable snapshot of a train's overall state: who it is, where it is going, how it is doing
 * and why it is late.
 * <p>
 * This is the overview record - enough to fill a train list, a status panel or a map marker without
 * further queries. The full stop-by-stop run is a {@link JourneySnapshot}, the physical make-up a
 * {@link TrainCompositionSnapshot}, and the delay situation in detail a {@link DelayReport}.
 * <p>
 * The train line and category are the live configured objects rather than copies, so they follow
 * later edits to their name or colour instead of going stale.
 *
 * @param trainId       The id of the train.
 * @param sessionId     Changes whenever tracking of this train restarts, e.g. after it was out of
 *                      service or received a new schedule. Consumers should not relate data across
 *                      a change of this value.
 * @param trainName     The train's own name. What to actually show is {@link #displayName()}.
 * @param ownerId       The id of the player who owns the train, or {@code null}.
 * @param iconId        The id of the train's icon.
 * @param mapColorIndex The train's colour index on a map.
 * @param line          The current section's train line, or {@code null} if it carries none.
 * @param category      The current section's train category, or {@code null} if it carries none.
 * @param lifecycle     How reliable this train's data currently is.
 * @param liveState     What the train is doing right now.
 * @param serviceState  Whether the train can run at all. {@link #isCancelled()} answers the common
 *                      question about it.
 * @param delayed       Whether the train is currently late beyond the configured threshold.
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
    TrainLine line,
    TrainCategory category,
    TrainLifecycleState lifecycle,
    LiveTrainState liveState,
    ServiceState serviceState,
    boolean delayed,
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

    public TrainSnapshot {
        delays = delays == null ? List.of() : List.copyOf(delays);
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
            section == null ? null : section.getTrainLine().orElse(null),
            section == null ? null : section.getTrainCategory().orElse(null),
            tracked.getLifecycleState(),
            tracked.getLiveState(),
            tracked.getServiceState(),
            tracked.isDelayed(),
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
            TrainPositionSnapshot.of(train),
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
     */
    public String destinationText() {
        return currentTitle != null && !currentTitle.isBlank() ? currentTitle : destination.name();
    }

    /**
     * What to show as this train's name: the name of the line it currently runs on, or its own name
     * if it carries no line or the line is unnamed.
     */
    public String displayName() {
        String lineName = line == null ? null : line.getLineName();
        return lineName == null || lineName.isEmpty() ? trainName : lineName;
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

    /** The train line this train currently runs on, if it carries one. */
    public Optional<TrainLine> trainLine() {
        return Optional.ofNullable(line);
    }

    /** The train category this train currently carries, if any. */
    public Optional<TrainCategory> trainCategory() {
        return Optional.ofNullable(category);
    }

    /** Whether this train carries a train line at all. */
    public boolean hasLine() {
        return line != null;
    }

    /** Whether this train carries a train category at all. */
    public boolean hasCategory() {
        return category != null;
    }

    /** The most important status reason currently applying, if there is any. */
    public Optional<DelayInstance> primaryDelay() {
        return delays.isEmpty() ? Optional.empty() : Optional.of(delays.get(0));
    }
}
