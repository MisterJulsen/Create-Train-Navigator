package de.mrjulsen.crn.backend.delay.causes;

import java.util.Collection;

import de.mrjulsen.crn.backend.delay.DelayCause;
import de.mrjulsen.crn.backend.delay.DelayContext;
import de.mrjulsen.crn.backend.delay.DelayInstance;
import de.mrjulsen.crn.backend.delay.DelaySeverity;
import de.mrjulsen.crn.backend.delay.DisruptionHandling;
import de.mrjulsen.crn.config.ModCommonConfig;

/** The train's schedule is paused, so it is currently taken out of service. */
public final class SchedulePausedCause extends DelayCause {

    @Override
    public DelaySeverity severity() {
        return DelaySeverity.IMPORTANT;
    }

    @Override
    public Collection<DelayInstance> detect(DelayContext ctx) {
        return ctx.createTrain().runtime != null && ctx.createTrain().runtime.paused && !ctx.createTrain().derailed
            ? present(ctx)
            : absent();
    }

    /** Pausing is deliberate, so the train is only reported briefly. */
    @Override
    public int displayDurationWhileOutOfService() {
        return ModCommonConfig.DISRUPTION_DISPLAY_DURATION_PAUSED.get();
    }

    @Override
    public DisruptionHandling disruptionHandling() {
        return DisruptionHandling.DELIBERATE;
    }
}
