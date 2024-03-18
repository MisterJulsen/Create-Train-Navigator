package de.mrjulsen.crn.client.ber.variants;

import com.mojang.blaze3d.vertex.PoseStack;

import de.mrjulsen.crn.client.ber.base.AbstractBlockEntityRenderInstance;
import de.mrjulsen.crn.client.ber.base.IBlockEntityRendererInstance.BlockEntityRendererContext;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public interface IBERRenderSubtype<T extends BlockEntity, S extends AbstractBlockEntityRenderInstance<T>> {
    void update(Level level, BlockPos pos, BlockState state, T blockEntity, S parent);
    default void renderAdditional(BlockEntityRendererContext context, T pBlockEntity, S parent, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pOverlay) {}
    default void tick(Level level, BlockPos pos, BlockState state, T pBlockEntity, S parent) {}
}
