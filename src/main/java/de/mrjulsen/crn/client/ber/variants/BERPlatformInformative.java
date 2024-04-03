package de.mrjulsen.crn.client.ber.variants;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.block.be.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.client.ber.AdvancedDisplayRenderInstance;
import de.mrjulsen.crn.client.ber.base.BERText;
import de.mrjulsen.crn.client.ber.base.BERText.TextTransformation;
import de.mrjulsen.crn.client.ber.base.IBlockEntityRendererInstance.BlockEntityRendererContext;
import de.mrjulsen.crn.client.ber.base.IBlockEntityRendererInstance.EUpdateReason;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.DeparturePrediction.SimpleDeparturePrediction;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.mcdragonlib.DragonLibConstants;
import de.mrjulsen.mcdragonlib.utils.TimeUtils;
import de.mrjulsen.mcdragonlib.utils.Utils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class BERPlatformInformative implements IBERRenderSubtype<AdvancedDisplayBlockEntity, AdvancedDisplayRenderInstance, Boolean> {

    private List<SimpleDeparturePrediction> lastPredictions = new ArrayList<>();

    private static final int TIME_LABEL_WIDTH = 16;
    
    private static final String keyPlatform = "gui.createrailwaysnavigator.platform";
    private static final String keyLine = "gui.createrailwaysnavigator.line";
    private static final String keyDestination = "gui.createrailwaysnavigator.destination";
    private static final String keyDeparture = "gui.createrailwaysnavigator.departure";
    private static final String keyFollowingTrains = "gui.createrailwaysnavigator.following_trains";

    
    // cache
    private boolean wasPlatformFixed;

    @Override
    public boolean isSingleLined() {
        return false;
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity pBlockEntity, AdvancedDisplayRenderInstance parent) {
        
    }

    private boolean extendedDisplay(AdvancedDisplayBlockEntity blockEntity) {
        return blockEntity.getYSize() > 1;
    }
    
    @Override
    public void renderAdditional(BlockEntityRendererContext context, AdvancedDisplayBlockEntity pBlockEntity, AdvancedDisplayRenderInstance parent, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pOverlay, Boolean backSide) {
        boolean isPlatformFixed = pBlockEntity.isPlatformFixed();
        context.renderUtils().initRenderEngine();
        if (!isPlatformFixed) {            
            context.renderUtils().fillColor(pBufferSource, pBlockEntity, (0xFF << 24) | (pBlockEntity.getColor() & 0x00FFFFFF), pPoseStack, 2.5f, 6.0f, 0.0f, pBlockEntity.getXSizeScaled() * 16 - 5, 0.25f, pBlockEntity.getBlockState().getValue(HorizontalDirectionalBlock.FACING), pPackedLight);

            int count = pBlockEntity.getPlatformInfoLinesCount();
            for (int i = 0; i < count; i += 2) {
                context.renderUtils().fillColor(pBufferSource, pBlockEntity, 0x22FFFFFF, pPoseStack, 2, 4 + ((i + 1) * 5.34f) - 1f, 0.0f, pBlockEntity.getXSizeScaled() * 16 - 4, 5.34f, pBlockEntity.getBlockState().getValue(HorizontalDirectionalBlock.FACING), pPackedLight);
            }
        } else {
            if (extendedDisplay(pBlockEntity)) {
                context.renderUtils().fillColor(pBufferSource, pBlockEntity, (0xFF << 24) | (pBlockEntity.getColor() & 0x00FFFFFF), pPoseStack, 2.5f, 15.5f, 0.0f, pBlockEntity.getXSizeScaled() * 16 - 5, 0.25f, pBlockEntity.getBlockState().getValue(HorizontalDirectionalBlock.FACING), pPackedLight);
            }
        }
    }

    @Override
    public void update(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent, EUpdateReason reason) {
        List<SimpleDeparturePrediction> preds = blockEntity.getPredictions();
        boolean isPlatformFixed = blockEntity.isPlatformFixed();
        /*
        if (preds.size() <= 0) {
            parent.labels.clear();
            return;
        }
        */
        
        int maxLines = blockEntity.getPlatformInfoLinesCount() - (isPlatformFixed ? 1 : 0) ;
        boolean refreshAll = reason != EUpdateReason.DATA_CHANGED ||
            !ModUtils.compareCollections(lastPredictions, preds, (a, b) -> a.stationInfo().platform().equals(b.stationInfo().platform()) && a.trainId().equals(b.trainId())) ||
            wasPlatformFixed != isPlatformFixed
        ;
                
        lastPredictions = preds;
        wasPlatformFixed = blockEntity.isPlatformFixed();

        if (refreshAll) {  
            parent.labels.clear();
            if (isPlatformFixed) {
                addNextDeparture(level, pos, state, blockEntity, parent, reason, 0);
            } else {                
                addHeader(level, pos, state, blockEntity, parent, reason);
            }

            if (isPlatformFixed && preds.size() > 0) {
                for (int i = 1; i < maxLines && i < preds.size(); i++) {
                    addLine(level, pos, state, blockEntity, parent, reason, i, 4 + ((i + 2) * 5.4f));
                }
            } else {
                for (int i = 0; i < maxLines && i < preds.size(); i++) {
                    addLine(level, pos, state, blockEntity, parent, reason, i, 4 + ((i + 1) * 5.34f));
                }
            }
        }
    }

    private void addLine(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent, EUpdateReason reason, int predictionIdx, float y) {
        float displayWidth = blockEntity.getXSizeScaled() * 16 - 4;
        
        // PLATFORM
        Component label = blockEntity.isPlatformFixed() ? Utils.emptyText() : Utils.text(lastPredictions.get(predictionIdx).stationInfo().platform());
        float labelWidth = blockEntity.getPlatformWidth() < 0 ? parent.getFontUtils().font.width(label) * 0.4f : Math.min(parent.getFontUtils().font.width(label) * 0.4f, blockEntity.getPlatformWidth() - 2);
        int platformMaxWidth = blockEntity.getPlatformWidth() < 0 ? (int)(displayWidth - 6) : blockEntity.getPlatformWidth() - 2;

        BERText lastLabel = new BERText(parent.getFontUtils(), label, 0)
            .withIsCentered(false)
            .withMaxWidth(platformMaxWidth, true)
            .withStretchScale(0.2f, 0.4f)
            .withStencil(0, platformMaxWidth)
            .withColor((0xFF << 24) | (blockEntity.getColor()))
            .withPredefinedTextTransformation(new TextTransformation(blockEntity.getXSizeScaled() * 16 - 3 - labelWidth, y, 0.01f, 1, 0.4f))
            .build();
        parent.labels.add(lastLabel);

        // TIME
        parent.labels.add(new BERText(parent.getFontUtils(), () -> {
            List<Component> texts = new ArrayList<>();
            int rawTime = (int)(blockEntity.getLastRefreshedTime() % DragonLibConstants.TICKS_PER_DAY + Constants.TIME_SHIFT + lastPredictions.get(predictionIdx).departureTicks());
            texts.add(Utils.text(TimeUtils.parseTime(rawTime - rawTime % ModClientConfig.REALTIME_PRECISION_THRESHOLD.get(), ModClientConfig.TIME_FORMAT.get())));
            return texts;
        }, 0)
            .withIsCentered(false)
            .withMaxWidth(TIME_LABEL_WIDTH - 4, true)
            .withStretchScale(0.2f, 0.4f)
            .withStencil(0, TIME_LABEL_WIDTH - 4)
            .withColor((0xFF << 24) | (blockEntity.getColor()))
            .withRefreshRate(100)
            .withPredefinedTextTransformation(new TextTransformation(3, y, 0.01f, 1, 0.4f))
            .build()
        );        

        float platformWidth = blockEntity.getPlatformWidth() < 0 ? lastLabel.getScaledTextWidth() + 2 : blockEntity.getPlatformWidth();
        int trainNameWidth = blockEntity.getTrainNameWidth();

        lastLabel = new BERText(parent.getFontUtils(), () -> {
            List<Component> texts = new ArrayList<>();
            texts.add(Utils.text(lastPredictions.get(predictionIdx).trainName()));
            return texts;             
        }, 0)
            .withIsCentered(false)
            .withMaxWidth(Math.min(trainNameWidth - 1, displayWidth - TIME_LABEL_WIDTH - platformWidth - 1), false)
            .withStretchScale(0.2f, 0.4f)
            .withStencil(0, Math.min(trainNameWidth - 1, displayWidth - TIME_LABEL_WIDTH - platformWidth - 1))
            .withColor((0xFF << 24) | (blockEntity.getColor()))
            .withPredefinedTextTransformation(new TextTransformation(TIME_LABEL_WIDTH, y, 0.01f, 1, 0.4f))
            .build()
        ;        
        parent.labels.add(lastLabel);

        lastLabel = new BERText(parent.getFontUtils(), () -> {
            List<Component> texts = new ArrayList<>();
            texts.add(Utils.text(lastPredictions.get(predictionIdx).scheduleTitle()));
            return texts;
        }, 0)
            .withIsCentered(false)
            .withMaxWidth(displayWidth - TIME_LABEL_WIDTH - trainNameWidth - platformWidth + 1, true)
            .withStretchScale(0.25f, 0.4f)
            .withStencil(0, displayWidth - TIME_LABEL_WIDTH - trainNameWidth - platformWidth + 1)
            .withCanScroll(true, 1)
            .withColor((0xFF << 24) | (blockEntity.getColor()))
            .withPredefinedTextTransformation(new TextTransformation(TIME_LABEL_WIDTH + trainNameWidth, y, 0.01f, 1, 0.4f))
            .build()
        ;
        
        parent.labels.add(lastLabel);
    }

    private void addNextDeparture(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent, EUpdateReason reason, int predictionIdx) {
        float displayWidth = blockEntity.getXSizeScaled() * 16 - 6;
        int timeLabelWidth = TIME_LABEL_WIDTH - 3;

        // PLATFORM
        Component label = Utils.text(blockEntity.getStationInfo().platform()).withStyle(ChatFormatting.BOLD);
        float labelWidth = parent.getFontUtils().font.width(label) * 0.8f;
        BERText lastLabel = new BERText(parent.getFontUtils(), label, 0)
            .withIsCentered(false)
            .withMaxWidth(displayWidth, true)
            .withStretchScale(0.8f, 0.8f)
            .withStencil(0, displayWidth)
            .withColor((0xFF << 24) | (blockEntity.getColor()))
            .withPredefinedTextTransformation(new TextTransformation(displayWidth - labelWidth + 3, 3, 0.0f, 1, 1f))
            .build();
        parent.labels.add(lastLabel);
        
        if (lastPredictions.size() <= 0) {
            return;
        }

        float platformWidth = lastLabel.getScaledTextWidth();

        // TIME
        parent.labels.add(new BERText(parent.getFontUtils(), () -> {
            List<Component> texts = new ArrayList<>();
            int rawTime = (int)(blockEntity.getLevel().getDayTime() % DragonLibConstants.TICKS_PER_DAY + Constants.TIME_SHIFT + lastPredictions.get(predictionIdx).departureTicks());
            texts.add(Utils.text(TimeUtils.parseTime(rawTime - rawTime % ModClientConfig.REALTIME_PRECISION_THRESHOLD.get(), ModClientConfig.TIME_FORMAT.get())));
            return texts;
        }, 0)
            .withIsCentered(false)
            .withMaxWidth(timeLabelWidth, true)
            .withStretchScale(0.2f, 0.4f)
            .withStencil(0, timeLabelWidth)
            .withRefreshRate(100)
            .withColor((0xFF << 24) | (blockEntity.getColor()))
            .withPredefinedTextTransformation(new TextTransformation(3, 3, 0.0f, 1, 0.4f))
            .build()
        );

        parent.labels.add(new BERText(parent.getFontUtils(), Utils.text(lastPredictions.get(predictionIdx).trainName()), 0)
            .withIsCentered(false)
            .withMaxWidth(timeLabelWidth, true)
            .withStretchScale(0.15f, 0.3f)
            .withStencil(0, timeLabelWidth)
            .withColor((0xFF << 24) | (blockEntity.getColor()))
            .withPredefinedTextTransformation(new TextTransformation(3, 7, 0.0f, 1, 0.3f))
            .build()
        );

        parent.labels.add(new BERText(parent.getFontUtils(), Utils.text(lastPredictions.get(predictionIdx).scheduleTitle()).withStyle(ChatFormatting.BOLD), 0)
            .withIsCentered(false)
            .withMaxWidth(displayWidth - timeLabelWidth - platformWidth - 5, true)
            .withStretchScale(0.3f, 0.6f)
            .withStencil(0, displayWidth - timeLabelWidth - platformWidth - 5)
            .withCanScroll(true, 0.5f)
            .withColor((0xFF << 24) | (blockEntity.getColor()))
            .withPredefinedTextTransformation(new TextTransformation(3 + timeLabelWidth + 1, 8, 0.0f, 1, 0.6f))
            .build()
        );

        parent.labels.add(new BERText(parent.getFontUtils(), parent.getStopoversString(blockEntity), 0)
            .withIsCentered(false)
            .withMaxWidth(displayWidth - TIME_LABEL_WIDTH - platformWidth - 2, true)
            .withStretchScale(0.2f, 0.25f)
            .withStencil(0, displayWidth - TIME_LABEL_WIDTH - platformWidth - 2)
            .withCanScroll(true, 0.5f)
            .withColor((0xFF << 24) | (blockEntity.getColor()))
            .withPredefinedTextTransformation(new TextTransformation(TIME_LABEL_WIDTH + 1, 5, 0.0f, 1, 0.25f))
            .build()
        );

        if (extendedDisplay(blockEntity)) {
            parent.labels.add(new BERText(parent.getFontUtils(), Utils.translate(keyFollowingTrains), 0)
                .withIsCentered(false)
                .withMaxWidth(displayWidth, true)
                .withStretchScale(0.15f, 0.2f)
                .withStencil(0, displayWidth)
                .withColor((0xFF << 24) | (blockEntity.getColor()))
                .withPredefinedTextTransformation(new TextTransformation(3, 17, 0.0f, 1, 0.2f))
                .build()
            );
        }
    }

    private void addHeader(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent, EUpdateReason reason) {
        float displayWidth = blockEntity.getXSizeScaled() * 16 - 4;
        float TIME_LABEL_WIDTH = 16;
        
        // TIME
        parent.labels.add(new BERText(parent.getFontUtils(), Utils.translate(keyDeparture).withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.ITALIC), 0)
            .withIsCentered(false)
            .withMaxWidth(TIME_LABEL_WIDTH - 4, true)
            .withStretchScale(0.15f, 0.3f)
            .withStencil(0, Math.min(TIME_LABEL_WIDTH - 4, displayWidth - 2))
            .withColor((0xFF << 24) | (blockEntity.getColor()))
            .withPredefinedTextTransformation(new TextTransformation(3, 3, 0.0f, 1, 0.3f))
            .build()
        );

        // PLATFORM
        Component label = Utils.translate(keyPlatform).withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.ITALIC);
        float labelWidth = blockEntity.getPlatformWidth() < 0 ? parent.getFontUtils().font.width(label) * 0.3f : Math.min(parent.getFontUtils().font.width(label) * 0.4f, blockEntity.getPlatformWidth() - 2);
        int platformMaxWidth = blockEntity.getPlatformWidth() < 0 ? (int)(displayWidth - 6) : blockEntity.getPlatformWidth() - 2;
        
        BERText lastLabel = new BERText(parent.getFontUtils(), label, 0)
            .withIsCentered(false)
            .withMaxWidth(platformMaxWidth, true)
            .withStretchScale(0.15f, 0.3f)
            .withStencil(0, labelWidth)
            .withColor((0xFF << 24) | (blockEntity.getColor()))
            .withPredefinedTextTransformation(new TextTransformation(blockEntity.getXSizeScaled() * 16 - 3 - labelWidth, 3, 0.0f, 1, 0.3f))
            .build();
        parent.labels.add(lastLabel);

        float platformWidth = blockEntity.getPlatformWidth() < 0 ? lastLabel.getScaledTextWidth() + 2 : blockEntity.getPlatformWidth();
        int trainNameWidth = blockEntity.getTrainNameWidth();

        lastLabel = new BERText(parent.getFontUtils(), Utils.translate(keyLine).withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.ITALIC), 0)
            .withIsCentered(false)
            .withMaxWidth(Math.min(trainNameWidth - 1, displayWidth - TIME_LABEL_WIDTH - platformWidth - 1), true)
            .withStretchScale(0.15f, 0.3f)
            .withStencil(0, Math.min(trainNameWidth - 1, displayWidth - TIME_LABEL_WIDTH - platformWidth - 1))
            .withColor((0xFF << 24) | (blockEntity.getColor()))
            .withPredefinedTextTransformation(new TextTransformation(TIME_LABEL_WIDTH, 3, 0.0f, 1, 0.3f))
            .build()
        ;        
        parent.labels.add(lastLabel);

        lastLabel = new BERText(parent.getFontUtils(), Utils.translate(keyDestination).withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.ITALIC), 0)
            .withIsCentered(false)
            .withMaxWidth(displayWidth - TIME_LABEL_WIDTH - trainNameWidth - platformWidth + 1, true)
            .withStretchScale(0.15f, 0.3f)
            .withStencil(0, displayWidth - TIME_LABEL_WIDTH - trainNameWidth - platformWidth + 1)
            .withColor((0xFF << 24) | (blockEntity.getColor()))
            .withPredefinedTextTransformation(new TextTransformation(TIME_LABEL_WIDTH + trainNameWidth, 3, 0.0f, 1, 0.3f))
            .build()
        ;
        
        parent.labels.add(lastLabel);
    }
}
