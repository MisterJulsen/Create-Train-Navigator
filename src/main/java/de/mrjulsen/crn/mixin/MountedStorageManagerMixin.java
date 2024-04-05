package de.mrjulsen.crn.mixin;

import java.util.LinkedHashSet;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.MountedStorageManager;

import de.mrjulsen.crn.block.be.IContraptionBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

@Mixin(MountedStorageManager.class)
public class MountedStorageManagerMixin {

    @Inject(method = "entityTick", remap = false, at = @At(value = "HEAD"))
    public void tick$inject(AbstractContraptionEntity entity, CallbackInfo ci) {
        Set<BlockEntity> beList = new LinkedHashSet<>();
        beList.addAll(entity.getContraption().maybeInstancedBlockEntities);
        beList.addAll(entity.getContraption().specialRenderedBlockEntities);

        for (BlockEntity be : beList) {            
            if (be instanceof IContraptionBlockEntity tile) {
                tile.contraptionTick(entity.level, be.getBlockPos(), be.getBlockState(), entity.getContraption());
            }
        }

        beList.clear();
    }
}
