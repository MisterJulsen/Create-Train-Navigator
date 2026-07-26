package de.mrjulsen.crn.core.delay.causes;

import java.util.Collection;

import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.crn.core.delay.DelayArgument;
import de.mrjulsen.crn.core.delay.DelayCause;
import de.mrjulsen.crn.core.delay.DelayContext;
import de.mrjulsen.crn.core.delay.DelayInstance;
import de.mrjulsen.crn.core.delay.DelaySeverity;

public final class PlatformOccupiedCause extends DelayCause {

    @Override
    public DelaySeverity severity() {
        return DelaySeverity.DELAY;
    }

    @Override
    public Collection<DelayInstance> detect(DelayContext ctx) {
        if (ctx.train().isWaitingForPlatform()) {
            return present(ctx, DelayArgument.trainName(ctx.train().getPlatformWaitOccupant()));
        }

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
