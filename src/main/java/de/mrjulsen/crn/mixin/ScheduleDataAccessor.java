package de.mrjulsen.crn.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.simibubi.create.content.trains.schedule.ScheduleRuntime;

import net.minecraft.nbt.CompoundTag;

@Mixin(ScheduleRuntime.class)
public interface ScheduleDataAccessor {
	@Accessor("conditionProgress")
	List<Integer> crn$conditionProgress();
	@Accessor("conditionContext")
	List<CompoundTag> crn$conditionContext();
}
