package de.mrjulsen.crn.backend.delay.causes;

import java.util.Collection;

import de.mrjulsen.crn.backend.delay.DelayArgument;
import de.mrjulsen.crn.backend.delay.DelayCause;
import de.mrjulsen.crn.backend.delay.DelayContext;
import de.mrjulsen.crn.backend.delay.DelayInstance;
import de.mrjulsen.crn.backend.delay.DelaySeverity;

/**
 * The train is held at a signal to give way to another train that is itself on time, i.e. a
 * scheduling priority rather than a knock-on delay.
 */
public final class PriorityOtherTrainCause extends DelayCause {

    @Override
    public DelaySeverity severity() {
        return DelaySeverity.DELAY;
    }

    @Override
    public Collection<DelayInstance> detect(DelayContext ctx) {
        if (!ctx.hasSignificantSignalWait() || ctx.blockingTrainNames().isEmpty()) {
            return absent();
        }
        if (ctx.delayedBlockingTrainName().isPresent()) {
            return absent();
        }
        return present(ctx, DelayArgument.trainName(ctx.anyBlockingTrainName().orElse("")));
    }
}
