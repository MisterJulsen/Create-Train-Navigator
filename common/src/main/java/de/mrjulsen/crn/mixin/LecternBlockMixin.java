package de.mrjulsen.crn.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import de.mrjulsen.crn.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

@Mixin(LecternBlock.class)
public class LecternBlockMixin {
    
    @Inject(method = "useItemOn", at = @At(value = "HEAD"), cancellable = true)
    public void useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<ItemInteractionResult> cir) {
        if (state.is(Blocks.LECTERN) && !state.getValue(LecternBlock.HAS_BOOK)) {
            if (!level.isClientSide) {
                ItemStack lecternStack = player.isCreative() ? stack.copy() : stack.split(1);
                ModBlocks.NAVIGATOR_LECTERN.get().replaceLectern(state, level, pos, lecternStack);
            }
            cir.setReturnValue(ItemInteractionResult.SUCCESS);
        }
    }
}
