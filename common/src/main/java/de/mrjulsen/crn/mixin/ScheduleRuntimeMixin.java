package de.mrjulsen.crn.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.schedule.ScheduleEntry;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import com.simibubi.create.content.trains.schedule.condition.ScheduleWaitCondition;

import de.mrjulsen.crn.data.schedule.condition.TrainSeparationCondition;
import net.minecraft.world.level.Level;

@Mixin(ScheduleRuntime.class)
public abstract class ScheduleRuntimeMixin {

    @Unique
    private ScheduleRuntime crn$self() {
        return (ScheduleRuntime)(Object)this;
    }

    @Inject(method = "tickConditions", at = @At("HEAD"), remap = false, cancellable = true)
    private void crn$holdForSeparation(Level level, CallbackInfo ci) {
        ScheduleRuntime runtime = crn$self();
        Schedule schedule = runtime.schedule;
        if (schedule == null || runtime.currentEntry < 0 || runtime.currentEntry >= schedule.entries.size()) {
            return;
        }

        ScheduleEntry entry = schedule.entries.get(runtime.currentEntry);
        if (!entry.instruction.supportsConditions() || !crn$anyConditionGroupComplete(runtime, entry)) {
            return;
        }

        if (TrainSeparationCondition.remainingHoldTicks(runtime.train, entry) > 0) {
            ci.cancel();
        }
    }

    @Unique
    private boolean crn$anyConditionGroupComplete(ScheduleRuntime runtime, ScheduleEntry entry) {
        List<List<ScheduleWaitCondition>> conditions = entry.conditions;
        if (conditions == null || runtime.conditionProgress == null) {
            return false;
        }
        int groups = Math.min(conditions.size(), runtime.conditionProgress.size());
        for (int i = 0; i < groups; i++) {
            if (runtime.conditionProgress.get(i) >= conditions.get(i).size()) {
                return true;
            }
        }
        return false;
    }
}
