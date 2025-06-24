package de.mrjulsen.crn.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.simibubi.create.content.contraptions.Contraption;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

@Mixin(Contraption.class)
public interface ContraptionAccessor {
    
    @Accessor(value = "updateTags", remap = false)
    Map<BlockPos, CompoundTag> crn$updateTags();
}
