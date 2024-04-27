package de.mrjulsen.crn.client.ber.variants;

import com.mojang.blaze3d.vertex.PoseStack;

import de.mrjulsen.crn.block.be.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.client.ber.AdvancedDisplayRenderInstance;
import de.mrjulsen.crn.client.ber.base.BERText;
import de.mrjulsen.crn.client.ber.base.BERText.TextTransformation;
import de.mrjulsen.crn.client.ber.base.IBlockEntityRendererInstance.BlockEntityRendererContext;
import de.mrjulsen.crn.client.ber.base.IBlockEntityRendererInstance.EUpdateReason;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class BERTrainDestinationInformative implements IBERRenderSubtype<AdvancedDisplayBlockEntity, AdvancedDisplayRenderInstance, Boolean> {

    @Override
    public boolean isSingleLined() {
        return true;
    }

    @Override
    public void update(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent, EUpdateReason reason) {
        if (blockEntity.getTrainData() == null) {
            return;
        }
        parent.labels.clear();
        
        int displayWidth = blockEntity.getXSizeScaled();
        boolean isSingleBlock = blockEntity.getXSizeScaled() <= 1;

        float maxWidth = displayWidth * 16 - 6;
        maxWidth /= 0.5f;

        // TRAIN NAME
        MutableComponent line = TextUtils.text(String.format("%02d", blockEntity.getCarriageData().index() + 1)).withStyle(ChatFormatting.BOLD);
        float textWidth = parent.getFontUtils().font.width(line) * 0.25f;
        BERText lastLabel = parent.carriageIndexLabel = new BERText(parent.getFontUtils(), line, 0)
            .withIsCentered(false)
            .withMaxWidth(maxWidth, false)
            .withStretchScale(0.5f, 0.5f)
            .withColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
            .withPredefinedTextTransformation(new TextTransformation(displayWidth * 16 - 2.5f - textWidth, 2.5f, 0.0f, 0.5f, 0.3f))
            .build();
            parent.labels.add(lastLabel);

        line = TextUtils.text(blockEntity.getTrainData().trainName()).withStyle(ChatFormatting.BOLD);
        lastLabel = new BERText(parent.getFontUtils(), line, 0)
            .withIsCentered(isSingleBlock)
            .withMaxWidth(maxWidth - 10f - textWidth, true)
            .withStretchScale(0.3f, 0.6f)
            .withStencil(0, maxWidth - 10f - textWidth)
            .withCanScroll(true, 0.5f)
            .withColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
            .withPredefinedTextTransformation(new TextTransformation(3.0f, 2.5f, 0.0f, 0.5f, 0.3f))
            .build();
        parent.labels.add(lastLabel);

        // DESTINATION
        line = TextUtils.text(blockEntity.getTrainData().getNextStop().get().stationTagName()).withStyle(ChatFormatting.BOLD);
        parent.labels.add(new BERText(parent.getFontUtils(), line, 0)
            .withIsCentered(false)
            .withMaxWidth(maxWidth, true)
            .withStretchScale(0.25f, 0.5f)
            .withStencil(0, maxWidth)
            .withCanScroll(true, 1)
            .withColor((0xFF << 24) | (blockEntity.getColor()))
            .withPredefinedTextTransformation(new TextTransformation(3.0f, 6.0f, 0.0f, 0.5f, 0.25f))
            .build()
        );

        // STOPOVERS
        line = parent.getStopoversString(blockEntity);
        parent.labels.add(new BERText(parent.getFontUtils(), line, 0)
            .withIsCentered(false)
            .withMaxWidth(maxWidth, true)
            .withStretchScale(0.25f, 0.4f)
            .withStencil(0, maxWidth)
            .withCanScroll(true, 1)
            .withColor((0xFF << 24) | (blockEntity.getColor()))
            .withPredefinedTextTransformation(new TextTransformation(3.0f, 8.75f, 0.0f, 0.5f, 0.2f))
            .build()
        );

        // DESTINATION
        line = TextUtils.text(blockEntity.getTrainData().getNextStop().get().scheduleTitle()).withStyle(ChatFormatting.BOLD);
        parent.labels.add(new BERText(parent.getFontUtils(), line, 0)
            .withIsCentered(false)
            .withMaxWidth(maxWidth, true)
            .withStretchScale(0.25f, 0.5f)
            .withStencil(0, maxWidth)
            .withCanScroll(true, 1)
            .withColor((0xFF << 24) | (blockEntity.getColor()))
            .withPredefinedTextTransformation(new TextTransformation(3.0f, 11.0f, 0.0f, 0.5f, 0.25f))
            .build()
        );
    }

    @Override
    public void renderAdditional(BlockEntityRendererContext context, AdvancedDisplayBlockEntity pBlockEntity, AdvancedDisplayRenderInstance parent, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pOverlay, Boolean backSide) {
        context.renderUtils().initRenderEngine();
        float uv = 1.0f / 256.0f;
        context.renderUtils().renderTexture(
            new ResourceLocation("create:textures/gui/assemble.png"),
            pBufferSource,
            pBlockEntity,
            pPoseStack,
            pBlockEntity.getXSizeScaled() * 16 - 6f - (parent.carriageIndexLabel == null ? 0 : parent.carriageIndexLabel.getScaledTextWidth() * 0.5f),
            2.5f,
            0.0f,
            3.0f,
            2.0f,
            uv * 22,
            uv * 231,
            uv * 22 + uv * 13,
            uv * 231 + uv * 5,
            pBlockEntity.getBlockState().getValue(HorizontalDirectionalBlock.FACING),
            (0xFF << 24) | (pBlockEntity.getColor() & 0x00FFFFFF),
            pPackedLight
        );

        
        context.renderUtils().fillColor(pBufferSource, pBlockEntity, (0xFF << 24) | (pBlockEntity.getColor() & 0x00FFFFFF), pPoseStack, 2.5f, 5.0f, 0.0f, pBlockEntity.getXSizeScaled() * 16 - 5, 0.25f, pBlockEntity.getBlockState().getValue(HorizontalDirectionalBlock.FACING), pPackedLight);
    }

    
}
