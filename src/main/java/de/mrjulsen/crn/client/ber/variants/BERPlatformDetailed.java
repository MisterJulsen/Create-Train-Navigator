package de.mrjulsen.crn.client.ber.variants;

import java.util.ArrayList;
import java.util.Collection;
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
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BERPlatformDetailed implements IBERRenderSubtype<AdvancedDisplayBlockEntity, AdvancedDisplayRenderInstance, Boolean> {

    private static final String keyTime = "gui.createrailwaysnavigator.time";

    private Collection<SimpleDeparturePrediction> lastPredictions = new ArrayList<>();

    private BERText timeLabel;
    private BERText[][] additionalLabels;

    private int timer;
    private static final int MAX_TIMER = 100;
    private boolean showTime = false;

    @Override
    public boolean isSingleLined() {
        return false;
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity pBlockEntity, AdvancedDisplayRenderInstance parent) {
        if (timeLabel != null) timeLabel.tick();

        if (additionalLabels != null) {
            for (int i = 0; i < additionalLabels.length; i++) {
                if (additionalLabels[i] == null) {
                    continue;
                }

                for (int k = 0; k < additionalLabels[i].length; k++) {
                    if (additionalLabels[i][k] == null) {
                        continue;
                    }

                    additionalLabels[i][k].tick();
                }
            }
        }
        timeLabel.tick();

        timer++;
        if ((timer %= MAX_TIMER) == 0) {
            showTime = !showTime;
        }
    }

    private int maxLines(AdvancedDisplayBlockEntity blockEntity) {
        return blockEntity.getYSize() * 3 - 1;
    }
    
    @Override
    public void renderAdditional(BlockEntityRendererContext context, AdvancedDisplayBlockEntity pBlockEntity, AdvancedDisplayRenderInstance parent, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pOverlay, Boolean backSide) {
        if (additionalLabels != null) {
            for (int i = 0; i < additionalLabels.length; i++) {
                if (additionalLabels[i] == null) {
                    continue;
                }

                if (i >= maxLines(pBlockEntity) - 1 && showTime) {
                    break;
                } else {
                    for (int k = 0; k < additionalLabels[i].length; k++) {
                        if (additionalLabels[i][k] == null) {
                            continue;
                        }
    
                        additionalLabels[i][k].render(pPoseStack, pBufferSource, pPackedLight);
                    }

                    if (i >= maxLines(pBlockEntity) - 1) {
                        return;
                    }
                }
            }
        }
        timeLabel.render(pPoseStack, pBufferSource, pPackedLight);
    }

    @Override
    public void update(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent, EUpdateReason reason) {
    
        List<SimpleDeparturePrediction> preds = blockEntity.getPredictions().stream().filter(x -> x.departureTicks() < 1000).toList();

        if (preds.size() <= 0) {
            additionalLabels = null;
            setTimer(level, pos, state, blockEntity, parent, reason, 4f);
            return;
        }
        
        int maxLines = maxLines(blockEntity);
        boolean refreshAll = reason != EUpdateReason.DATA_CHANGED || !ModUtils.compareCollections(lastPredictions, preds, (a, b) -> a.stationInfo().platform().equals(b.stationInfo().platform()) && a.trainId().equals(b.trainId()));
        

        if (refreshAll) {
            additionalLabels = null;
            timeLabel = null;
            additionalLabels = new BERText[Math.min(preds.size(), maxLines)][];

            for (int i = 0; i < additionalLabels.length; i++) {
                additionalLabels[i] = addLine(level, pos, state, blockEntity, parent, reason, preds.get(i), 4 + (i * 5.4f), i >= maxLines - 1);
            }
            setTimer(level, pos, state, blockEntity, parent, reason, 4 + ((additionalLabels.length < maxLines ? additionalLabels.length : maxLines - 1) * 5.4f));
        }
        lastPredictions = preds;

    }

    private BERText[] addLine(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent, EUpdateReason reason, SimpleDeparturePrediction prediction, float y, boolean lastPossibleLine) {
        float displayWidth = blockEntity.getXSizeScaled() * 16 - 6;
        float maxTimeWidth = 12;

        BERText[] labels = new BERText[3];
        
        labels[0] = new BERText(parent.getFontUtils(), () -> {
            List<Component> texts = new ArrayList<>();
            int rawTime = (int)(blockEntity.getLastRefreshedTime() % DragonLibConstants.TICKS_PER_DAY + Constants.TIME_SHIFT + prediction.departureTicks());
            texts.add(Utils.text(TimeUtils.parseTime(rawTime - rawTime % ModClientConfig.REALTIME_PRECISION_THRESHOLD.get(), ModClientConfig.TIME_FORMAT.get())));
            return texts;
        }, 0)
            .withIsCentered(false)
            .withMaxWidth(maxTimeWidth, true)
            .withStretchScale(0.2f, 0.4f)
            .withStencil(0, maxTimeWidth)
            .withCanScroll(true, 1)
            .withColor((0xFF << 24) | (blockEntity.getColor()))
            .withPredefinedTextTransformation(new TextTransformation(3, y, 0.0f, 1, 0.4f))
            .build()
        ;

        // PLATFORM
        Component label = Utils.text(prediction.stationInfo().platform());
        float labelWidth = parent.getFontUtils().font.width(label) * 0.4f;
        BERText lastLabel = new BERText(parent.getFontUtils(), label, 0)
            .withIsCentered(false)
            .withMaxWidth(displayWidth, false)
            .withStretchScale(0.25f, 0.4f)
            .withStencil(0, displayWidth)
            .withColor((0xFF << 24) | (blockEntity.getColor()))
            .withPredefinedTextTransformation(new TextTransformation(displayWidth - labelWidth + 3, y, 0.0f, 1, 0.4f))
            .build();
        labels[1] = lastLabel;

        float platformWidth = lastLabel.getScaledTextWidth();

        labels[2] = new BERText(parent.getFontUtils(), () -> {
            List<Component> texts = new ArrayList<>();
            texts.add(Utils.text(prediction.trainName() + " " + prediction.scheduleTitle()));
            /*
            if (lastPossibleLine) {
                texts.add(Utils.translate(keyTime, TimeUtils.parseTime((int)(blockEntity.getLevel().getDayTime() % 24000 + 6000), ModClientConfig.TIME_FORMAT.get())));
            }
            */
            return texts;             
        }, 0)
            .withIsCentered(true)
            .withMaxWidth(displayWidth - maxTimeWidth - platformWidth - 2, true)
            .withStretchScale(0.25f, 0.4f)
            .withStencil(0, displayWidth - maxTimeWidth - platformWidth - 2)
            .withCanScroll(true, 1)
            .withTicksPerPage(100)
            .withRefreshRate(lastPossibleLine ? 16 : 0)
            .withColor((0xFF << 24) | (blockEntity.getColor()))
            .withPredefinedTextTransformation(new TextTransformation(3 + maxTimeWidth + 1, y, 0.0f, 1, 0.4f))
            .build()
        ;

        return labels;
    }

    public void setTimer(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent, EUpdateReason reason, float y) {
        float displayWidth = blockEntity.getXSizeScaled() * 16 - 6;
        timeLabel = new BERText(parent.getFontUtils(), () -> List.of(Utils.translate(keyTime, TimeUtils.parseTime((int)(blockEntity.getLevel().getDayTime() % 24000 + 6000), ModClientConfig.TIME_FORMAT.get()))), 0)
            .withIsCentered(true)
            .withMaxWidth(displayWidth, true)
            .withStretchScale(0.4f, 0.4f)
            .withStencil(0, displayWidth)
            .withCanScroll(true, 1)
            .withColor((0xFF << 24) | (blockEntity.getColor()))
            .withTicksPerPage(100)
            .withRefreshRate(16)
            .withPredefinedTextTransformation(new TextTransformation(3, y, 0.0f, 1, 0.4f))
            .build()
        ;
    }
}
