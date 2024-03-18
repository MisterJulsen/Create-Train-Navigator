package de.mrjulsen.crn.client.ber.base;

import com.mojang.blaze3d.vertex.PoseStack;

import de.mrjulsen.crn.util.BERUtils;
import de.mrjulsen.crn.util.FontUtils;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public interface IBlockEntityRendererInstance<T extends BlockEntity> {

    final FontUtils fontUtils = new FontUtils(Style.DEFAULT_FONT);

    /**
     * The rendering method.
     * @param context
     * @param pBlockEntity
     * @param pPartialTicks
     * @param pPoseStack
     * @param pBufferSource
     * @param pPackedLight
     * @param pOverlay
     */
    void render(BlockEntityRendererContext context, T pBlockEntity, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pOverlay);

    /**
     * Called every tick. Can be used for animations.
     * @param level
     * @param pos
     * @param state
     * @param blockEntity
     */
    default void tick(Level level, BlockPos pos, BlockState state, T blockEntity) { }

    /**
     * Called when the content of the BER changes. Can be used to perform recalculations only when necessary.
     * @param level
     * @param pos
     * @param state
     * @param blockEntity
     */
    default void update(Level level, BlockPos pos, BlockState state, T blockEntity) { }

    default FontUtils getFontUtils() {
        return fontUtils;
    }

    /**
     * Additional data from the default Block Entity Renderer.
     */
    public static record BlockEntityRendererContext(BlockEntityRendererProvider.Context context, BERUtils renderUtils) {}
}
