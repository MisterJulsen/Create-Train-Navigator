package de.mrjulsen.crn.client.ber.base;

import com.mojang.blaze3d.vertex.PoseStack;

import de.mrjulsen.crn.client.ber.base.IBlockEntityRendererInstance.BlockEntityRendererContext;
import de.mrjulsen.crn.util.BERUtils;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;

public class StaticBlockEntityRenderer<T extends BlockEntity & IBERInstance<T>> extends RotatableBlockEntityRenderer<T> {

    private final BlockEntityRendererContext context;

    public StaticBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
        this.context = new BlockEntityRendererContext(context, new BERUtils());
    }

    @Override
    protected void renderBlock(T pBlockEntity, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pOverlay) {
        context.renderUtils().initRenderEngine();
        pBlockEntity.getRenderer().render(context, pBlockEntity, pPartialTicks, pPoseStack, pBufferSource, pPackedLight, pOverlay);
        pBlockEntity.getRenderer().postRender(context, pBlockEntity, pPartialTicks, pPoseStack, pBufferSource, pPackedLight, pOverlay);
    }

}
