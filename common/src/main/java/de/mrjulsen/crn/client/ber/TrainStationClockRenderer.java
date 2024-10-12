package de.mrjulsen.crn.client.ber;

import com.mojang.math.Axis;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.blockentity.TrainStationClockBlockEntity;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.ber.AbstractBlockEntityRenderInstance;
import de.mrjulsen.mcdragonlib.client.ber.BERGraphics;
import de.mrjulsen.mcdragonlib.client.util.BERUtils;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

public class TrainStationClockRenderer extends AbstractBlockEntityRenderInstance<TrainStationClockBlockEntity> {

    private static final ResourceLocation DIAL_TEXTURE = new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "textures/block/dial.png");

    public TrainStationClockRenderer(TrainStationClockBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public void render(BERGraphics<TrainStationClockBlockEntity> graphics, float partialTick) {
        BERUtils.initRenderEngine();
        
        float z = 3.2f;

        graphics.poseStack().translate(8, 8, 8 + z);
        BERUtils.renderTexture(DIAL_TEXTURE, graphics, !graphics.blockEntity().isGlowing(), -7, -7, -0.2f, 14, 14, 0, 0, 1, 1, graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING), (0xFF << 24) | (graphics.blockEntity().getColor()), graphics.blockEntity().isGlowing() ? LightTexture.FULL_BRIGHT : graphics.packedLight());

        graphics.poseStack().pushPose();
        graphics.poseStack().mulPose(Axis.ZP.rotationDegrees(-90 + ModUtils.clockHandDegrees(graphics.blockEntity().getLevel().getDayTime() + DragonLib.DAYTIME_SHIFT, 12000)));
        BERUtils.fillColor(graphics, -0.5f, -0.5f, 0, 6, 1, 0xFF191919, graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING));
        graphics.poseStack().popPose();

        graphics.poseStack().pushPose();
        graphics.poseStack().mulPose(Axis.ZP.rotationDegrees(-90 + ModUtils.clockHandDegrees(graphics.blockEntity().getLevel().getDayTime() + DragonLib.DAYTIME_SHIFT, 1000)));
        BERUtils.fillColor(graphics, -0.5f, -0.5f, 0.1f, 7, 1, 0xFF222222, graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING));
        graphics.poseStack().popPose();

        graphics.poseStack().translate(0, 0, -z * 2);
        graphics.poseStack().pushPose();
        graphics.poseStack().mulPose(Axis.YP.rotationDegrees(180));
        BERUtils.renderTexture(DIAL_TEXTURE, graphics, !graphics.blockEntity().isGlowing(), -7, -7, -0.2f, 14, 14, 0, 0, 1, 1, graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING).getOpposite(), (0xFF << 24) | (graphics.blockEntity().getColor()), graphics.blockEntity().isGlowing() ? LightTexture.FULL_BRIGHT : graphics.packedLight());
        graphics.poseStack().popPose();

        graphics.poseStack().pushPose();
        graphics.poseStack().mulPose(Axis.ZN.rotationDegrees(-90 + ModUtils.clockHandDegrees(graphics.blockEntity().getLevel().getDayTime() + DragonLib.DAYTIME_SHIFT, 12000)));
        graphics.poseStack().mulPose(Axis.YP.rotationDegrees(180));
        BERUtils.fillColor(graphics, -0.5f, -0.5f, 0, 6, 1, 0xFF191919, graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING));
        graphics.poseStack().popPose();

        graphics.poseStack().pushPose();
        graphics.poseStack().mulPose(Axis.ZN.rotationDegrees(-90 + ModUtils.clockHandDegrees(graphics.blockEntity().getLevel().getDayTime() + DragonLib.DAYTIME_SHIFT, 1000)));
        graphics.poseStack().mulPose(Axis.YP.rotationDegrees(180));
        BERUtils.fillColor(graphics, -0.5f, -0.5f, 0.1f, 7, 1, 0xFF222222, graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING));
        graphics.poseStack().popPose();
    }
}
