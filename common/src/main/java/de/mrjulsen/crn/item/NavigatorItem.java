package de.mrjulsen.crn.item;

import com.mojang.blaze3d.vertex.PoseStack;
import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.crn.registry.ModBlocks;
import de.mrjulsen.mcdragonlib.client.render.ICustomItemRenderer;
import de.mrjulsen.mcdragonlib.client.util.DLGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.state.BlockState;

public class NavigatorItem extends Item implements ICustomItemRenderer {

    public static final String NBT_BACKGROUND_ID = "BackgroundId";

    public NavigatorItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        ItemStack stack = context.getItemInHand();

        if (player.mayBuild()) {            
            if (player.isShiftKeyDown()) {
                if (ModBlocks.NAVIGATOR_LECTERN.has(state)) {
                    if (!level.isClientSide) {
                        ModBlocks.NAVIGATOR_LECTERN.get().withBlockEntityDo(level, pos, be -> be.swapControllers(stack, player, context.getHand(), state));
                    }
                    return InteractionResult.SUCCESS;
                }
            } else {
                if (state.is(Blocks.LECTERN) && !state.getValue(LecternBlock.HAS_BOOK)) {
                    if (!level.isClientSide) {
                        ItemStack lecternStack = player.isCreative() ? stack.copy() : stack.split(1);
                        ModBlocks.NAVIGATOR_LECTERN.get().replaceLectern(state, level, pos, lecternStack);
                    }
                    return InteractionResult.SUCCESS;
                }

                if (ModBlocks.NAVIGATOR_LECTERN.has(state))
                    return InteractionResult.PASS;
            }
        }

        return player.isShiftKeyDown() ? InteractionResult.FAIL : use(level, player, context.getHand()).getResult();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        if (pLevel.isClientSide) {
            ClientWrapper.showNavigatorGui();
            return InteractionResultHolder.success(pPlayer.getItemInHand(pUsedHand));
        }        
        return super.use(pLevel, pPlayer, pUsedHand);
    }

    @Override
    public void renderAdditional(DLGraphics graphics, ItemStack itemStack, ItemDisplayContext context, boolean leftHand, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay, BakedModel model) {
        ClientWrapper.renderNavigatorItem(graphics, itemStack, context, leftHand, poseStack, buffer, combinedLight, combinedOverlay, model);
    }
}
