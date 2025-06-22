package de.mrjulsen.crn.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.trains.GlobalRailwayManager;

import de.mrjulsen.crn.data.train.TrainListener;
import net.minecraft.world.level.Level;

@Mixin(GlobalRailwayManager.class)
public class GlobalRailwayManagerMixin {
    

    @Inject(method = "tick", at = @At(value = "TAIL"), remap = false)
    public void onTick(Level level, CallbackInfo ci) {
        TrainListener.tick();
    }
}
