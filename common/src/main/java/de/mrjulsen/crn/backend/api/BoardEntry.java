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
import de.mrjulsen.crn.data.TrainCategory;
import de.mrjulsen.crn.data.TrainLine;
import de.mrjulsen.crn.data.storage.GlobalSettings;

/**
 * One row of a departure or arrival board: a specific train calling at a specific station, with
 * everything needed to render that row without further queries - including the platform, which the
 * {@linkplain StationRef#tag() station's tag} carries.
 *
 * @param trainId       The id of the calling train.
 * @param sessionId     The train's tracking session. See {@link TrainSnapshot#sessionId()}.
 * @param trainName     The train's own name. What to actually show is {@link #displayName()}.
 * @param line          The train line serving this call, or {@code null} if it carries none.
 * @param category      The train category of this call, or {@code null} if it carries none.
 * @param station       The station this row belongs to.
 * @param title         The schedule title the train carries towards this call. May be empty.
 * @param destination   The terminus advertised for this call. A train carrying a schedule title
 *                      advertises that instead - see {@link #destinationText()}.
 * @param scheduled     The timetable times of this call.
 * @param realtime      The projected times of this call.
 * @param arrivalDeviation   How much later than scheduled the train arrives, in ticks.
 * @param departureDeviation How much later than scheduled the train departs, in ticks.
 * @param delayed       Whether this call is late beyond the configured threshold.
 * @param serviceState  Whether the calling train can run at all. {@link #isCancelled()} answers the
 *                      common question about it.
 * @param entryIndex    The schedule entry index of this call, identifying it within the journey.
 * @param sectionIndex  The position of the section this call belongs to, or {@code -1}.
 * @param stopovers     The stations served after this call within the same section.
 * @param delays        The status reasons currently applying to the train, most important first.
 */
public record BoardEntry(
    UUID trainId,
    UUID sessionId,
    String trainName,
    TrainLine line,
    TrainCategory category,
    StationRef station,
    String title,
    StationRef destination,
    StopTimes scheduled,
    StopTimes realtime,
    long arrivalDeviation,
    long departureDeviation,
    boolean delayed,
    ServiceState serviceState,
    int entryIndex,
    int sectionIndex,
    List<StationRef> stopovers,
    List<DelayInstance> delays
) {

    public BoardEntry {
        stopovers = stopovers == null ? List.of() : List.copyOf(stopovers);
        delays = delays == null ? List.of() : List.copyOf(delays);
        station = station == null ? StationRef.NONE : station;
        destination = destination == null ? StationRef.NONE : destination;
    }

    /** Captures the given call of the given train as a board row. */
    public static BoardEntry of(TrackedTrain train, JourneyStop stop) {
        StopSnapshot snapshot = StopSnapshot.of(train, stop, StopVisitState.UPCOMING);
        JourneySection section = stop.getSection();

        return new BoardEntry(
            train.getTrainId(),
            train.getSessionId(),
            train.getTrainName(),
            section == null ? null : section.getTrainLine().orElse(null),
            section == null ? null : section.getTrainCategory().orElse(null),
            snapshot.station(),
            stop.getTitle() == null ? "" : stop.getTitle(),
            resolveDestination(train, stop, snapshot),
            snapshot.scheduled(),
            snapshot.realtime(),
            snapshot.arrivalDeviation(),
            snapshot.departureDeviation(),
            snapshot.delayed(),
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
            .orElse(snapshot.station());
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
        return title != null && !title.isBlank() ? title : destination.name();
    }

    /** The station's name, as a shorthand for {@code station().name()}. */
    public String stationName() {
        return station.name();
    }

    /** The train line serving this call, if it carries one. */
    public Optional<TrainLine> trainLine() {
        return Optional.ofNullable(line);
    }

    /** The train category of this call, if it carries one. */
    public Optional<TrainCategory> trainCategory() {
        return Optional.ofNullable(category);
    }

    /** Whether this call is served by a train line at all. */
    public boolean hasLine() {
        return line != null;
    }

    /**
     * What to show as the operator of this call: the name of the line serving it, or the train's own
     * name if it carries no line or the line is unnamed.
     * <p>
     * Derived from this call's own line rather than from wherever the train happens to be, so a row
     * for a later section of the journey is labelled with the line that will actually run it.
     */
    public String displayName() {
        String lineName = line == null ? null : line.getLineName();
        return lineName == null || lineName.isEmpty() ? trainName : lineName;
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
}
