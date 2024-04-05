package de.mrjulsen.crn.client.ber.base;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public abstract class RotatableBlockEntityRenderer<T extends BlockEntity> extends SafeBlockEntityRenderer<T> {
    
    protected final Font font;

    public RotatableBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    protected final void renderSafe(T pBlockEntity, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pOverlay) {
        BlockState blockState = pBlockEntity.getBlockState();
        
        pPoseStack.pushPose();
        pPoseStack.translate(0.5D, 0, 0.5F);
        pPoseStack.mulPose(Axis.YP.rotationDegrees(
            blockState.getValue(HorizontalDirectionalBlock.FACING) == Direction.EAST || blockState.getValue(HorizontalDirectionalBlock.FACING) == Direction.WEST
                ? blockState.getValue(HorizontalDirectionalBlock.FACING).getOpposite().toYRot()
                : blockState.getValue(HorizontalDirectionalBlock.FACING).toYRot()
        ));
        pPoseStack.translate(-0.5f, 1, -0.5f);
        pPoseStack.scale(0.0625f, -0.0625f, 0.0625f);

        renderBlock(pBlockEntity, pPartialTicks, pPoseStack, pBufferSource, pPackedLight, pOverlay);

        pPoseStack.popPose();
        
    }

    protected abstract void renderBlock(T pBlockEntity, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pOverlay);
}
