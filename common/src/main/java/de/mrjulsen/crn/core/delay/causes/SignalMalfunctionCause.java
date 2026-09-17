package de.mrjulsen.crn.core.delay.causes;

import java.util.Collection;

import de.mrjulsen.crn.core.delay.DelayCause;
import de.mrjulsen.crn.core.delay.DelayContext;
import de.mrjulsen.crn.core.delay.DelayInstance;
import de.mrjulsen.crn.core.delay.DelaySeverity;

public final class SignalMalfunctionCause extends DelayCause {

    @Override
    public DelaySeverity severity() {
        return DelaySeverity.DELAY;
    }

    @Override
    public Collection<DelayInstance> detect(DelayContext ctx) {
        return ctx.hasSignificantSignalWait() && ctx.isRedSignal() && ctx.blockingTrainNames().isEmpty()
            ? present(ctx)
            : absent();
    }
}
