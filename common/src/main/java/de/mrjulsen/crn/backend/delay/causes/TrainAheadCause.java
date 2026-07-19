package de.mrjulsen.crn.backend.delay.causes;

import java.util.Collection;
import java.util.Optional;

import de.mrjulsen.crn.backend.delay.DelayArgument;
import de.mrjulsen.crn.backend.delay.DelayCause;
import de.mrjulsen.crn.backend.delay.DelayContext;
import de.mrjulsen.crn.backend.delay.DelayInstance;
import de.mrjulsen.crn.backend.delay.DelaySeverity;

/** The train is held at a signal by another train that is itself delayed. */
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
