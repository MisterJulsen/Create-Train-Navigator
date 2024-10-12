package de.mrjulsen.crn.client.ber.variants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity.EUpdateReason;
import de.mrjulsen.crn.block.display.AdvancedDisplaySource.ETimeDisplay;
import de.mrjulsen.crn.client.ber.AdvancedDisplayRenderInstance;
import de.mrjulsen.crn.client.ber.IBERRenderSubtype;
import de.mrjulsen.crn.client.lang.ELanguage;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.train.TrainStatus.CompiledTrainStatus;
import de.mrjulsen.crn.data.train.portable.StationDisplayData;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.ber.BERGraphics;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel.BoundsHitReaction;
import de.mrjulsen.mcdragonlib.client.util.BERUtils;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.TimeUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class BERPlatformInformative implements IBERRenderSubtype<AdvancedDisplayBlockEntity, AdvancedDisplayRenderInstance, Boolean> {
   
    private static final String keyFollowingTrains = "gui.createrailwaysnavigator.following_trains";

    private static final float LINE_HEIGHT = 5.4f;

    private int maxLines = 0;
    private boolean showInfoLine = false;
    private Component infoLineText = TextUtils.empty();
    private BERLabel statusLabel;
    private BERLabel[] focusArea;
    private BERLabel[][] lines;
    private final BERLabel followingTrainsLabel = new BERLabel(ELanguage.translate(keyFollowingTrains)).setPos(3, 16).setScale(0.2f, 0.2f).setYScale(0.2f);
    

    @Override
    public void renderTick(float deltaTime) {
        DLUtils.doIfNotNull(statusLabel, x -> x.renderTick());
        DLUtils.doIfNotNull(focusArea, x -> {
            for (int i = 0; i < x.length; i++) {
                DLUtils.doIfNotNull(x[i], y -> y.renderTick());
            }
        });
        DLUtils.doIfNotNull(lines, x -> {
            for (int i = 0; i < x.length; i++) {
                DLUtils.doIfNotNull(x[i], y -> {
                    for (int j = 0; j < y.length; j++) {
                        DLUtils.doIfNotNull(y[j], z -> z.renderTick());
                    }
                });
            }
        });
    }

    private boolean isExtendedDisplay(AdvancedDisplayBlockEntity blockEntity) {
        return blockEntity.getYSize() > 1;
    }
    
    @Override
    public void render(BERGraphics<AdvancedDisplayBlockEntity> graphics, float pPartialTicks, AdvancedDisplayRenderInstance parent, int light, boolean backSide) {
        if (isExtendedDisplay(graphics.blockEntity())) {
            BERUtils.fillColor(graphics, 2.5f, 15.5f, 0.0f, graphics.blockEntity().getXSizeScaled() * 16 - 5, 0.25f, (0xFF << 24) | (graphics.blockEntity().getColor() & 0x00FFFFFF), graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING), light);
            followingTrainsLabel
                .setColor((0xFF << 24) | (graphics.blockEntity().getColor() & 0x00FFFFFF))
            ;
            followingTrainsLabel.render(graphics, light);
        }

        DLUtils.doIfNotNull(focusArea, a -> {
            for (int i = 0; i < a.length; i++) {
                BERLabel label = a[i];
                if (label == null) continue;

                graphics.poseStack().pushPose();
                if (backSide) {           
                    float maxWidth = graphics.blockEntity().getXSizeScaled() * 16;
                    if (i == LineComponent.TIME.i()) {
                        graphics.poseStack().translate(-label.getX() + maxWidth - 3 - label.getTextWidth(), 0, 0);
                    } else if (i == LineComponent.REAL_TIME.i()) {
                        graphics.poseStack().translate(-label.getX() + maxWidth - 3 - label.getTextWidth(), 0, 0);
                    } else if (i == LineComponent.TRAIN_NAME.i()) {
                        graphics.poseStack().translate(-label.getX() + maxWidth - 3 - label.getTextWidth(), 0, 0);
                    } else if (i == LineComponent.DESTINATION.i()) {
                        graphics.poseStack().translate(-label.getX() + 5 + a[LineComponent.PLATFORM.i()].getTextWidth(), 0, 0);
                    } else if (i == LineComponent.STOPOVERS.i()) {
                        graphics.poseStack().translate(-label.getX() + 5 + a[LineComponent.PLATFORM.i()].getTextWidth(), 0, 0);
                    } else if (i == LineComponent.PLATFORM.i()) {
                        graphics.poseStack().translate(-label.getX() + 3, 0, 0);
                    }
                }

                label.render(graphics, light);
                graphics.poseStack().popPose();
            }
        });
        DLUtils.doIfNotNull(lines, x -> {
            for (BERLabel[] line : x) {
                if (line == null) continue;
                for (BERLabel label : line) {
                    if (label == null) continue;
                    label.render(graphics, light);
                }
            }
        });

        if (statusLabel != null && !statusLabel.getText().getString().isBlank()) {
            graphics.poseStack().pushPose();
            if (backSide && focusArea != null && focusArea[LineComponent.PLATFORM.i()] != null) {                
                graphics.poseStack().translate(-statusLabel.getX() + 5 + focusArea[LineComponent.PLATFORM.i()].getTextWidth(), 0, 0);
            }
            DLUtils.doIfNotNull(statusLabel, x -> x.render(graphics, light));
            graphics.poseStack().popPose();
        }
    }

    @Override
    public void update(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent, EUpdateReason reason) {
        List<StationDisplayData> preds = blockEntity.getStops().stream().filter(x -> !x.getTrainData().isCancelled() || DragonLib.getCurrentWorldTime() < x.getStationData().getScheduledDepartureTime() + ModClientConfig.DISPLAY_LEAD_TIME.get()).toList();
        
        if (preds.isEmpty()) {
            lines = null;
            focusArea = null;
            statusLabel = null;
            return;
        }
            
        if (reason == EUpdateReason.LAYOUT_CHANGED || this.lines == null || this.focusArea == null) {
            updateLayout(blockEntity, preds);
        }

        showInfoLine = preds.get(0).getStationData().isDepartureDelayed() && preds.get(0).getTrainData().hasStatusInfo();
        if (showInfoLine) {
            // Update status label
            Collection<Component> content = new ArrayList<>();
            if (preds.get(0).getTrainData().isCancelled()) {
                content.add(ELanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.cancelled"));
            } else {                
                content.add(ELanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.delayed", TimeUtils.formatToMinutes(preds.get(0).getStationData().getDepartureTimeDeviation())));
                for (CompiledTrainStatus status : preds.get(0).getTrainData().getStatus()) {
                    content.add(status.text());
                }
            }
            this.infoLineText = TextUtils.concat(TextUtils.text("  +++  "), content);
        } else {
            infoLineText = TextUtils.empty();
        }

        updateFocusContent(blockEntity, preds.get(0));
        for (int i = 1; i < this.lines.length && i < preds.size(); i++) {
            StationDisplayData stop = preds.get(i);
            updateTableContent(blockEntity, stop, i);
        }
    }

    private void updateLayout(AdvancedDisplayBlockEntity blockEntity, List<StationDisplayData> preds) {
        this.focusArea = new BERLabel[7];
        this.lines = new BERLabel[0][];

        followingTrainsLabel        
            .setColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
        ;

        BERLabel timeLabel = new BERLabel()
            .setPos(3, 3)
            .setYScale(0.4f)
            .setScale(0.4f, 0.2f)
            .setColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
        ;
        focusArea[LineComponent.TIME.i()] = timeLabel;

        BERLabel realTimeLabel = new BERLabel()
            .setPos(3, 7)
            .setYScale(0.4f)
            .setScale(0.4f, 0.2f)
            .setBackground((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF), false)
            .setColor(0xFF111111)
        ;
        focusArea[LineComponent.REAL_TIME.i()] = realTimeLabel;

        BERLabel trainNameLabel = new BERLabel()
            .setPos(3, 7)
            .setYScale(0.3f)
            .setMaxWidth(12, BoundsHitReaction.IGNORE)
            .setScale(0.3f, 0.15f)
            .setColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
        ;
        focusArea[LineComponent.TRAIN_NAME.i()] = trainNameLabel;

        BERLabel platformLabel = new BERLabel()
            .setYScale(0.8f)
            .setScale(0.6f, 0.5f)
            .setColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
        ;
        focusArea[LineComponent.PLATFORM.i()] = platformLabel;

        BERLabel destinationLabel = new BERLabel()
            .setYScale(0.6f)
            .setScale(0.6f, 0.4f)
            .setScrollingSpeed(2)
            .setColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
        ;
        focusArea[LineComponent.DESTINATION.i()] = destinationLabel;

        BERLabel stopoversLabel = new BERLabel()
            .setYScale(0.2f)
            .setScale(0.2f, 0.1f)
            .setScrollingSpeed(2)
            .setColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
        ;
        focusArea[LineComponent.STOPOVERS.i()] = stopoversLabel;

        statusLabel = new BERLabel()
            .setText(infoLineText)
            .setYScale(0.3f)
            .setScale(0.3f, 0.3f)
            .setScrollingSpeed(2)
            .setBackground((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF), true)
            .setColor(0xFF111111)
        ;

        if (isExtendedDisplay(blockEntity)) {            
            maxLines = (blockEntity.getYSizeScaled() - 1) * 3;
            int maxIndices = Math.min(this.maxLines, preds.size());
            this.lines = new BERLabel[Math.max(maxIndices, 0)][];
            for (int i = 1; i < this.lines.length; i++) {
                StationDisplayData stop = preds.get(i);
                this.lines[i] = createTableLine(blockEntity, stop, i);
            }
        }
    }

    private BERLabel[] createTableLine(AdvancedDisplayBlockEntity blockEntity, StationDisplayData stop, int index) {
        BERLabel[] components = new BERLabel[5];

        components[LineComponent.TIME.i()] = new BERLabel()
            .setYScale(0.4f)
            .setMaxWidth(12, BoundsHitReaction.SCALE_SCROLL)
            .setScale(0.4f, 0.2f)
            .setColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
        ;
        components[LineComponent.REAL_TIME.i()] = new BERLabel()
            .setYScale(0.4f)
            .setMaxWidth(12, BoundsHitReaction.SCALE_SCROLL)
            .setScale(0.4f, 0.2f)
            .setBackground((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF), false)
            .setColor(0xFF111111)
        ;
        components[LineComponent.TRAIN_NAME.i()] = new BERLabel()
            .setYScale(0.4f)
            //.setMaxWidth(14, BoundsHitReaction.SCALE_SCROLL)
            .setScrollingSpeed(2)
            .setScale(0.4f, 0.2f)
            .setColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
        ;
        
        components[LineComponent.PLATFORM.i()] = new BERLabel()
            .setYScale(0.4f)
            .setScale(0.4f, 0.2f)
            .setColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
        ;

        components[LineComponent.DESTINATION.i()] = new BERLabel()
            .setYScale(0.4f)
            .setScrollingSpeed(2)
            .setScale(0.4f, 0.2f)
            .setColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
        ;

        return components;
    }

    private void updateFocusContent(AdvancedDisplayBlockEntity blockEntity, StationDisplayData stop) {

        followingTrainsLabel        
            .setColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
        ;

        BERLabel timeLabel = focusArea[LineComponent.TIME.i()]
            .setText(TextUtils.text(ModUtils.formatTime(stop.getScheduledTime(), blockEntity.getTimeDisplay() == ETimeDisplay.ETA)))
        ;

        BERLabel realTimeLabel = focusArea[LineComponent.REAL_TIME.i()]
            .setText(TextUtils.text(stop.isDelayed() ? ModUtils.formatTime(stop.getRealTime(), blockEntity.getTimeDisplay() == ETimeDisplay.ETA) : ""))
        ;

        BERLabel trainNameLabel = focusArea[LineComponent.TRAIN_NAME.i()]
            .setText(TextUtils.text(stop.getTrainData().getName()))
            .setPos(3, 7 + (stop.isDelayed() ? 4.5f : 0))
            .setMaxWidth(blockEntity.getTrainNameWidth(), BoundsHitReaction.SCALE_SCROLL)
        ;

        BERLabel platformLabel = focusArea[LineComponent.PLATFORM.i()];
        platformLabel
            .setText(TextUtils.text(stop.getStationData().getStationInfo().platform()).withStyle(ChatFormatting.BOLD))
            .setPos(blockEntity.getXSizeScaled() * 16 - 3 - platformLabel.getTextWidth(), 3);
        ;
        float platformWidth = platformLabel.getTextWidth();

        float x = 5 + Math.max(trainNameLabel.getTextWidth(), Math.max(timeLabel.getTextWidth(), realTimeLabel.getTextWidth()));
        float w = blockEntity.getXSizeScaled() * 16 - 5 - platformWidth - x;
        focusArea[LineComponent.DESTINATION.i()]
            .setText(stop.isLastStop() ?
                ELanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.arrival") :
                TextUtils.text(stop.getStationData().getDestination()))
            .setPos(x, 8.5f)
            .setMaxWidth(w, BoundsHitReaction.SCALE_SCROLL)
        ;

        focusArea[LineComponent.STOPOVERS.i()]
            .setText(stop.isLastStop() ?
                ELanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".schedule_board.train_from", stop.getFirstStopName()) :
                TextUtils.concat(TextUtils.text(" \u25CF "), stop.getStopovers().stream().map(a -> (Component)TextUtils.text(a)).toList()))
            .setPos(x, 6)
            .setMaxWidth(w, BoundsHitReaction.SCALE_SCROLL)
        ;

        statusLabel
            .setText(infoLineText)
            .setPos(x, 2.5f)
            .setMaxWidth(w, BoundsHitReaction.SCALE_SCROLL)
        ;
    }

    private void updateTableContent(AdvancedDisplayBlockEntity blockEntity, StationDisplayData stop, int index) {
        boolean isLast = stop.isLastStop();
        BERLabel[] components = lines[index];
        components[LineComponent.TIME.i()]
            .setText(TextUtils.text(ModUtils.formatTime(stop.getScheduledTime(), blockEntity.getTimeDisplay() == ETimeDisplay.ETA)))
        ;
        components[LineComponent.REAL_TIME.i()]
            .setText(TextUtils.text(stop.getTrainData().isCancelled() ?
                " \u274C " : // X
                (stop.getStationData().isDepartureDelayed() ?
                    (ModUtils.formatTime(isLast ? stop.getStationData().getRealTimeArrivalTime() : stop.getStationData().getRealTimeDepartureTime(), blockEntity.getTimeDisplay() == ETimeDisplay.ETA)) : 
                    ""))) // Nothing (not delayed)
        ;
        components[LineComponent.TRAIN_NAME.i()]
            .setText(TextUtils.text(stop.getTrainData().getName()))
        ;
        components[LineComponent.PLATFORM.i()]
            .setText(blockEntity.isPlatformFixed() ?
                TextUtils.empty() :
                TextUtils.text(stop.getStationData().getStationInfo().platform()))
        ;
        components[LineComponent.DESTINATION.i()]
            .setText(isLast ?
                ELanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".schedule_board.train_from", stop.getFirstStopName()) :
                TextUtils.text(stop.getStationData().getDestination()))
        ;

        int x = 3;
        components[LineComponent.TIME.i()].setPos(x, 11 + 3 + index * LINE_HEIGHT);
        x += components[LineComponent.TIME.i()].getTextWidth() + 2;
        components[LineComponent.REAL_TIME.i()].setPos(x, 11 + 3 + index * LINE_HEIGHT);
        x += components[LineComponent.REAL_TIME.i()].getTextWidth() + 2 + (!components[LineComponent.REAL_TIME.i()].getText().getString().isEmpty() ? 2 : 0);
                
        BERLabel trainNameLabel = components[LineComponent.TRAIN_NAME.i()]
            .setPos(x, 11 + 3 + index * LINE_HEIGHT)
            .setMaxWidth(blockEntity.getTrainNameWidth(), BoundsHitReaction.SCALE_SCROLL)
        ;
        x += trainNameLabel.getMaxWidth() + 2;         
        
        BERLabel platformLabel = components[LineComponent.PLATFORM.i()];
        float platformWidth = platformLabel.getTextWidth();
        platformLabel.setPos(blockEntity.getXSizeScaled() * 16 - 3 - platformWidth, 11 + 3 + index * LINE_HEIGHT);
        components[LineComponent.DESTINATION.i()].setPos(x, 11 + 3 + index * LINE_HEIGHT);
        components[LineComponent.DESTINATION.i()].setMaxWidth(blockEntity.getXSizeScaled() * 16 - 3 - x - platformWidth - 3, BoundsHitReaction.SCALE_SCROLL);
    }

    private static enum LineComponent {
        TIME(0),
        REAL_TIME(1),
        TRAIN_NAME(2),
        DESTINATION(3),
        PLATFORM(4),
        STOPOVERS(5);

        int index;
        LineComponent(int index) {
            this.index = index;
        }
        public int i() {
            return index;
        }
    }
}
