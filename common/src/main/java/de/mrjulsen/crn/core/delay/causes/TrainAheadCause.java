package de.mrjulsen.crn.core.delay.causes;

import java.util.Collection;
import java.util.Optional;

import de.mrjulsen.crn.core.delay.DelayArgument;
import de.mrjulsen.crn.core.delay.DelayCause;
import de.mrjulsen.crn.core.delay.DelayContext;
import de.mrjulsen.crn.core.delay.DelayInstance;
import de.mrjulsen.crn.core.delay.DelaySeverity;

public final class TrainAheadCause extends DelayCause {

    @Override
    public DelaySeverity severity() {
        return DelaySeverity.DELAY;
    }

    @Override
    public Collection<DelayInstance> detect(DelayContext ctx) {
        if (!ctx.hasSignificantSignalWait()) {
            return absent();
        }
        Optional<String> delayedAhead = ctx.delayedBlockingTrainName();
        return delayedAhead.isPresent() ? present(ctx, DelayArgument.trainName(delayedAhead.get())) : absent();
    }
}
