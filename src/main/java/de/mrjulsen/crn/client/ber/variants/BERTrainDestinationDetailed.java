package de.mrjulsen.crn.client.ber.variants;

import de.mrjulsen.crn.block.be.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.client.ber.AdvancedDisplayRenderInstance;
import de.mrjulsen.crn.client.ber.base.BERText;
import de.mrjulsen.crn.client.ber.base.BERText.TextTransformation;
import de.mrjulsen.crn.util.ModGuiUtils;
import de.mrjulsen.mcdragonlib.utils.Utils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BERTrainDestinationDetailed implements IBERRenderSubtype<AdvancedDisplayBlockEntity, AdvancedDisplayRenderInstance, Boolean> {

    @Override
    public void update(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent) {
        if (blockEntity.getTrainData() == null) {
            return;
        }
        parent.labels.clear();
        
        int displayWidth = blockEntity.getXSizeScaled();
        boolean isSingleBlock = blockEntity.getXSizeScaled() <= 1;

        // TRAIN NAME
        float maxWidth = isSingleBlock ? 11.0f : 12.0f;
        MutableComponent line = Utils.text(blockEntity.getTrainData().trainName()).withStyle(ChatFormatting.BOLD);
        BERText lastLabel = new BERText(parent.getFontUtils(), line, 0)
            .withIsCentered(isSingleBlock)
            .withMaxWidth(maxWidth, isSingleBlock)
            .withStretchScale(0.3f, 0.6f)
            .withStencil(0, displayWidth * 16 - 5)
            .withCanScroll(isSingleBlock, 0.5f)
            .withColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
            .withPredefinedTextTransformation(new TextTransformation(isSingleBlock ? 2.5f : 3.0f, 4, 0.0f, 1, 0.5f))
            .build();
        parent.labels.add(lastLabel);

        // DESTINATION
        float startX = lastLabel.getScaledTextWidth();
        line = Utils.text(blockEntity.getTrainData().getNextStop().get().scheduleTitle());
        maxWidth = displayWidth * 16 - 7 - startX;        
        parent.labels.add(new BERText(parent.getFontUtils(), line, 0)
            .withIsCentered(true)
            .withMaxWidth(maxWidth, true)
            .withStretchScale(0.25f, 0.5f)
            .withStencil(0, maxWidth)
            .withCanScroll(true, 1)
            .withColor((0xFF << 24) | (blockEntity.getColor()))
            .withPredefinedTextTransformation(new TextTransformation(2 + startX + 2, 4, 0.0f, 1, 0.5f))
            .build()
        );


        maxWidth = isSingleBlock ? 11.0f : 12.0f;
        line = Utils.text("via").withStyle(ChatFormatting.ITALIC);
        maxWidth = displayWidth * 16 - 8;
        maxWidth /= 0.75f;
        lastLabel = new BERText(parent.getFontUtils(), line, 0)
            .withIsCentered(false)
            .withMaxWidth(maxWidth, true)
            .withStretchScale(0.35f, 0.35f)
            .withColor(ModGuiUtils.darkenColor(blockEntity.getColor(), -0.75f))
            .withPredefinedTextTransformation(new TextTransformation(isSingleBlock ? 2.5f : 3.0f, 10, 0.0f, 0.75f, 0.3f))
            .build();

            parent.labels.add(lastLabel);

        startX = lastLabel.getScaledTextWidth();
        startX *= 0.75f;
        line = parent.getStopoversString(blockEntity);
        maxWidth = displayWidth * 16 - 7 - startX;
        maxWidth /= 0.75f;
        
        parent.labels.add(new BERText(parent.getFontUtils(), line, 0)
            .withIsCentered(false)
            .withMaxWidth(maxWidth, true)
            .withStretchScale(0.35f, 0.35f)
            .withStencil(0, maxWidth)
            .withCanScroll(true, 1)
            .withColor(ModGuiUtils.darkenColor(blockEntity.getColor(), -0.75f))
            .withPredefinedTextTransformation(new TextTransformation(2 + startX + 2, 10, 0.0f, 0.75f, 0.3f))
            .build()
        );
    }    
}
