package de.mrjulsen.crn.mixin;

import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.display.properties.BasicDisplaySettings;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import de.mrjulsen.crn.block.AbstractAdvancedDisplayBlock;
import de.mrjulsen.crn.block.IBlockGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(DyeItem.class)
public abstract class ItemMixin extends Item {

    public ItemMixin(Properties properties) {
        super(properties);
    }

    public DyeItem self() {
        return (DyeItem)(Object)this;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof AbstractAdvancedDisplayBlock block) {
            AdvancedDisplayBlockEntity blockEntity = ((AdvancedDisplayBlockEntity)level.getBlockEntity(pos)).getController(new IBlockGetter.WorldBlockGetter(level));
            DyeColor dye = self().getDyeColor();
            level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            int dyeColor = dye == DyeColor.ORANGE ? 0xFFFF9900 : dye.getTextColor();

            blockEntity.applyToAll(be -> {
                be.getSettingsAs(BasicDisplaySettings.class).ifPresent(x -> {
                    if (context.getPlayer().isShiftKeyDown()) {
                        x.setBackColor(dyeColor);
                    } else {
                        x.setFontColor(dyeColor);
                    }
                    be.notifyUpdate();
                });
            });

            if (level.isClientSide) {
                blockEntity.getRenderer().update(level, pos, state, blockEntity, AdvancedDisplayBlockEntity.EUpdateReason.LAYOUT_CHANGED);
            }
            return InteractionResult.SUCCESS;
        }
        return super.useOn(context);
    }
}
