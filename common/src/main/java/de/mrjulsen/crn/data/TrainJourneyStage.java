package de.mrjulsen.crn.data;

import java.util.Optional;

import de.mrjulsen.crn.backend.api.JourneySnapshot;
import de.mrjulsen.crn.backend.api.SectionSnapshot;
import de.mrjulsen.crn.backend.api.StopSnapshot;
import de.mrjulsen.crn.backend.api.TrainSnapshot;
import de.mrjulsen.crn.backend.core.LiveTrainState;
import de.mrjulsen.crn.config.ModCommonConfig;

/**
 * How far a train is through the service it is running, as the displays on board it need to know.
 * <p>
 * The point of this is what a passenger standing in the carriage should be told: whether to get off
 * soon, whether to stay off, or nothing in particular. It is therefore not a property of the train
 * but of the moment - {@link #of(TrainSnapshot, JourneySnapshot, long)} works it out from the
 * journey and the time, on whichever side is asking.
 * <p>
 * Derived rather than sent, so the announcement thresholds keep running between two updates instead
 * of freezing at whatever they were when the last one arrived.
 */
public enum TrainJourneyStage {

    /** Under way with its terminus still some stops off. */
    RUNNING,

    /** Not running at all: no schedule, out of service, or the train is not known. */
    OUT_OF_SERVICE,

    /** Standing at the last stop of its service. Everybody out. */
    AT_TERMINUS,

    /** The next stop is the last one, but far enough off not to announce yet. */
    BEFORE_TERMINUS,

    /** The next stop is the last one and close enough to announce. */
    TERMINUS_ANNOUNCED,

    /**
     * The next stop is where this service hands over to another and close enough to announce. The
     * train carries on and passengers may stay seated, so whether this counts as terminating is left
     * to whoever is showing it.
     */
    SOFT_TERMINUS_ANNOUNCED,

    /**
     * Standing where its service begins, before it has set off. Nobody should board yet, since the
     * train is not carrying anybody anywhere until it starts.
     */
    BEFORE_START;

    /** Whether the train is not carrying passengers at the moment. */
    public boolean isOutOfService() {
        return this == OUT_OF_SERVICE || this == BEFORE_START;
    }

    /**
     * Whether the service is coming to an end.
     *
     * @param includeSoftTerminus Whether a handover to another service counts as an ending. It does
     *                            for a display announcing the next stop, and does not for one telling
     *                            passengers to get out.
     */
    public boolean isTerminating(boolean includeSoftTerminus) {
        return this == AT_TERMINUS || this == BEFORE_TERMINUS || this == TERMINUS_ANNOUNCED
            || (this == SOFT_TERMINUS_ANNOUNCED && includeSoftTerminus);
    }

    public boolean isAboutToStart() {
        return this == BEFORE_START;
    }

    /** Whether boarding this train would be a mistake. */
    public boolean shouldNotBoard(boolean includeSoftTerminus) {
        return this == AT_TERMINUS || this == TERMINUS_ANNOUNCED || isAboutToStart()
            || (this == SOFT_TERMINUS_ANNOUNCED && includeSoftTerminus);
    }

    /** Whether anything about this train is worth saying instead of the ordinary destination. */
    public boolean isIrregular(boolean includeSoftTerminus) {
        return shouldNotBoard(includeSoftTerminus) || isOutOfService();
    }

    /**
     * Works out which stage the given train is in.
     *
     * @param now The current transformed game time, against which the announcement threshold is
     *            measured.
     */
    public static TrainJourneyStage of(TrainSnapshot train, JourneySnapshot journey, long now) {
        if (train == null || journey == null || journey.isEmpty() || train.isCancelled()
                || train.liveState() == LiveTrainState.NO_SCHEDULE) {
            return OUT_OF_SERVICE;
        }

        StopSnapshot current = journey.currentStop().orElse(null);
        if (current == null) {
            return RUNNING;
        }

        boolean arrived = train.liveState() == LiveTrainState.AT_STATION;
        SectionSnapshot section = journey.operatingSection(arrived).orElse(null);
        if (section == null || !section.usable()) {
            return OUT_OF_SERVICE;
        }

        // Still on its way to where its service begins: the train is running empty towards its start,
        // so nobody should get on. Once it is standing there it is in service like any other.
        if (!arrived && journey.isOrigin(current)) {
            return BEFORE_START;
        }

        boolean terminus = journey.isTerminus(current);
        boolean handover = journey.isHandover(current);
        if (!terminus && !handover) {
            return RUNNING;
        }
        if (arrived) {
            return terminus ? AT_TERMINUS : RUNNING;
        }
        if (current.arrivalIn(now) > ModCommonConfig.NEXT_STOP_ANNOUNCEMENT.get()) {
            return BEFORE_TERMINUS;
        }
        return terminus ? TERMINUS_ANNOUNCED : SOFT_TERMINUS_ANNOUNCED;
    }
}
