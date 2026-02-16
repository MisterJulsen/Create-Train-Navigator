package de.mrjulsen.crn.mixin.extensions;

import org.spongepowered.asm.mixin.Mixin;

import com.simibubi.create.content.trains.schedule.condition.TimeOfDayCondition;

import de.mrjulsen.crn.api.IPredictableWaitCondition;
import de.mrjulsen.crn.util.ModUtils;

@Mixin(TimeOfDayCondition.class)
public abstract class TimeOfDayConditionMixin implements IPredictableWaitCondition {

    private TimeOfDayCondition self() {
        return (TimeOfDayCondition)(Object)this;
    }

    @Override
    public long waitUntil(long triggeredAtWorldTime) {
        long ticksOfDay = (ModUtils.convertToTimeTicks(self().getData().getInt("Hour"), self().getData().getInt("Minute")) - 6000) % 24000;
        long worldTimeTicksOfDay = triggeredAtWorldTime % 24000;
        long ticks = ticksOfDay - worldTimeTicksOfDay;
        long res = triggeredAtWorldTime + (int)(ticks >= 0 ? ticks % self().getRotation() : self().getRotation() + (ticks % self().getRotation())) + 1;
        return res;
    }
}
