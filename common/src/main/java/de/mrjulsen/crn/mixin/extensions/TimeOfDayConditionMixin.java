package de.mrjulsen.crn.mixin.extensions;

import org.spongepowered.asm.mixin.Mixin;

import com.simibubi.create.content.trains.schedule.condition.TimeOfDayCondition;

import de.mrjulsen.crn.data.schedule.IPredictableWaitCondition;
import de.mrjulsen.mcdragonlib.util.time.DLTime;
import de.mrjulsen.mcdragonlib.util.time.ITimeSystem;
import de.mrjulsen.mcdragonlib.util.time.VanillaTimeSystem;
import org.spongepowered.asm.mixin.Unique;

@Mixin(TimeOfDayCondition.class)
public abstract class TimeOfDayConditionMixin implements IPredictableWaitCondition {

    @Unique
    private TimeOfDayCondition crn$self() {
        return (TimeOfDayCondition)(Object)this;
    }

    /**
     * Predict when the in-game clock next reaches the configured hour:minute.
     * <p>
     * {@code triggeredAtWorldTime} and the returned value live on the backend axis (see
     * {@link de.mrjulsen.crn.api.core.RailwayBackendApi#getCurrentTime()}), which is a linear
     * real-tick clock. The hour/minute/rotation arithmetic Create uses is however defined on the
     * day-time (clock) axis, which under time-changing mods advances at a different, possibly
     * position-dependent rate. So we convert the trigger time onto the clock axis, do Create's
     * rotation math there, and convert the resulting clock-tick wait back into real ticks — honouring
     * time zones via {@link DLTime#addGameTicks}. In vanilla both systems are the identity, so this is
     * bit-identical to a plain tick calculation.
     */
    @Override
    public long waitUntil(long triggeredAtWorldTime) {
        int targetHour = crn$self().getData().getInt("Hour");
        int targetMinute = crn$self().getData().getInt("Minute");
        long rotation = crn$self().getRotation();

        ITimeSystem system = DLTime.defaultTimeSystem();
        DLTime triggeredAt = DLTime.fromGameTicks(triggeredAtWorldTime, VanillaTimeSystem.INSTANCE);
        long clockTicks = Math.round(triggeredAt.toTicks(system));

        long dayTimeInRotation = Math.floorMod(clockTicks, rotation);
        long targetTicks = (((targetHour + 18) % 24) * 1000L + (long) Math.ceil(targetMinute / 60f * 1000)) % rotation;

        long diff = targetTicks - dayTimeInRotation;
        if (diff < 0) {
            diff += rotation;
        }
        return Math.round(triggeredAt.addGameTicks(diff, system).toTicks(VanillaTimeSystem.INSTANCE));
    }
}
