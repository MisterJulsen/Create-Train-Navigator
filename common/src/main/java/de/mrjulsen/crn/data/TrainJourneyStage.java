package de.mrjulsen.crn.data;


import de.mrjulsen.crn.api.core.snapshot.JourneySnapshot;
import de.mrjulsen.crn.api.core.snapshot.SectionSnapshot;
import de.mrjulsen.crn.api.core.snapshot.StopSnapshot;
import de.mrjulsen.crn.api.core.snapshot.TrainSnapshot;
import de.mrjulsen.crn.core.train.LiveTrainState;
import de.mrjulsen.crn.config.ModServerConfig;

public enum TrainJourneyStage {

    RUNNING,

    OUT_OF_SERVICE,

    AT_TERMINUS,

    BEFORE_TERMINUS,

    TERMINUS_ANNOUNCED,

    SOFT_TERMINUS_ANNOUNCED,

    BEFORE_START;

    public boolean isOutOfService() {
        return this == OUT_OF_SERVICE || this == BEFORE_START;
    }

    public boolean isTerminating(boolean includeSoftTerminus) {
        return this == AT_TERMINUS || this == BEFORE_TERMINUS || this == TERMINUS_ANNOUNCED
            || (this == SOFT_TERMINUS_ANNOUNCED && includeSoftTerminus);
    }

    public boolean isAboutToStart() {
        return this == BEFORE_START;
    }

    public boolean shouldNotBoard(boolean includeSoftTerminus) {
        return this == AT_TERMINUS || this == TERMINUS_ANNOUNCED || isAboutToStart()
            || (this == SOFT_TERMINUS_ANNOUNCED && includeSoftTerminus);
    }

    public boolean isIrregular(boolean includeSoftTerminus) {
        return shouldNotBoard(includeSoftTerminus) || isOutOfService();
    }

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
        if (current.arrivalIn(now) > ModServerConfig.NEXT_STOP_ANNOUNCEMENT.get()) {
            return BEFORE_TERMINUS;
        }
        return terminus ? TERMINUS_ANNOUNCED : SOFT_TERMINUS_ANNOUNCED;
    }
}
