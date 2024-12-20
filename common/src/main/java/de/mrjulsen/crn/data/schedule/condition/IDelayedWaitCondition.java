package de.mrjulsen.crn.data.schedule.condition;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.ScheduleEntry;
import com.simibubi.create.content.trains.station.GlobalStation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

@FunctionalInterface
public interface IDelayedWaitCondition {
    

    public static final String NBT_DELAY = "Delay";

    boolean runDelayed(DelayedWaitConditionContext context);
    public static record DelayedWaitConditionContext(Level level, Train train, CompoundTag nbt, GlobalStation station, ScheduleEntry scheduleEntry) {}
}
