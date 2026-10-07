package de.mrjulsen.crn.core.delay.causes;

import java.util.Collection;

import de.mrjulsen.crn.core.delay.DelayCause;
import de.mrjulsen.crn.core.delay.DelayContext;
import de.mrjulsen.crn.core.delay.DelayInstance;
import de.mrjulsen.crn.core.delay.DelaySeverity;
import de.mrjulsen.crn.core.delay.DisruptionHandling;
import de.mrjulsen.crn.config.ModServerConfig;

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

    @Override
    public int displayDurationWhileOutOfService() {
        return ModServerConfig.DISRUPTION_DISPLAY_DURATION_PAUSED.get();
    }

    @Override
    public DisruptionHandling disruptionHandling() {
        return DisruptionHandling.DELIBERATE;
    }
}
