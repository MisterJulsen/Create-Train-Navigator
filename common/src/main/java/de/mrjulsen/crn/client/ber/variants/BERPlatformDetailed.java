package de.mrjulsen.crn.client.ber.variants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity.EUpdateReason;
import de.mrjulsen.crn.block.display.properties.components.ITrainStopTypeSetting;
import de.mrjulsen.crn.block.properties.ETimeDisplay;
import de.mrjulsen.crn.block.display.properties.PlatformDisplayTableSettings;
import de.mrjulsen.crn.client.ber.AdvancedDisplayRenderInstance;
import de.mrjulsen.crn.client.lang.CustomLanguage;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.train.ETrainStopState;
import de.mrjulsen.crn.data.train.TrainStatus.CompiledTrainStatus;
import de.mrjulsen.crn.data.train.portable.StationDisplayData;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.ber.BERGraphics;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel.EScrollMode;
import de.mrjulsen.mcdragonlib.client.gui.widgets.richtext.PaddingF;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.Pair;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Point;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.mcdragonlib.util.time.ConfiguredTimeSystem;
import de.mrjulsen.mcdragonlib.util.time.DLTime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BERPlatformDetailed implements AbstractAdvancedDisplayRenderer<PlatformDisplayTableSettings> {

    private final MutableComponent textTrainTerminatesHere = CustomLanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.train_terminates");
    private static final String keyTime = "gui." + CreateRailwaysNavigator.MOD_ID + ".time";

    private static final float LINE_HEIGHT = 5.4f;

    private boolean showInfoLine = false;
    private MutableComponent infoLineText = TextUtils.empty();
    private int maxLines = 0;
    
    private final BERLabel timeLabel = new BERLabel();
    private final BERLabel statusLabel = new BERLabel();    
    private BERLabel[][] lines = new BERLabel[0][];

    public BERPlatformDetailed() {
        timeLabel.text.set(TextUtils.empty());
        timeLabel.horizontalAlign.set(ETextAlignment.CENTER);
        timeLabel.horizontalScale.set(Pair.of(0.4f, 0.4f));
        timeLabel.verticalScale.set(Pair.of(0.4f, 0.4f));
        
        statusLabel.text.set(TextUtils.empty());
        statusLabel.horizontalAlign.set(ETextAlignment.CENTER);
        statusLabel.horizontalScale.set(Pair.of(0.4f, 0.4f));
        statusLabel.verticalScale.set(Pair.of(0.4f, 0.4f));
        statusLabel.backgroundColor.set(DLColor.WHITE);
        statusLabel.color.set(DARK_FONT_COLOR);
        statusLabel.fullBackground.set(true);
        statusLabel.horizontalScrollingSpeed.set(SCROLLING_SPEED);
        statusLabel.backgroundPadding.set(new PaddingF(0.5f));
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent) {
        timeLabel.text.set(blockEntity.getXSize() > 1
                ? CustomLanguage.translate(keyTime, ModUtils.formatTime(DragonLib.getCurrentWorldTime(), false))
                : TextUtils.text(ModUtils.formatTime(DragonLib.getCurrentWorldTime(), false))
            ) 
        ;

        timeLabel.clippingArea.set(Rectangle.withSize(2, 2, blockEntity.getXSizeScaled() * 16 - 4, blockEntity.getYSizeScaled() * 16 - 4));
        timeLabel.glowing.set(blockEntity.isGlowing());
        statusLabel.clippingArea.set(Rectangle.withSize(2, 2, blockEntity.getXSizeScaled() * 16 - 4, blockEntity.getYSizeScaled() * 16 - 4));
        statusLabel.glowing.set(blockEntity.isGlowing());

        for (BERLabel[] lbls : lines) {
            for (BERLabel lbl : lbls) {
                lbl.glowing.set(blockEntity.isGlowing());
                lbl.clippingArea.set(Rectangle.withSize(2, 2, blockEntity.getXSizeScaled() * 16 - 4, blockEntity.getYSizeScaled() * 16 - 4));
            }
        }
    }
    
    @Override
    public void render(BERGraphics<AdvancedDisplayBlockEntity> graphics, float pPartialTicks, AdvancedDisplayRenderInstance parent, int light, boolean backSide) {
        for (int i = 0; i < lines.length && i < maxLines; i++) {
            for (int k = 0; k < lines[i].length; k++) {
                if (getDisplaySettings(graphics.blockEntity()).showTimeAndDate() && i >= maxLines - 1 && (DragonLib.getCurrentWorldTime() % 200 > 100)) {
                    timeLabel.render(graphics);
                    continue;
                }
                lines[i][k].render(graphics);
            }
        }

        if (getDisplaySettings(graphics.blockEntity()).showTimeAndDate() && lines.length < maxLines) {            
            timeLabel.render(graphics);
        }

        if (showInfoLine) {
            statusLabel.render(graphics);
        }
    }

    @Override
    public void update(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent, EUpdateReason reason) {
        List<StationDisplayData> preds = new ArrayList<>();
        
        for (int i = 0; i < blockEntity.getStops().size(); i++) {
            StationDisplayData data = blockEntity.getStops().get(i);
            boolean shouldShow = i == 0 || data.getStationData().getRealTimeArrivalTime() < DragonLib.getCurrentWorldTime() + ModClientConfig.DISPLAY_LEAD_TIME.get();
            boolean cancelled = data.getTrainData().isCancelled();
            boolean isStillValid = DragonLib.getCurrentWorldTime() < data.getStationData().getScheduledDepartureTime() + ModClientConfig.DISPLAY_LEAD_TIME.get();
            boolean terminus = data.isNextSectionExcluded();
            boolean start = data.isPrevSectionExcluded();

            ITrainStopTypeSetting.ETrainStopType type = getDisplaySettings(blockEntity).getTrainStopType();
            boolean showArrival = type.showArrivals(terminus) && !start;
            boolean showDeparture = type.showDepartures(start) && !terminus;
            boolean allowed = showArrival || showDeparture;

            if (allowed && shouldShow && (!cancelled || isStillValid)) {
                preds.add(data);
            }
        }

        showInfoLine = !preds.isEmpty() && ((preds.get(0).getStationData().isDepartureDelayed() && preds.get(0).getTrainData().hasStatusInfo()) || preds.get(0).getStationData().isStationChanged() || preds.get(0).isNextSectionExcluded());
        if (showInfoLine) {
            // Update status label
            this.infoLineText = TextUtils.concat(TextUtils.text("  +++  "), preds.stream().limit(maxLines).filter(x -> 
                (x.getTrainData().hasStatusInfo() &&
                x.getStationData().isDepartureDelayed()) ||
                x.getStationData().isStationChanged() || 
                x.isNextSectionExcluded()
            ).map(x -> {
                Collection<Component> content = new ArrayList<>();
                if (x.getTrainData().isCancelled()) {
                    return CustomLanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.cancelled");
                }

                // TRAIN TERMINATES
                if (x.isNextSectionExcluded()) {
                    content.add(textTrainTerminatesHere);
                }

                // DELAYED
                if (x.getStationData().isDepartureDelayed()) {
                    String delay = getDisplaySettings(blockEntity).getTimeDisplay() == ETimeDisplay.ETA ? ModUtils.timeRemainingString(x.getStationData().getDepartureTimeDeviation()) : String.valueOf((long)DLTime.fromTicks(x.getStationData().getDepartureTimeDeviation(), new ConfiguredTimeSystem()).toGameMinutes());
                    MutableComponent delayComponent = CustomLanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.delayed", delay);
                    if (getDisplaySettings(blockEntity).getTimeDisplay() == ETimeDisplay.ABS) {
                        delayComponent.append(" ").append(CustomLanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.delay_abs_suffix"));
                    }
                    content.add(delayComponent);
                }
                
                // PLATFORM CHANGED
                if (x.getStationData().isStationChanged()) {
                    if (!x.getStationData().getScheduledStation().tagId().equals(x.getStationData().getRealTimeStation().tagId())) {
                        content.add(CustomLanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.platform_and_station_changed", x.getStationData().getRealTimeStation().tagName(), x.getStationData().getRealTimeStation().info().platform()));
                    } else {
                        content.add(CustomLanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.platform_changed", x.getStationData().getRealTimeStation().info().platform()));
                    }
                }

                // STATUS
                for (CompiledTrainStatus status : x.getTrainData().getStatus()) {
                    content.add(status.text());
                }
                return CustomLanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.information_about_train", x.getTrainData().getName(ETrainStopState.DEPARTURE))
                    .append(TextUtils.text(": "))
                    .append(TextUtils.concat(TextUtils.text(" - "), content));
            }).toArray(Component[]::new));
        } else {
            infoLineText = TextUtils.empty();
        }

        int defaultMaxLines = blockEntity.getYSizeScaled() * 3 - 1;
        this.maxLines = defaultMaxLines - (showInfoLine ? 1 : 0);        
        int maxIndices = Math.max(0, Math.min(this.maxLines, preds.size()));
        if (reason == EUpdateReason.LAYOUT_CHANGED || this.lines == null || lines.length != maxIndices) {
            updateLayout(blockEntity, preds, maxIndices);
        }
            
        for (int i = 0; i < this.lines.length; i++) {
            StationDisplayData stop = preds.get(i);
            updateContent(blockEntity, stop, i);
        }

        statusLabel.text.set(infoLineText);
        statusLabel.position.set(Point.of(3, blockEntity.getYSizeScaled() * 16 - 12 * statusLabel.verticalMaxScale.get() - 2));
        statusLabel.preferredWidth.set((float)statusLabel.clippingArea.get().width() - 2);
        statusLabel.preferredHeight.set(Minecraft.getInstance().font.lineHeight * statusLabel.verticalMaxScale.get());
        statusLabel.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
        
        timeLabel.position.set(Point.of(3, 3 + (Math.min(lines.length, maxLines) - (lines.length < maxLines ? 0 : 1)) * LINE_HEIGHT));
    }

    private void updateLayout(AdvancedDisplayBlockEntity blockEntity, List<StationDisplayData> preds, int maxIndices) {
        this.lines = new BERLabel[maxIndices][];
        for (int i = 0; i < this.lines.length; i++) {
            StationDisplayData stop = preds.get(i);
            this.lines[i] = createLine(blockEntity, stop, i);
            updateContent(blockEntity, stop, i);
        }
        statusLabel.backgroundColor.set(getDisplaySettings(blockEntity).getFontColor());
        statusLabel.color.set(DLColor.pickBasedOnBrightness(getDisplaySettings(blockEntity).getFontColor(), LIGHT_FONT_COLOR, DARK_FONT_COLOR, 0.5f));

        timeLabel.preferredWidth.set((float)timeLabel.clippingArea.get().width());
        timeLabel.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
        timeLabel.color.set(getDisplaySettings(blockEntity).getFontColor());
    }

    private void updateContent(AdvancedDisplayBlockEntity blockEntity, StationDisplayData stop, int index) {
        PlatformDisplayTableSettings settings = getDisplaySettings(blockEntity);
        ETrainStopState stopState = ITrainStopTypeSetting.resolveStopState(stop, settings);

        BERLabel[] components = lines[index];
        Component scheduledTimeFormatted = TextUtils.text(ModUtils.formatTime(
                stopState == ETrainStopState.ARRIVAL ?
                        stop.getStationData().getScheduledArrivalTime() :
                        stop.getStationData().getScheduledDepartureTime(),
                settings.getTimeDisplay() == ETimeDisplay.ETA
        ));
        Component realTimeFormatted = TextUtils.text(ModUtils.formatTime(
                stopState == ETrainStopState.ARRIVAL ?
                        stop.getStationData().getRealTimeArrivalTime() :
                        stop.getStationData().getRealTimeDepartureTime(),
                settings.getTimeDisplay() == ETimeDisplay.ETA
        ));

        BERLabel timeComponent = components[LineComponent.TIME.i()];
        timeComponent.text.set(scheduledTimeFormatted);

        BERLabel realTimeComponent = components[LineComponent.REAL_TIME.i()];
        if (stop.getTrainData().isCancelled()) {
            realTimeComponent.text.set(TextUtils.text(" \u274C ")); // X
        } else if (stop.getStationData().isDepartureDelayed()) {
            realTimeComponent.text.set(realTimeFormatted);
        } else {
            realTimeComponent.text.set(TextUtils.empty());
        }
        realTimeComponent.color.set(DLColor.pickBasedOnBrightness(getDisplaySettings(blockEntity).getFontColor(), LIGHT_FONT_COLOR, DARK_FONT_COLOR, 0.5f));

        BERLabel trainNameComponent = components[LineComponent.TRAIN_NAME.i()];
        trainNameComponent.text.set(TextUtils.text(stop.getTrainData().getName(stopState)));

        if (settings.showLineColor() && stop.getTrainData().hasColor(stopState)) {
            trainNameComponent.backgroundColor.set(stop.getTrainData().getColor(stopState));
            trainNameComponent.color.set(DLColor.pickBasedOnBrightness(stop.getTrainData().getColor(stopState), LIGHT_FONT_COLOR, DARK_FONT_COLOR, 0.5f));
        } else {
            trainNameComponent.backgroundColor.set(DLColor.TRANSPARENT);
            trainNameComponent.color.set(settings.getFontColor());
        }

        BERLabel platformComponent = components[LineComponent.PLATFORM.i()];
        platformComponent.text.set(TextUtils.text(stop.getStationData().getRealTimeStation().info().platform()));

        BERLabel destinationComponent = components[LineComponent.DESTINATION.i()];
        destinationComponent.text.set(stopState == ETrainStopState.ARRIVAL ?
            CustomLanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".schedule_board.train_from", stop.getFirstStopName()) :
            TextUtils.text(stop.getStationData().getDestination())
        );

        int x = 3;
        timeComponent.position.set(Point.of(x, 3 + index * LINE_HEIGHT));
        x += timeComponent.getRenderedWidth() + 2;
        realTimeComponent.position.set(Point.of(x, 3 + index * LINE_HEIGHT));
        x += realTimeComponent.getRenderedWidth() + 2 + (realTimeComponent.text.get().getString().isEmpty() ? 0 : 2);
        trainNameComponent.position.set(Point.of(x, 3 + index * LINE_HEIGHT));

        float trainNameWidth = Math.min(settings.isAutoTrainNameWidth() ? trainNameComponent.getRenderedWidth() : settings.getTrainNameWidth(), (int)blockEntity.getXSizeScaled() * 16 - 3 - trainNameComponent.x.get());
        trainNameComponent.preferredWidth.set(trainNameWidth);
        trainNameComponent.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
        x += trainNameWidth + 2;

        float platformWidth = settings.isAutoPlatformWidth() ? platformComponent.getRenderedWidth() : settings.getPlatformWidth();
        platformComponent.position.set(Point.of(blockEntity.getXSizeScaled() * 16 - 3 - platformWidth, 3 + index * LINE_HEIGHT));
        platformComponent.preferredWidth.set(platformWidth);
        platformComponent.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);

        if (stop.getStationData().isStationChanged()) {
            platformComponent.backgroundColor.set(getDisplaySettings(blockEntity).getFontColor());
            platformComponent.color.set(DARK_FONT_COLOR);
        } else {
            platformComponent.backgroundColor.set(DLColor.TRANSPARENT);
            platformComponent.color.set(getDisplaySettings(blockEntity).getFontColor());
        }

        destinationComponent.position.set(Point.of(x, 3 + index * LINE_HEIGHT));
        destinationComponent.preferredWidth.set(blockEntity.getXSizeScaled() * 16 - 3 - x - platformWidth - 3);
        destinationComponent.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
    }

    private BERLabel[] createLine(AdvancedDisplayBlockEntity blockEntity, StationDisplayData stop, int index) {
        BERLabel[] components = new BERLabel[LineComponent.values().length];

        BERLabel timeComponent = components[LineComponent.TIME.i()] = new BERLabel();        
        timeComponent.clippingArea.set(Rectangle.withSize(2, 2, blockEntity.getXSizeScaled() * 16 - 4, blockEntity.getYSizeScaled() * 16 - 4));
        timeComponent.verticalScale.set(Pair.of(0.4f, 0.4f));
        timeComponent.horizontalScale.set(Pair.of(0.2f, 0.4f));
        timeComponent.preferredWidth.set(12f);
        timeComponent.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
        timeComponent.color.set(getDisplaySettings(blockEntity).getFontColor());
        
        BERLabel realTimeComponent = components[LineComponent.REAL_TIME.i()] = new BERLabel();
        realTimeComponent.clippingArea.set(Rectangle.withSize(2, 2, blockEntity.getXSizeScaled() * 16 - 4, blockEntity.getYSizeScaled() * 16 - 4));
        realTimeComponent.verticalScale.set(Pair.of(0.4f, 0.4f));
        realTimeComponent.horizontalScale.set(Pair.of(0.2f, 0.4f));
        realTimeComponent.preferredWidth.set(12f);
        realTimeComponent.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
        realTimeComponent.backgroundColor.set(getDisplaySettings(blockEntity).getFontColor());
        realTimeComponent.color.set(DARK_FONT_COLOR);
        realTimeComponent.backgroundPadding.set(new PaddingF(0.5f));
                
        BERLabel trainNameComponent = components[LineComponent.TRAIN_NAME.i()] = new BERLabel();
        trainNameComponent.clippingArea.set(Rectangle.withSize(2, 2, blockEntity.getXSizeScaled() * 16 - 4, blockEntity.getYSizeScaled() * 16 - 4));
        trainNameComponent.verticalScale.set(Pair.of(0.4f, 0.4f));
        trainNameComponent.horizontalScale.set(Pair.of(0.2f, 0.4f));
        trainNameComponent.horizontalScrollingSpeed.set(SCROLLING_SPEED);
        trainNameComponent.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
        trainNameComponent.color.set(getDisplaySettings(blockEntity).getFontColor());
        trainNameComponent.backgroundPadding.set(new PaddingF(0.5f));
        
        BERLabel platformComponent = components[LineComponent.PLATFORM.i()] = new BERLabel();
        platformComponent.clippingArea.set(Rectangle.withSize(2, 2, blockEntity.getXSizeScaled() * 16 - 4, blockEntity.getYSizeScaled() * 16 - 4));
        platformComponent.verticalScale.set(Pair.of(0.4f, 0.4f));
        platformComponent.horizontalScale.set(Pair.of(0.2f, 0.4f));
        platformComponent.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
        platformComponent.horizontalAlign.set(ETextAlignment.RIGHT);
        platformComponent.color.set(getDisplaySettings(blockEntity).getFontColor());
        platformComponent.backgroundPadding.set(new PaddingF(0.5f));
        
        BERLabel destinationComponent = components[LineComponent.DESTINATION.i()] = new BERLabel();
        destinationComponent.clippingArea.set(Rectangle.withSize(2, 2, blockEntity.getXSizeScaled() * 16 - 4, blockEntity.getYSizeScaled() * 16 - 4));
        destinationComponent.verticalScale.set(Pair.of(0.4f, 0.4f));
        destinationComponent.horizontalScale.set(Pair.of(0.2f, 0.4f));
        destinationComponent.horizontalScrollingSpeed.set(SCROLLING_SPEED);
        destinationComponent.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
        destinationComponent.color.set(getDisplaySettings(blockEntity).getFontColor());

        return components;
    }

    private static enum LineComponent {
        TIME(0),
        REAL_TIME(1),
        TRAIN_NAME(2),
        DESTINATION(3),
        PLATFORM(4);

        int index;
        LineComponent(int index) {
            this.index = index;
        }
        public int i() {
            return index;
        }
    }
}
