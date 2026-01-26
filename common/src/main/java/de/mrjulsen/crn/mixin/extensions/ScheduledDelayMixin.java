package de.mrjulsen.crn.mixin.extensions;

import org.spongepowered.asm.mixin.Mixin;

import com.simibubi.create.content.trains.schedule.condition.ScheduledDelay;
import de.mrjulsen.crn.api.IPredictableWaitCondition;

@Mixin(ScheduledDelay.class)
public abstract class ScheduledDelayMixin implements IPredictableWaitCondition {

    private ScheduledDelay self() {
        return (ScheduledDelay)(Object)this;
    }

    @Override
    public long waitUntil(long worldTime) {
        return worldTime + self().totalWaitTicks();
    }
}
