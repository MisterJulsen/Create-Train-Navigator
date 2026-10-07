package de.mrjulsen.crn.core.delay.causes;

import java.util.Collection;

import de.mrjulsen.crn.core.delay.DelayCause;
import de.mrjulsen.crn.core.delay.DelayContext;
import de.mrjulsen.crn.core.delay.DelayInstance;
import de.mrjulsen.crn.core.delay.DelaySeverity;
import de.mrjulsen.crn.core.delay.DisruptionHandling;

public final class CancelledCause extends DelayCause {

    @Override
    public DelaySeverity severity() {
        return DelaySeverity.IMPORTANT;
    }

    @Override
    public Collection<DelayInstance> detect(DelayContext ctx) {
        return ctx.train().isCancelled() && !ctx.createTrain().derailed
            && (ctx.createTrain().runtime == null || !ctx.createTrain().runtime.paused)
            ? present(ctx)
            : absent();
    }

    @Override
    public DisruptionHandling disruptionHandling() {
        return DisruptionHandling.DELIBERATE;
    }
}
