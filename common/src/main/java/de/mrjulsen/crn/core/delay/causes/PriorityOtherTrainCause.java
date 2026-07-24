package de.mrjulsen.crn.core.delay.causes;

import java.util.Collection;

import de.mrjulsen.crn.core.delay.DelayArgument;
import de.mrjulsen.crn.core.delay.DelayCause;
import de.mrjulsen.crn.core.delay.DelayContext;
import de.mrjulsen.crn.core.delay.DelayInstance;
import de.mrjulsen.crn.core.delay.DelaySeverity;

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
