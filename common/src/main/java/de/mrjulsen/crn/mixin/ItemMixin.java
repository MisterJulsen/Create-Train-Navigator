package de.mrjulsen.crn.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import de.mrjulsen.crn.block.AbstractAdvancedDisplayBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(Item.class)
public abstract class ItemMixin {

    public Item self() {
        return (Item)(Object)this;
    }

    @Inject(method = "useOn", at = @At(value = "HEAD"), cancellable = true)
    public void onUseOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (self() instanceof DyeItem) {
            Level level = context.getLevel();
            BlockPos pos = context.getClickedPos();
            BlockState state = level.getBlockState(pos);
            if (context.getPlayer().isShiftKeyDown() && state.getBlock() instanceof AbstractAdvancedDisplayBlock block) {
                cir.setReturnValue(block.use(state, level, pos, context.getPlayer(), context.getHand(), null));
                return;
            }
        }
    }
}
