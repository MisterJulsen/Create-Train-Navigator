package de.mrjulsen.crn.client.ber;

import org.joml.Vector3f;

import com.mojang.math.Axis;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.TrainStationClockBlock;
import de.mrjulsen.crn.block.blockentity.TrainStationClockBlockEntity;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.mcdragonlib.client.ber.AbstractBlockEntityRenderInstance;
import de.mrjulsen.mcdragonlib.client.ber.BERGraphics;
import de.mrjulsen.mcdragonlib.client.util.RenderUtils;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.time.ConfiguredTimeSystem;
import de.mrjulsen.mcdragonlib.util.time.DLTime;
import de.mrjulsen.mcdragonlib.util.time.ITimeSystem;
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
        RenderUtils.initRenderEngine();
        
        graphics.poseStack().pushPose();
        renderInternal(graphics, partialTick);
        graphics.poseStack().popPose();

        if (graphics.blockEntity().getBlockState().getValue(TrainStationClockBlock.DOUBLE)) {
            graphics.poseStack().pushPose();
            graphics.poseStack().translate(8, 8, 8);
            graphics.poseStack().mulPose(Axis.YP.rotationDegrees(90));
            graphics.poseStack().translate(-8, -8, -8);
            renderInternal(graphics, partialTick);
            graphics.poseStack().popPose();
        }
        
    }

    private void renderInternal(BERGraphics<TrainStationClockBlockEntity> graphics, float partialTicks) {
        float z = graphics.blockEntity().getBlockState().getValue(TrainStationClockBlock.DOUBLE) ? 7.25f : 3.25f;

        graphics.poseStack().translate(8, 8, 8 + z);
        RenderUtils.renderTexture(
            DIAL_TEXTURE, graphics,
            new Vector3f(-7, -7, -0.2f),
            14, 14,
            0, 0,
            1, 1,
            graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING),
            graphics.blockEntity().getColor(),
            graphics.blockEntity().isGlowing() ? LightTexture.FULL_BRIGHT : graphics.packedLight(),
            !graphics.blockEntity().isGlowing()
        );

        ITimeSystem timeSystem = new ConfiguredTimeSystem();
        DLTime time = DLTime.fromTicks(graphics.blockEntity().getLevel().getDayTime() + timeSystem.getDaytimeOffset(), timeSystem);

        graphics.poseStack().pushPose();
        graphics.poseStack().mulPose(Axis.ZP.rotationDegrees(-90 + ModUtils.clockHandDegrees(time.toGameDays(), 2)));
        RenderUtils.fillColor(graphics, new Vector3f(-0.5f, -0.5f, 0), 6, 1, DLColor.fromInt(0xFF191919), graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING));
        graphics.poseStack().popPose();

        graphics.poseStack().pushPose();
        graphics.poseStack().mulPose(Axis.ZP.rotationDegrees(-90 + ModUtils.clockHandDegrees(time.toGameHours(), 1)));
        RenderUtils.fillColor(graphics, new Vector3f(-0.5f, -0.5f, 0.1f), 7, 1, DLColor.fromInt(0xFF222222), graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING));
        graphics.poseStack().popPose();

        graphics.poseStack().translate(0, 0, -z * 2);
        graphics.poseStack().pushPose();
        graphics.poseStack().mulPose(Axis.YP.rotationDegrees(180));
        RenderUtils.renderTexture(DIAL_TEXTURE, graphics, new Vector3f(-7, -7, -0.2f), 14, 14, 0, 0, 1, 1, graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING).getOpposite(), graphics.blockEntity().getColor(), graphics.blockEntity().isGlowing() ? LightTexture.FULL_BRIGHT : graphics.packedLight(), !graphics.blockEntity().isGlowing());
        graphics.poseStack().popPose();

        graphics.poseStack().pushPose();
        graphics.poseStack().mulPose(Axis.ZN.rotationDegrees(-90 + ModUtils.clockHandDegrees(time.toGameDays(), 2)));
        graphics.poseStack().mulPose(Axis.YP.rotationDegrees(180));
        RenderUtils.fillColor(graphics, new Vector3f(-0.5f, -0.5f, 0), 6, 1, DLColor.fromInt(0xFF191919), graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING));
        graphics.poseStack().popPose();

        graphics.poseStack().pushPose();
        graphics.poseStack().mulPose(Axis.ZN.rotationDegrees(-90 + ModUtils.clockHandDegrees(time.toGameHours(), 1)));
        graphics.poseStack().mulPose(Axis.YP.rotationDegrees(180));
        RenderUtils.fillColor(graphics, new Vector3f(-0.5f, -0.5f, 0.1f), 7, 1, DLColor.fromInt(0xFF222222), graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING));
        graphics.poseStack().popPose();
    }
}
