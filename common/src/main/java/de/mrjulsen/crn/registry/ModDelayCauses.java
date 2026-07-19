package de.mrjulsen.crn.registry;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.backend.delay.DelayCause;
import de.mrjulsen.crn.backend.delay.DelayCauseRegistry;
import de.mrjulsen.crn.backend.delay.causes.BrokenTrackCause;
import de.mrjulsen.crn.backend.delay.causes.CancelledCause;
import de.mrjulsen.crn.backend.delay.causes.DerailedCause;
import de.mrjulsen.crn.backend.delay.causes.ExtendedStopCause;
import de.mrjulsen.crn.backend.delay.causes.NoRouteCause;
import de.mrjulsen.crn.backend.delay.causes.PlatformOccupiedCause;
import de.mrjulsen.crn.backend.delay.causes.PreviousJourneyCause;
import de.mrjulsen.crn.backend.delay.causes.PriorityOtherTrainCause;
import de.mrjulsen.crn.backend.delay.causes.SchedulePausedCause;
import de.mrjulsen.crn.backend.delay.causes.SignalMalfunctionCause;
import de.mrjulsen.crn.backend.delay.causes.StaffShortageCause;
import de.mrjulsen.crn.backend.delay.causes.StalledCause;
import de.mrjulsen.crn.backend.delay.causes.TrainAheadCause;
import de.mrjulsen.crn.backend.delay.causes.UnknownDelayCause;

/**
 * Registers all built-in {@link DelayCause}s of the backend. Registration order is priority order:
 * the most specific / most severe reasons come first, the generic {@link #UNKNOWN_DELAY} fallback
 * last. Add-ons register their own causes the same way via {@link DelayCauseRegistry}.
 */
public final class ModDelayCauses {

    private static final String ID = CreateRailwaysNavigator.MOD_ID;

    // Operational (train not running as planned).
    public static final DelayCause DERAILED = register("derailed", new DerailedCause());
    public static final DelayCause SCHEDULE_PAUSED = register("schedule_paused", new SchedulePausedCause());
    public static final DelayCause CANCELLED = register("cancelled", new CancelledCause());

    // Infrastructure / operational disruptions.
    public static final DelayCause BROKEN_TRACK = register("broken_track", new BrokenTrackCause());
    public static final DelayCause NO_ROUTE = register("no_route", new NoRouteCause());
    public static final DelayCause STAFF_SHORTAGE = register("staff_shortage", new StaffShortageCause());
    public static final DelayCause PLATFORM_OCCUPIED = register("platform_occupied", new PlatformOccupiedCause());

    // Traffic (other trains / signals).
    public static final DelayCause TRAIN_AHEAD = register("train_ahead", new TrainAheadCause());
    public static final DelayCause PRIORITY_OTHER_TRAIN = register("priority_other_train", new PriorityOtherTrainCause());
    public static final DelayCause SIGNAL_MALFUNCTION = register("signal_malfunction", new SignalMalfunctionCause());

    // Own operation.
    public static final DelayCause STALLED = register("stalled", new StalledCause());
    public static final DelayCause EXTENDED_STOP = register("extended_stop", new ExtendedStopCause());
    public static final DelayCause PREVIOUS_JOURNEY = register("previous_journey", new PreviousJourneyCause());

    // Fallback (must stay last).
    public static final DelayCause UNKNOWN_DELAY = register("unknown_delay", new UnknownDelayCause());

    private ModDelayCauses() {}

    private static DelayCause register(String name, DelayCause cause) {
        return DelayCauseRegistry.register(ID, name, cause);
    }

    /** Forces class loading so the {@code static final} registrations run. Called during mod init. */
    public static void init() {}
}
