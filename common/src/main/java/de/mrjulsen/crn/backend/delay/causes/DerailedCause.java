package de.mrjulsen.crn.backend.delay.causes;

import java.util.Collection;

import de.mrjulsen.crn.backend.delay.DelayCause;
import de.mrjulsen.crn.backend.delay.DelayContext;
import de.mrjulsen.crn.backend.delay.DelayInstance;
import de.mrjulsen.crn.backend.delay.DelaySeverity;
import de.mrjulsen.crn.config.ModCommonConfig;

/** The train has derailed. */
public final class DerailedCause extends DelayCause {

    @Override
    public DelaySeverity severity() {
        return DelaySeverity.IMPORTANT;
    }

    @Override
    public Collection<DelayInstance> detect(DelayContext ctx) {
        return ctx.createTrain().derailed ? present(ctx) : absent();
    }

    /** A fault rather than an intentional shutdown, so the train stays visible until dealt with. */
    @Override
    public int displayDurationWhileOutOfService() {
        return ModCommonConfig.DISRUPTION_DISPLAY_DURATION_DERAILED.get();
    }
}
