package de.mrjulsen.crn.backend.delay.causes;

import java.util.Collection;

import de.mrjulsen.crn.backend.delay.DelayCause;
import de.mrjulsen.crn.backend.delay.DelayContext;
import de.mrjulsen.crn.backend.delay.DelayInstance;
import de.mrjulsen.crn.backend.delay.DelaySeverity;

/** The track ahead ended or was torn up, so the train can no longer proceed. */
public final class BrokenTrackCause extends DelayCause {

    @Override
    public DelaySeverity severity() {
        return DelaySeverity.DELAY;
    }

    @Override
    public Collection<DelayInstance> detect(DelayContext ctx) {
        return ctx.createBrokenTrack() ? present(ctx) : absent();
    }
}
