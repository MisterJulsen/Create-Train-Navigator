package de.mrjulsen.crn.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.simibubi.create.content.trains.entity.TrainStatus;

@Mixin(TrainStatus.class)
public interface TrainStatusAccessor {
    @Accessor("navigation")
    boolean crn$navigation();
    @Accessor("track")
	boolean crn$track();
    @Accessor("conductor")
	boolean crn$conductor();
}
