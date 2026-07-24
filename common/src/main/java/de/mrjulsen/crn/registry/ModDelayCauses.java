package de.mrjulsen.crn.registry;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.core.delay.DelayCause;
import de.mrjulsen.crn.core.delay.DelayCauseRegistry;
import de.mrjulsen.crn.core.delay.causes.BrokenTrackCause;
import de.mrjulsen.crn.core.delay.causes.CancelledCause;
import de.mrjulsen.crn.core.delay.causes.DerailedCause;
import de.mrjulsen.crn.core.delay.causes.ExtendedStopCause;
import de.mrjulsen.crn.core.delay.causes.NoRouteCause;
import de.mrjulsen.crn.core.delay.causes.PlatformOccupiedCause;
import de.mrjulsen.crn.core.delay.causes.PreviousJourneyCause;
import de.mrjulsen.crn.core.delay.causes.PriorityOtherTrainCause;
import de.mrjulsen.crn.core.delay.causes.SchedulePausedCause;
import de.mrjulsen.crn.core.delay.causes.SignalMalfunctionCause;
import de.mrjulsen.crn.core.delay.causes.StaffShortageCause;
import de.mrjulsen.crn.core.delay.causes.StalledCause;
import de.mrjulsen.crn.core.delay.causes.TrainAheadCause;
import de.mrjulsen.crn.core.delay.causes.UnknownDelayCause;

public final class ModDelayCauses {

    private static final String ID = CreateRailwaysNavigator.MOD_ID;

    public static final DelayCause DERAILED = register("derailed", new DerailedCause());
    public static final DelayCause SCHEDULE_PAUSED = register("schedule_paused", new SchedulePausedCause());
    public static final DelayCause CANCELLED = register("cancelled", new CancelledCause());

    public static final DelayCause BROKEN_TRACK = register("broken_track", new BrokenTrackCause());
    public static final DelayCause NO_ROUTE = register("no_route", new NoRouteCause());
    public static final DelayCause STAFF_SHORTAGE = register("staff_shortage", new StaffShortageCause());
    public static final DelayCause PLATFORM_OCCUPIED = register("platform_occupied", new PlatformOccupiedCause());

    public static final DelayCause TRAIN_AHEAD = register("train_ahead", new TrainAheadCause());
    public static final DelayCause PRIORITY_OTHER_TRAIN = register("priority_other_train", new PriorityOtherTrainCause());
    public static final DelayCause SIGNAL_MALFUNCTION = register("signal_malfunction", new SignalMalfunctionCause());

    public static final DelayCause STALLED = register("stalled", new StalledCause());
    public static final DelayCause EXTENDED_STOP = register("extended_stop", new ExtendedStopCause());
    public static final DelayCause PREVIOUS_JOURNEY = register("previous_journey", new PreviousJourneyCause());

    public static final DelayCause UNKNOWN_DELAY = register("unknown_delay", new UnknownDelayCause());

    private ModDelayCauses() {}

    private static DelayCause register(String name, DelayCause cause) {
        return DelayCauseRegistry.register(ID, name, cause);
    }

    public static void init() {}
}
