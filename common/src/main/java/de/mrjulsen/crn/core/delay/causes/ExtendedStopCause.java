package de.mrjulsen.crn.core.delay.causes;

import java.util.Collection;

import de.mrjulsen.crn.core.delay.DelayCause;
import de.mrjulsen.crn.core.delay.DelayContext;
import de.mrjulsen.crn.core.delay.DelayInstance;
import de.mrjulsen.crn.core.delay.DelaySeverity;
import de.mrjulsen.crn.core.timing.StopTimings;

public final class ExtendedStopCause extends DelayCause {

    @Override
    public DelaySeverity severity() {
        return DelaySeverity.DELAY;
    }

    @Override
    public Collection<DelayInstance> detect(DelayContext ctx) {
        if (!ctx.isAtStation()) {
            return absent();
        }
        StopTimings timing = ctx.currentTiming();
        if (timing == null || !timing.getScheduled().isKnown()) {
            return absent();
        }
        long normalStay = Math.max(timing.getScheduled().stayDuration(), timing.dwellDuration());
        long projectedDwell = ctx.dwellTicks() + Math.max(0, ctx.separationHoldTicks());
        return projectedDwell > normalStay + ctx.delayThreshold() ? present(ctx) : absent();
    }
}
