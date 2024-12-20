package de.mrjulsen.crn.client.ber.variants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity.EUpdateReason;
import de.mrjulsen.crn.block.properties.ETimeDisplay;
import de.mrjulsen.crn.block.display.properties.PlatformDisplayFocusSettings;
import de.mrjulsen.crn.client.ber.AdvancedDisplayRenderInstance;
import de.mrjulsen.crn.client.lang.CustomLanguage;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.train.TrainStatus.CompiledTrainStatus;
import de.mrjulsen.crn.data.train.portable.StationDisplayData;
import de.mrjulsen.crn.data.train.portable.TrainStopDisplayData;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.ber.BERGraphics;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel.BoundsHitReaction;
import de.mrjulsen.mcdragonlib.client.util.BERUtils;
import de.mrjulsen.mcdragonlib.util.ColorUtils;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.TimeUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class BERPlatformInformative implements AbstractAdvancedDisplayRenderer<PlatformDisplayFocusSettings> { //TODO PlatformWidth verwenden
   
    private static final String keyFollowingTrains = "gui.createrailwaysnavigator.following_trains";

    private static final float LINE_HEIGHT = 5.4f;

    private int maxLines = 0;
    private boolean showInfoLine = false;
    private Component infoLineText = TextUtils.empty();
    private BERLabel statusLabel;
    private final BERLabel platformLabel = new BERLabel();
    private BERLabel[] focusArea;
    private BERLabel[][] lines;
    private final BERLabel followingTrainsLabel = new BERLabel(CustomLanguage.translate(keyFollowingTrains)).setPos(3, 16).setScale(0.2f, 0.2f).setYScale(0.2f);
    

    @Override
    public void renderTick(float deltaTime) {
        DLUtils.doIfNotNull(statusLabel, x -> x.renderTick());
        DLUtils.doIfNotNull(platformLabel, x -> x.renderTick());
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
            BERUtils.fillColor(graphics, 2.5f, 15.5f, 0.0f, graphics.blockEntity().getXSizeScaled() * 16 - 5, 0.25f, (0xFF << 24) | (getDisplaySettings(graphics.blockEntity()).getFontColor() & 0x00FFFFFF), graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING), light);
            followingTrainsLabel
                .setColor((0xFF << 24) | (getDisplaySettings(graphics.blockEntity()).getFontColor() & 0x00FFFFFF))
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
                        graphics.poseStack().translate(-label.getX() + 5 + platformLabel.getMaxWidth(), 0, 0);
                    } else if (i == LineComponent.STOPOVERS.i()) {
                        graphics.poseStack().translate(-label.getX() + 5 + platformLabel.getMaxWidth(), 0, 0);
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

        graphics.poseStack().pushPose();
        
        if (statusLabel != null && !statusLabel.getText().getString().isBlank()) {
            graphics.poseStack().pushPose();
            if (backSide) {           
                graphics.poseStack().translate(-statusLabel.getX() + 5 + platformLabel.getMaxWidth(), 0, 0);
            }
            DLUtils.doIfNotNull(statusLabel, x -> x.render(graphics, light));
            graphics.poseStack().popPose();
        }
        if (backSide && platformLabel != null) {                
            graphics.poseStack().translate(-graphics.blockEntity().getXSizeScaled() * 16 + 6 + platformLabel.getTextWidth(), 0, 0);
        }
        platformLabel.render(graphics, light);
        graphics.poseStack().popPose();
    }

    @Override
    public void update(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent, EUpdateReason reason) {
        List<StationDisplayData> preds = blockEntity.getStops().stream().filter(x -> !x.getTrainData().isCancelled() || DragonLib.getCurrentWorldTime() < x.getStationData().getScheduledDepartureTime() + ModClientConfig.DISPLAY_LEAD_TIME.get()).toList();
        
        if (preds.isEmpty()) {
            lines = null;
            focusArea = null;
            statusLabel = null;

            if (blockEntity.isPlatformFixed() && blockEntity.getStationInfo() != null) {
                this.platformLabel
                    .setText(TextUtils.text(blockEntity.getStationInfo().platform()).withStyle(ChatFormatting.BOLD))
                    .setPos(blockEntity.getXSizeScaled() * 16 - 3 - platformLabel.getTextWidth(), 3);
                ;
            } else {
                this.platformLabel.setText(TextUtils.empty());
            }
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
                content.add(CustomLanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.cancelled"));
            } else {
                TrainStopDisplayData  displayData = preds.get(0).getStationData();
                String delay = getDisplaySettings(blockEntity).getTimeDisplay() == ETimeDisplay.ETA ? ModUtils.timeRemainingString(displayData.getDepartureTimeDeviation()) : String.valueOf(TimeUtils.formatToMinutes(displayData.getDepartureTimeDeviation()));
                MutableComponent delayComponent = CustomLanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.delayed", delay);
                if (getDisplaySettings(blockEntity).getTimeDisplay() == ETimeDisplay.ABS) {
                    delayComponent.append(" ").append(CustomLanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.delay_abs_suffix"));
                }
                content.add(delayComponent);
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
            .setColor((0xFF << 24) | (getDisplaySettings(blockEntity).getFontColor() & 0x00FFFFFF))
        ;

        BERLabel timeLabel = new BERLabel()
            .setPos(3, 3)
            .setYScale(0.4f)
            .setScale(0.4f, 0.2f)
            .setColor((0xFF << 24) | (getDisplaySettings(blockEntity).getFontColor() & 0x00FFFFFF))
        ;
        focusArea[LineComponent.TIME.i()] = timeLabel;

        BERLabel realTimeLabel = new BERLabel()
            .setPos(3, 7)
            .setYScale(0.4f)
            .setScale(0.4f, 0.2f)
            .setBackground((0xFF << 24) | (getDisplaySettings(blockEntity).getFontColor() & 0x00FFFFFF), false)
            .setColor(0xFF111111)
        ;
        focusArea[LineComponent.REAL_TIME.i()] = realTimeLabel;

        BERLabel trainNameLabel = new BERLabel()
            .setPos(3, 7)
            .setYScale(0.3f)
            .setMaxWidth(12, BoundsHitReaction.IGNORE)
            .setScale(0.3f, 0.15f)
            .setColor((0xFF << 24) | (getDisplaySettings(blockEntity).getFontColor() & 0x00FFFFFF))
        ;
        focusArea[LineComponent.TRAIN_NAME.i()] = trainNameLabel;

        this.platformLabel
            .setYScale(0.8f)
            .setScale(0.6f, 0.5f)
            .setColor((0xFF << 24) | (getDisplaySettings(blockEntity).getFontColor() & 0x00FFFFFF))
        ;
        //focusArea[LineComponent.PLATFORM.i()] = platformLabel;

        BERLabel destinationLabel = new BERLabel()
            .setYScale(0.6f)
            .setScale(0.6f, 0.4f)
            .setScrollingSpeed(2)
            .setColor((0xFF << 24) | (getDisplaySettings(blockEntity).getFontColor() & 0x00FFFFFF))
        ;
        focusArea[LineComponent.DESTINATION.i()] = destinationLabel;

        BERLabel stopoversLabel = new BERLabel()
            .setYScale(0.2f)
            .setScale(0.2f, 0.1f)
            .setScrollingSpeed(2)
            .setColor((0xFF << 24) | (getDisplaySettings(blockEntity).getFontColor() & 0x00FFFFFF))
        ;
        focusArea[LineComponent.STOPOVERS.i()] = stopoversLabel;

        statusLabel = new BERLabel()
            .setText(infoLineText)
            .setYScale(0.3f)
            .setScale(0.3f, 0.3f)
            .setScrollingSpeed(2)
            .setBackground((0xFF << 24) | (getDisplaySettings(blockEntity).getFontColor() & 0x00FFFFFF), true)
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
            .setColor((0xFF << 24) | (getDisplaySettings(blockEntity).getFontColor() & 0x00FFFFFF))
        ;
        components[LineComponent.REAL_TIME.i()] = new BERLabel()
            .setYScale(0.4f)
            .setMaxWidth(12, BoundsHitReaction.SCALE_SCROLL)
            .setScale(0.4f, 0.2f)
            .setBackground((0xFF << 24) | (getDisplaySettings(blockEntity).getFontColor() & 0x00FFFFFF), false)
            .setColor(0xFF111111)
        ;
        components[LineComponent.TRAIN_NAME.i()] = new BERLabel()
            .setYScale(0.4f)
            //.setMaxWidth(14, BoundsHitReaction.SCALE_SCROLL)
            .setScrollingSpeed(2)
            .setScale(0.4f, 0.2f)
            .setColor((0xFF << 24) | (getDisplaySettings(blockEntity).getFontColor() & 0x00FFFFFF))
        ;
        
        components[LineComponent.PLATFORM.i()] = new BERLabel()
            .setYScale(0.4f)
            .setScale(0.4f, 0.2f)
            .setColor((0xFF << 24) | (getDisplaySettings(blockEntity).getFontColor() & 0x00FFFFFF))
        ;

        components[LineComponent.DESTINATION.i()] = new BERLabel()
            .setYScale(0.4f)
            .setScrollingSpeed(2)
            .setScale(0.4f, 0.2f)
            .setColor((0xFF << 24) | (getDisplaySettings(blockEntity).getFontColor() & 0x00FFFFFF))
        ;

        return components;
    }

    private void updateFocusContent(AdvancedDisplayBlockEntity blockEntity, StationDisplayData stop) {
        PlatformDisplayFocusSettings settings = getDisplaySettings(blockEntity);
        boolean isLast = settings.showArrival() && stop.isLastStop();

        followingTrainsLabel
            .setColor((0xFF << 24) | (getDisplaySettings(blockEntity).getFontColor() & 0x00FFFFFF))
        ;

        BERLabel timeLabel = focusArea[LineComponent.TIME.i()]
            .setText(TextUtils.text(ModUtils.formatTime(stop.getScheduledTime(), getDisplaySettings(blockEntity).getTimeDisplay() == ETimeDisplay.ETA)))
        ;

        BERLabel realTimeLabel = focusArea[LineComponent.REAL_TIME.i()]
            .setText(TextUtils.text(stop.isDelayed() ? ModUtils.formatTime(stop.getRealTime(), getDisplaySettings(blockEntity).getTimeDisplay() == ETimeDisplay.ETA) : ""))
            .setColor(ColorUtils.brightnessDependingFontColor(getDisplaySettings(blockEntity).getFontColor(), LIGHT_FONT_COLOR, DARK_FONT_COLOR))
        ;

        int trainNameWidth = settings.getTrainNameWidthNextStop();
        BERLabel trainNameLabel = focusArea[LineComponent.TRAIN_NAME.i()];
        trainNameLabel
            .setText(TextUtils.text(stop.getTrainData().getName()))
            .setPos(3, 7 + (stop.isDelayed() ? 4.5f : 0))
            .setMaxWidth(settings.isAutoTrainNameWidthNextStop() ? trainNameLabel.getTextWidth() : trainNameWidth, BoundsHitReaction.SCALE_SCROLL)
            .setBackground(settings.showLineColor() && stop.getTrainData().hasColor() ? (0xFF << 24) | (stop.getTrainData().getColor() & 0x00FFFFFF) : 0, false)
        ;
        if (settings.showLineColor() && stop.getTrainData().hasColor()) {
            trainNameLabel
                .setBackground((0xFF << 24) | (stop.getTrainData().getColor() & 0x00FFFFFF), false)
                .setColor(ColorUtils.brightnessDependingFontColor(stop.getTrainData().getColor(), LIGHT_FONT_COLOR, DARK_FONT_COLOR))
            ;
        } else {
            trainNameLabel
                .setBackground(0, false)
                .setColor((0xFF << 24) | (settings.getFontColor() & 0x00FFFFFF))
            ;
        }

        this.platformLabel
            .setText(TextUtils.text(stop.getStationData().getStationInfo().platform()).withStyle(ChatFormatting.BOLD))
        ;

        float x = 5 + Math.max(trainNameLabel.getMaxWidth(), Math.max(timeLabel.getTextWidth(), realTimeLabel.getTextWidth()));
        float platformWidth = Math.min(blockEntity.getXSizeScaled() * 16 - 3 - x, settings.isAutoPlatformWidthNextStop() ? platformLabel.getTextWidth() : settings.getPlatformWidthNextStop());
        this.platformLabel
            .setText(TextUtils.text(stop.getStationData().getStationInfo().platform()).withStyle(ChatFormatting.BOLD))
            .setPos(blockEntity.getXSizeScaled() * 16 - 3 - Math.min(platformWidth, platformLabel.getTextWidth()), 3)
            .setMaxWidth(platformWidth, BoundsHitReaction.SCALE_SCROLL)
        ;

        float w = blockEntity.getXSizeScaled() * 16 - 5 - platformWidth - x;
        focusArea[LineComponent.DESTINATION.i()]
            .setText(isLast ?
                CustomLanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.arrival") :
                TextUtils.text(stop.getStationData().getDestination()))
            .setPos(x, 8.5f)
            .setMaxWidth(w, BoundsHitReaction.SCALE_SCROLL)
        ;

        focusArea[LineComponent.STOPOVERS.i()]
            .setText(isLast ?
                CustomLanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".schedule_board.train_from", stop.getFirstStopName()) :
                TextUtils.concat(TextUtils.text(" \u25CF "), stop.getStopovers().stream().map(a -> (Component)TextUtils.text(a)).toList()))
            .setPos(x, 6)
            .setMaxWidth(w, BoundsHitReaction.SCALE_SCROLL)
        ;

        statusLabel
            .setText(infoLineText)
            .setPos(x, 2.5f)
            .setMaxWidth(w, BoundsHitReaction.SCALE_SCROLL)
            .setColor(ColorUtils.brightnessDependingFontColor(getDisplaySettings(blockEntity).getFontColor(), LIGHT_FONT_COLOR, DARK_FONT_COLOR))
        ;
    }

    private void updateTableContent(AdvancedDisplayBlockEntity blockEntity, StationDisplayData stop, int index) {
        PlatformDisplayFocusSettings settings = getDisplaySettings(blockEntity);
        boolean isLast = (settings.showArrival() && stop.isLastStop()) || stop.isNextSectionExcluded();

        BERLabel[] components = lines[index];
        components[LineComponent.TIME.i()]
            .setText(TextUtils.text(ModUtils.formatTime(stop.getScheduledTime(), getDisplaySettings(blockEntity).getTimeDisplay() == ETimeDisplay.ETA)))
        ;
        components[LineComponent.REAL_TIME.i()]
            .setText(TextUtils.text(stop.getTrainData().isCancelled() ?
                " \u274C " : // X
                (stop.getStationData().isDepartureDelayed() ?
                    (ModUtils.formatTime(isLast ? stop.getStationData().getRealTimeArrivalTime() : stop.getStationData().getRealTimeDepartureTime(), getDisplaySettings(blockEntity).getTimeDisplay() == ETimeDisplay.ETA)) : 
                    ""))) // Nothing (not delayed)
            .setColor(ColorUtils.brightnessDependingFontColor(getDisplaySettings(blockEntity).getFontColor(), LIGHT_FONT_COLOR, DARK_FONT_COLOR))
        ;
        BERLabel trainNameLabel = components[LineComponent.TRAIN_NAME.i()]
            .setText(TextUtils.text(stop.getTrainData().getName()))
            .setBackground(settings.showLineColor() && stop.getTrainData().hasColor() ? (0xFF << 24) | (stop.getTrainData().getColor() & 0x00FFFFFF) : 0, false)
        ;
        if (settings.showLineColor() && stop.getTrainData().hasColor()) {
            trainNameLabel
                .setBackground((0xFF << 24) | (stop.getTrainData().getColor() & 0x00FFFFFF), false)
                .setColor(ColorUtils.brightnessDependingFontColor(stop.getTrainData().getColor(), LIGHT_FONT_COLOR, DARK_FONT_COLOR))
            ;
        } else {
            trainNameLabel
                .setBackground(0, false)
                .setColor((0xFF << 24) | (settings.getFontColor() & 0x00FFFFFF))
            ;
        }
        components[LineComponent.PLATFORM.i()]
            .setText(blockEntity.isPlatformFixed() ?
                TextUtils.empty() :
                TextUtils.text(stop.getStationData().getStationInfo().platform()))
        ;
        components[LineComponent.DESTINATION.i()]
            .setText(isLast ?
                CustomLanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".schedule_board.train_from", stop.getFirstStopName()) :
                TextUtils.text(stop.getStationData().getDestination()))
        ;

        int x = 3;
        components[LineComponent.TIME.i()].setPos(x, 11 + 3 + index * LINE_HEIGHT);
        x += components[LineComponent.TIME.i()].getTextWidth() + 2;
        components[LineComponent.REAL_TIME.i()].setPos(x, 11 + 3 + index * LINE_HEIGHT);
        x += components[LineComponent.REAL_TIME.i()].getTextWidth() + 2 + (!components[LineComponent.REAL_TIME.i()].getText().getString().isEmpty() ? 2 : 0);

        float trainNameWidth = settings.isAutoTrainNameWidth() ? trainNameLabel.getTextWidth() : settings.getTrainNameWidth();
        trainNameLabel
            .setPos(x, 11 + 3 + index * LINE_HEIGHT)
            .setMaxWidth(trainNameWidth, BoundsHitReaction.SCALE_SCROLL)
        ;
        x += trainNameLabel.getMaxWidth() + 2;
        
        
        BERLabel platformLabel = components[LineComponent.PLATFORM.i()];
        float platformWidth = settings.isAutoPlatformWidth() ? platformLabel.getTextWidth() : settings.getPlatformWidth();
        platformLabel
            .setPos(blockEntity.getXSizeScaled() * 16 - 3 - platformLabel.getTextWidth(), 11 + 3 + index * LINE_HEIGHT)
            .setMaxWidth(platformWidth, BoundsHitReaction.SCALE_SCROLL)
        ;
        components[LineComponent.DESTINATION.i()]
            .setPos(x, 11 + 3 + index * LINE_HEIGHT)
            .setMaxWidth(blockEntity.getXSizeScaled() * 16 - 3 - x - platformWidth - 3, BoundsHitReaction.SCALE_SCROLL)
        ;
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
