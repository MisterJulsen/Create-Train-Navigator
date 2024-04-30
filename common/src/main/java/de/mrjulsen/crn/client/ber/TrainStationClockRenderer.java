package de.mrjulsen.crn.client.ber;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;

import de.mrjulsen.crn.block.be.TrainStationClockBlockEntity;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.ber.AbstractBlockEntityRenderInstance;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

public class TrainStationClockRenderer extends AbstractBlockEntityRenderInstance<TrainStationClockBlockEntity> {

    public TrainStationClockRenderer(TrainStationClockBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public void render(BlockEntityRendererContext context, TrainStationClockBlockEntity pBlockEntity, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pOverlay) {

        context.renderUtils().initRenderEngine();

        

        float z = 3.2f;

        pPoseStack.translate(8, 8, 8 + z);
        pPoseStack.pushPose();
        pPoseStack.mulPose(Vector3f.ZP.rotationDegrees(-90 + ModUtils.clockHandDegrees(pBlockEntity.getLevel().getDayTime() + DragonLib.DAYTIME_SHIFT, 12000)));
        context.renderUtils().fillColor(pBufferSource, pBlockEntity, 0xFF191919, pPoseStack, -0.5f, -0.5f, 0, 6, 1, pBlockEntity.getBlockState().getValue(HorizontalDirectionalBlock.FACING), pPackedLight);
        pPoseStack.popPose();

        pPoseStack.pushPose();
        pPoseStack.mulPose(Vector3f.ZP.rotationDegrees(-90 + ModUtils.clockHandDegrees(pBlockEntity.getLevel().getDayTime() + DragonLib.DAYTIME_SHIFT, 1000)));
        context.renderUtils().fillColor(pBufferSource, pBlockEntity, 0xFF222222, pPoseStack, -0.5f, -0.5f, 0.1f, 7, 1, pBlockEntity.getBlockState().getValue(HorizontalDirectionalBlock.FACING), pPackedLight);
        pPoseStack.popPose();

        pPoseStack.translate(0, 0, -z * 2);
        pPoseStack.pushPose();
        pPoseStack.mulPose(Vector3f.ZN.rotationDegrees(-90 + ModUtils.clockHandDegrees(pBlockEntity.getLevel().getDayTime() + DragonLib.DAYTIME_SHIFT, 12000)));
        pPoseStack.mulPose(Vector3f.YP.rotationDegrees(180));
        context.renderUtils().fillColor(pBufferSource, pBlockEntity, 0xFF191919, pPoseStack, -0.5f, -0.5f, 0, 6, 1, pBlockEntity.getBlockState().getValue(HorizontalDirectionalBlock.FACING), pPackedLight);
        pPoseStack.popPose();

        pPoseStack.pushPose();
        pPoseStack.mulPose(Vector3f.ZN.rotationDegrees(-90 + ModUtils.clockHandDegrees(pBlockEntity.getLevel().getDayTime() + DragonLib.DAYTIME_SHIFT, 1000)));
        pPoseStack.mulPose(Vector3f.YP.rotationDegrees(180));
        context.renderUtils().fillColor(pBufferSource, pBlockEntity, 0xFF222222, pPoseStack, -0.5f, -0.5f, 0.1f, 7, 1, pBlockEntity.getBlockState().getValue(HorizontalDirectionalBlock.FACING), pPackedLight);
        pPoseStack.popPose();
    }
}
