package de.mrjulsen.crn.backend.delay.causes;

import java.util.Collection;

import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.crn.backend.delay.DelayArgument;
import de.mrjulsen.crn.backend.delay.DelayCause;
import de.mrjulsen.crn.backend.delay.DelayContext;
import de.mrjulsen.crn.backend.delay.DelayInstance;
import de.mrjulsen.crn.backend.delay.DelaySeverity;

/**
 * The train is waiting to enter its destination station because the platform it is bound for is
 * still occupied by another train.
 */
public final class PlatformOccupiedCause extends DelayCause {

    @Override
    public DelaySeverity severity() {
        return DelaySeverity.DELAY;
    }

    @Override
    public Collection<DelayInstance> detect(DelayContext ctx) {
        Train train = ctx.createTrain();
        if (train.navigation == null || train.navigation.destination == null) {
            return absent();
        }
        if (!ctx.isWaitingForSignal() && !ctx.isStalledSignificantly()) {
            return absent();
        }
        Train occupant = train.navigation.destination.getPresentTrain();
        if (occupant == null || occupant == train) {
            return absent();
        }
        return present(ctx, DelayArgument.trainName(occupant.name.getString()));
    }
}
