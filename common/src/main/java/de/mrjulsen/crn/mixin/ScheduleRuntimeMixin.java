/*
 * Create Railways Navigator 1.20.1 - C6-DL3
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.ScheduleEntry;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import com.simibubi.create.content.trains.station.GlobalStation;

import de.mrjulsen.crn.core.TrainManager;
import de.mrjulsen.crn.data.schedule.condition.TrainSeparationCondition;
import net.minecraft.world.level.Level;

@Mixin(ScheduleRuntime.class)
public abstract class ScheduleRuntimeMixin {

    @Inject(method = "tickConditions", at = @At("HEAD"), remap = false, cancellable = true)
    private void crn$holdForSeparation(Level level, CallbackInfo ci) {
        Train train = ((ScheduleRuntime)(Object)this).train;
        ScheduleEntry entry = TrainSeparationCondition.readyEntry(train);
        if (entry == null) {
            return;
        }

        if (TrainSeparationCondition.remainingHoldTicks(train, entry) > 0 || TrainSeparationCondition.mustGiveWay(train, entry)) {
            ci.cancel();
            return;
        }

        GlobalStation station = train.getCurrentStation();
        if (station != null && station.name != null) {
            TrainManager.getInstance().claimSeparatedDeparture(train, station.name);
        }
    }
}
