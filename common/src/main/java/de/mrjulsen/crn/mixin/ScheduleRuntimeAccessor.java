package de.mrjulsen.crn.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;

import net.minecraft.nbt.CompoundTag;

@Mixin(ScheduleRuntime.class)
public interface ScheduleRuntimeAccessor {
    @Accessor("train")
    Train crn$getTrain();

    @Accessor("ticksInTransit")
    int crn$getTicksInTransit();

    @Invoker("estimateStayDuration")
    int crn$runEstimateStayDuration(int index);
    
	@Accessor("conditionProgress")
	List<Integer> crn$conditionProgress();

	@Accessor("conditionContext")
	List<CompoundTag> crn$conditionContext();
    
	@Accessor("predictionTicks")
	List<Integer> crn$predictionTicks();
}
