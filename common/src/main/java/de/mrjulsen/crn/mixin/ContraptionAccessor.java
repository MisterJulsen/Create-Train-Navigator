package de.mrjulsen.crn.mixin;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.render.ClientContraption;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

@Mixin(Contraption.class)
public interface ContraptionAccessor {
    
    @Accessor(value = "updateTags", remap = false)
    Map<BlockPos, CompoundTag> crn$updateTags();
    
    @Accessor(value = "clientContraption", remap = false)
    AtomicReference<ClientContraption> crn$clientContraption();
}
