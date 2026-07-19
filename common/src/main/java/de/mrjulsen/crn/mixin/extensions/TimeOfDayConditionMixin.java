package de.mrjulsen.crn.mixin.extensions;

import org.spongepowered.asm.mixin.Mixin;

import com.simibubi.create.content.trains.schedule.condition.TimeOfDayCondition;

import de.mrjulsen.crn.api.IPredictableWaitCondition;
import org.spongepowered.asm.mixin.Unique;

/**
 * Predicts {@link TimeOfDayCondition} the same way Create's own {@code tickCompletion}/{@code
 * getWaitingStatus} compute it, so the backend's estimate can never drift from what actually
 * happens in-game. {@code getRotation()} is the "every X hours" recurrence (e.g. every 6h/2h/45m);
 * a plain once-a-day trigger is just a rotation of 24000 ticks.
 */
@Mixin(TimeOfDayCondition.class)
public abstract class TimeOfDayConditionMixin implements IPredictableWaitCondition {

    @Unique
    private TimeOfDayCondition crn$self() {
        return (TimeOfDayCondition)(Object)this;
    }

    @Override
    public long waitUntil(long triggeredAtWorldTime) {
        int targetHour = crn$self().getData().getInt("Hour");
        int targetMinute = crn$self().getData().getInt("Minute");
        long rotation = crn$self().getRotation();

        long dayTimeInRotation = triggeredAtWorldTime % rotation;
        long targetTicks = (((targetHour + 18) % 24) * 1000L + (long) Math.ceil(targetMinute / 60f * 1000)) % rotation;

        long diff = targetTicks - dayTimeInRotation;
        if (diff < 0) {
            diff += rotation;
        }
        return triggeredAtWorldTime + diff;
    }
}
