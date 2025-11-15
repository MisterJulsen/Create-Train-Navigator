package de.mrjulsen.crn.mixin;

import java.util.LinkedHashSet;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.MountedStorageManager;
import com.simibubi.create.content.trains.entity.CarriageContraption;

import de.mrjulsen.crn.CRNPlatformSpecific;
import de.mrjulsen.crn.block.blockentity.IContraptionBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

@Mixin(MountedStorageManager.class)
public class MountedStorageManagerMixin {

    @Inject(method = "tick", remap = false, at = @At(value = "HEAD"))
    public void onEntityTick(AbstractContraptionEntity entity, CallbackInfo ci) {
        if (entity.getContraption() instanceof CarriageContraption carriage) {
            Set<BlockEntity> beList = new LinkedHashSet<>();

            for (StructureBlockInfo info : entity.getContraption().getBlocks().values()) {
                BlockEntity be = CRNPlatformSpecific.getClientContraptionBlockEntity(entity.getContraption(), info.pos());
                if (be != null) {
                    beList.add(be);
                }
            }

            for (BlockEntity be : beList) {            
                if (be instanceof IContraptionBlockEntity tile) {
                    tile.contraptionTick(entity.level(), be.getBlockPos(), be.getBlockState(), carriage);
                }
            }

            beList.clear();
        }
    }
}
