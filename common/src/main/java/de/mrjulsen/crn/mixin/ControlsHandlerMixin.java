package de.mrjulsen.crn.mixin;

import java.lang.ref.WeakReference;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsHandler;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;

import de.mrjulsen.crn.data.train.TrainListener;
import net.minecraft.core.BlockPos;

@Mixin(ControlsHandler.class)
public class ControlsHandlerMixin { 

    @Accessor("entityRef")
    private static WeakReference<AbstractContraptionEntity> crn$entityRef() {
        throw new AssertionError();
    }
    
    @Inject(method = "startControlling", remap = false, at = @At(value = "HEAD"))
	private static void onStartControlling(AbstractContraptionEntity entity, BlockPos controllerLocalPos, CallbackInfo ci) {
        if (entity.getContraption() instanceof CarriageContraption carriage && carriage.entity instanceof CarriageContraptionEntity trainEntity) {
            if (TrainListener.data.containsKey(trainEntity.trainId)) {
                TrainListener.data.get(trainEntity.trainId).isManualControlled = true;
            }
        }
    }
    @Inject(method = "stopControlling", remap = false, at = @At(value = "HEAD"))
	private static void onStopControlling(CallbackInfo ci) {
        if (crn$entityRef().get() != null && crn$entityRef().get().getContraption() instanceof CarriageContraption carriage && carriage.entity instanceof CarriageContraptionEntity trainEntity) {
            if (TrainListener.data.containsKey(trainEntity.trainId)) {
                TrainListener.data.get(trainEntity.trainId).isManualControlled = false;
            }
        }
    }
}
