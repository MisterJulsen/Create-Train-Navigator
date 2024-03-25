package de.mrjulsen.crn.client.ber.variants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.mojang.blaze3d.vertex.PoseStack;

import de.mrjulsen.crn.block.be.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.client.ber.AdvancedDisplayRenderInstance;
import de.mrjulsen.crn.client.ber.base.BERText;
import de.mrjulsen.crn.client.ber.base.BERText.TextTransformation;
import de.mrjulsen.crn.client.ber.base.IBlockEntityRendererInstance.BlockEntityRendererContext;
import de.mrjulsen.crn.client.ber.base.IBlockEntityRendererInstance.EUpdateReason;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.DeparturePrediction.SimpleDeparturePrediction;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.mcdragonlib.utils.TimeUtils;
import de.mrjulsen.mcdragonlib.utils.Utils;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BERPlatformDetailed implements IBERRenderSubtype<AdvancedDisplayBlockEntity, AdvancedDisplayRenderInstance, Boolean> {

    private static final String keyTrainDeparture = "gui.createrailwaysnavigator.route_overview.notification.journey_begins";
    private static final String keyTime = "gui.createrailwaysnavigator.time";

    private Collection<UUID> lastTrainOrder = new ArrayList<>();

    private float lastPlatformLabel1Width = 0;
    private BERText label1;
    private float lastPlatformLabel2Width = 0;
    private BERText label2;

    @Override
    public boolean isSingleLined() {
        return true;
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity pBlockEntity, AdvancedDisplayRenderInstance parent) {
        if (label1 != null) label1.tick();
        if (label2 != null) label2.tick();
    }
    
    @Override
    public void renderAdditional(BlockEntityRendererContext context, AdvancedDisplayBlockEntity pBlockEntity, AdvancedDisplayRenderInstance parent, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pOverlay, Boolean backSide) {
        if (label1 != null) label1.render(pPoseStack, pBufferSource, pPackedLight);
        if (label2 != null) label2.render(pPoseStack, pBufferSource, pPackedLight);
    }

    @Override
    public void update(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent, EUpdateReason reason) {
    
        List<SimpleDeparturePrediction> preds = blockEntity.getPredictions().stream().filter(x -> x.departureTicks() < 1000).toList();
        Collection<UUID> uuidOrder = preds.stream().map(x -> x.trainId()).toList();

        float displayWidth = blockEntity.getXSizeScaled() * 16 - 6;
        float maxWidth = 12;
        boolean refreshAll = reason != EUpdateReason.DATA_CHANGED || !lastTrainOrder.equals(uuidOrder);

        parent.labels.clear();

        if (preds.size() <= 0) {
            label1 = null;
            label2 = null;
            setTimer(level, pos, state, blockEntity, parent, reason, 4f);
            return;
        }

        parent.labels.add(new BERText(parent.getFontUtils(), () -> {
            List<Component> texts = new ArrayList<>();
            boolean hasPredictions = preds.size() > 0;
            if (hasPredictions) {
                texts.add(Utils.text(TimeUtils.parseTime((int)(blockEntity.getLastRefreshedTime() % 24000 + 6000 + preds.get(0).departureTicks()), ModClientConfig.TIME_FORMAT.get()))); 
            } else {
                texts.add(Utils.emptyText()); 
            }
            return List.of(ModUtils.concat(texts.toArray(Component[]::new)));
        }, 0)
            .withIsCentered(false)
            .withMaxWidth(maxWidth, true)
            .withStretchScale(0.2f, 0.4f)
            .withStencil(0, maxWidth)
            .withCanScroll(true, 1)
            .withColor((0xFF << 24) | (blockEntity.getColor()))
            .withPredefinedTextTransformation(new TextTransformation(3, 4, 0.0f, 1, 0.4f))
            .build()
        );

        boolean hasPredictions = preds.size() > 0;

        // PLATFORM
        Component label = hasPredictions ? Utils.text(preds.get(0).stationInfo().platform()) : Utils.emptyText();
        float labelWidth = parent.getFontUtils().font.width(label) * 0.4f;
        BERText lastLabel = new BERText(parent.getFontUtils(), label, 0)
            .withIsCentered(false)
            .withMaxWidth(displayWidth, false)
            .withStretchScale(0.25f, 0.4f)
            .withStencil(0, displayWidth)
            .withColor((0xFF << 24) | (blockEntity.getColor()))
            .withPredefinedTextTransformation(new TextTransformation(displayWidth - labelWidth + 3, 4, 0.0f, 1, 0.4f))
            .build();
        parent.labels.add(lastLabel);


        // TRAIN NAME
        float platformWidth = lastLabel.getScaledTextWidth();
        boolean platformWidthChanged = lastPlatformLabel1Width != platformWidth;
        if ((platformWidthChanged || refreshAll) && hasPredictions) {
            label1 = new BERText(parent.getFontUtils(), hasPredictions ? Utils.text(preds.get(0).trainName() + " " + preds.get(0).scheduleTitle()) : Utils.emptyText(), 0)
                .withIsCentered(true)
                .withMaxWidth(displayWidth - maxWidth - platformWidth - 2, true)
                .withStretchScale(0.25f, 0.4f)
                .withStencil(0, displayWidth - maxWidth - platformWidth - 2)
                .withCanScroll(true, 1)
                .withColor((0xFF << 24) | (blockEntity.getColor()))
                .withPredefinedTextTransformation(new TextTransformation(3 + maxWidth + 1, 4, 0.0f, 1, 0.4f))
                .build()
            ;
            lastPlatformLabel1Width = platformWidth;
        } else if (!hasPredictions) {
            label1 = null;
        }

        if (preds.size() <= 1) {
            label2 = null;
            setTimer(level, pos, state, blockEntity, parent, reason, 9f);
            return;
        }

        parent.labels.add(new BERText(parent.getFontUtils(), () -> List.of(Utils.text(TimeUtils.parseTime((int)(blockEntity.getLastRefreshedTime() % 24000 + 6000 + preds.get(1).departureTicks()), ModClientConfig.TIME_FORMAT.get()))), 0)
            .withIsCentered(false)
            .withMaxWidth(maxWidth, true)
            .withStretchScale(0.2f, 0.4f)
            .withStencil(0, maxWidth)
            .withCanScroll(true, 1)
            .withColor((0xFF << 24) | (blockEntity.getColor()))
            .withPredefinedTextTransformation(new TextTransformation(3, 9, 0.0f, 1, 0.4f))
            .build()
        );

        // PLATFORM
        label = hasPredictions ? Utils.text(preds.get(1).stationInfo().platform()) : Utils.emptyText();
        labelWidth = parent.getFontUtils().font.width(label) * 0.4f;
        lastLabel = new BERText(parent.getFontUtils(), label, 0)
            .withIsCentered(false)
            .withMaxWidth(displayWidth, false)
            .withStretchScale(0.25f, 0.4f)
            .withStencil(0, displayWidth)
            .withColor((0xFF << 24) | (blockEntity.getColor()))
            .withPredefinedTextTransformation(new TextTransformation(displayWidth - labelWidth + 3, 9, 0.0f, 1, 0.4f))
            .build();
        parent.labels.add(lastLabel);


        // TRAIN NAME
        platformWidth = lastLabel.getScaledTextWidth();
        platformWidthChanged = lastPlatformLabel2Width != platformWidth;
        if (platformWidthChanged || refreshAll) {
            label2 = new BERText(parent.getFontUtils(), Utils.text(preds.get(1).trainName() + " " + preds.get(1).scheduleTitle()), 0)
                .withIsCentered(true)
                .withMaxWidth(displayWidth - maxWidth - platformWidth - 2, true)
                .withStretchScale(0.25f, 0.4f)
                .withStencil(0, displayWidth - maxWidth - platformWidth - 2)
                .withCanScroll(true, 1)
                .withColor((0xFF << 24) | (blockEntity.getColor()))
                .withPredefinedTextTransformation(new TextTransformation(3 + maxWidth + 1, 9, 0.0f, 1, 0.4f))
                .build()
            ;
            lastPlatformLabel2Width = platformWidth;
        } else if (!hasPredictions) {
            label2 = null;
        }
        
        lastTrainOrder = uuidOrder;
    }

    public void setTimer(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent, EUpdateReason reason, float y) {
        float displayWidth = blockEntity.getXSizeScaled() * 16 - 6;
        parent.labels.add(new BERText(parent.getFontUtils(), () -> List.of(Utils.translate(keyTime, TimeUtils.parseTime((int)(blockEntity.getLevel().getDayTime() % 24000 + 6000), ModClientConfig.TIME_FORMAT.get()))), 0)
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
        );
    }
}
