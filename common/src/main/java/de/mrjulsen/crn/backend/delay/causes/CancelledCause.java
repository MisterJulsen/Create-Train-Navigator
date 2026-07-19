package de.mrjulsen.crn.backend.delay.causes;

import java.util.Collection;

import de.mrjulsen.crn.backend.delay.DelayCause;
import de.mrjulsen.crn.backend.delay.DelayContext;
import de.mrjulsen.crn.backend.delay.DelayInstance;
import de.mrjulsen.crn.backend.delay.DelaySeverity;
import de.mrjulsen.crn.backend.delay.DisruptionHandling;

/** The train is not running at all (its schedule cannot currently be operated). */
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

    /** In practice a train whose schedule was removed, i.e. one that was parked on purpose. */
    @Override
    public DisruptionHandling disruptionHandling() {
        return DisruptionHandling.DELIBERATE;
    }
}
