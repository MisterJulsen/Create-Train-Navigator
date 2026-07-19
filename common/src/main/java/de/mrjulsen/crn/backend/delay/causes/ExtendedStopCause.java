package de.mrjulsen.crn.backend.delay.causes;

import java.util.Collection;

import de.mrjulsen.crn.backend.delay.DelayCause;
import de.mrjulsen.crn.backend.delay.DelayContext;
import de.mrjulsen.crn.backend.delay.DelayInstance;
import de.mrjulsen.crn.backend.delay.DelaySeverity;
import de.mrjulsen.crn.backend.timing.StopTimings;

/** The train is being held at a station noticeably longer than its scheduled departure. */
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
        return ctx.dwellTicks() > normalStay + ctx.delayThreshold() ? present(ctx) : absent();
    }
}
