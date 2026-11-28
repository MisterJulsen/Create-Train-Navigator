/*
 * Create Railways Navigator 1.21.1
 * Copyright (c) 2026 MrJulsen
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package de.mrjulsen.crn.mixin.extensions;

import org.spongepowered.asm.mixin.Mixin;

import com.simibubi.create.content.trains.schedule.condition.ScheduledDelay;
import de.mrjulsen.crn.data.schedule.IPredictableWaitCondition;

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
