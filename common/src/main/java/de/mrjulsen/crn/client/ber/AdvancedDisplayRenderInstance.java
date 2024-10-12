package de.mrjulsen.crn.client.ber;

import com.mojang.math.Axis;

import de.mrjulsen.crn.block.AbstractAdvancedDisplayBlock;
import de.mrjulsen.crn.block.AbstractAdvancedSidedDisplayBlock;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity.EUpdateReason;
import de.mrjulsen.crn.block.properties.ESide;
import de.mrjulsen.crn.client.AdvancedDisplaysRegistry;
import de.mrjulsen.crn.client.AdvancedDisplaysRegistry.DisplayTypeResourceKey;
import de.mrjulsen.mcdragonlib.client.ber.AbstractBlockEntityRenderInstance;
import de.mrjulsen.mcdragonlib.client.ber.BERGraphics;
import de.mrjulsen.mcdragonlib.data.Pair;
import de.mrjulsen.mcdragonlib.data.Tripple;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class AdvancedDisplayRenderInstance extends AbstractBlockEntityRenderInstance<AdvancedDisplayBlockEntity> {

    public IBERRenderSubtype<AdvancedDisplayBlockEntity, AdvancedDisplayRenderInstance, Boolean> renderSubtype;
    private DisplayTypeResourceKey lastType;
    private int lastXSize = 0;

    public AdvancedDisplayRenderInstance(AdvancedDisplayBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public void render(BERGraphics<AdvancedDisplayBlockEntity> graphics, float partialTick) {
        
        if (!graphics.blockEntity().isController()) {
            return;
        }
        
        final int light = graphics.blockEntity().isGlowing() ? LightTexture.FULL_BRIGHT : graphics.packedLight();

        if (graphics.blockEntity().getBlockState().getBlock() instanceof AbstractAdvancedDisplayBlock) {

            renderSubtype.renderTick(Minecraft.getInstance().getDeltaFrameTime());
            
            Tripple<Float, Float, Float> rotation = graphics.blockEntity().renderRotation.get();
            Pair<Float, Float> offset = graphics.blockEntity().renderOffset.get();
            Pair<Float, Float> zOffset = graphics.blockEntity().renderZOffset.get();
            float scale = graphics.blockEntity().renderScale.get();

            graphics.poseStack().pushPose();
            graphics.poseStack().translate(offset.getFirst(), offset.getSecond(), zOffset.getFirst());
            graphics.poseStack().mulPose(Axis.XP.rotationDegrees(rotation.getFirst()));
            graphics.poseStack().mulPose(Axis.YP.rotationDegrees(rotation.getSecond()));
            graphics.poseStack().mulPose(Axis.ZP.rotationDegrees(rotation.getThird()));
            graphics.poseStack().scale(scale, scale, 1);   
            renderSubtype.render(graphics, partialTick, this, light, false);
            graphics.poseStack().popPose();

            if (!(graphics.blockEntity().getBlockState().getBlock() instanceof AbstractAdvancedSidedDisplayBlock) || graphics.blockEntity().getBlockState().getValue(AbstractAdvancedSidedDisplayBlock.SIDE) == ESide.BOTH) {
                graphics.poseStack().pushPose();
                graphics.poseStack().mulPose(Axis.YP.rotationDegrees(180));
                graphics.poseStack().translate(-graphics.blockEntity().getXSize() * 16, 0, -16);
                graphics.poseStack().translate(offset.getFirst(), offset.getSecond(), zOffset.getSecond());
                graphics.poseStack().mulPose(Axis.XP.rotationDegrees(rotation.getFirst()));
                graphics.poseStack().mulPose(Axis.YP.rotationDegrees(rotation.getSecond()));
                graphics.poseStack().mulPose(Axis.ZP.rotationDegrees(rotation.getThird()));
                graphics.poseStack().scale(scale, scale, 1);
                renderSubtype.render(graphics, partialTick, this, light, true);
                graphics.poseStack().popPose();
            }
        }
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity) {
        renderSubtype.tick(level, pos, state, blockEntity, this);

        if (blockEntity.getXSizeScaled() != lastXSize) {
            update(level, pos, state, blockEntity, EUpdateReason.LAYOUT_CHANGED);
        }
        lastXSize = blockEntity.getXSizeScaled();
    }

    @Override
    public void update(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, Object data) {
        EUpdateReason reason = (EUpdateReason)data;
        DisplayTypeResourceKey type = blockEntity.getDisplayTypeKey();
        if (lastType == null || !lastType.equals(type)) {
            renderSubtype = AdvancedDisplaysRegistry.getRenderer(type);
        }

        lastType = type;

        renderSubtype.update(level, pos, state, blockEntity, this, reason);
    }
}
