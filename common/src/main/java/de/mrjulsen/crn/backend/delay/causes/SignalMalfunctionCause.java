package de.mrjulsen.crn.backend.delay.causes;

import java.util.Collection;

import de.mrjulsen.crn.backend.delay.DelayCause;
import de.mrjulsen.crn.backend.delay.DelayContext;
import de.mrjulsen.crn.backend.delay.DelayInstance;
import de.mrjulsen.crn.backend.delay.DelaySeverity;

/** The train is held at a red signal with no other train occupying the block ahead. */
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
