package de.mrjulsen.crn.client.ber.variants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import de.mrjulsen.crn.block.display.properties.components.ITrainStopTypeSetting;
import de.mrjulsen.crn.data.train.ETrainStopState;
import de.mrjulsen.mcdragonlib.util.math.Size;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.joml.Vector3f;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity.EUpdateReason;
import de.mrjulsen.crn.block.properties.ETimeDisplay;
import de.mrjulsen.crn.block.display.properties.DepartureBoardDisplayTableSettings;
import de.mrjulsen.crn.client.ber.AdvancedDisplayRenderInstance;
import de.mrjulsen.crn.client.lang.CustomLanguage;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.train.TrainStatus.CompiledTrainStatus;
import de.mrjulsen.crn.data.train.portable.StationDisplayData;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.ber.BERGraphics;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel.EScrollMode;
import de.mrjulsen.mcdragonlib.client.gui.widgets.richtext.PaddingF;
import de.mrjulsen.mcdragonlib.client.util.RenderUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.Pair;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Point;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.mcdragonlib.util.time.ConfiguredTimeSystem;
import de.mrjulsen.mcdragonlib.util.time.DLTime;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class BERDepartureBoardTable implements AbstractAdvancedDisplayRenderer<DepartureBoardDisplayTableSettings> {

    private final MutableComponent textTrainTerminates = CustomLanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.train_terminates");
    private static final String keyDeparture = "gui.createrailwaysnavigator.departure";
    private static final String keyTrain = "gui.createrailwaysnavigator.line";
    private static final String keyDestination = "gui.createrailwaysnavigator.destination";
    private static final String keyPlatform = "gui.createrailwaysnavigator.platform";
    private static final String keyVia = "gui.createrailwaysnavigator.via";
    private static final String keyTooSmall = "gui.createrailwaysnavigator.too_small";

    private static final int MIN_SIZE = 4;
    private static final float LINE_HEIGHT = 5.4f;    
    private static final float Y_OFFSET = LINE_HEIGHT; // Headline    
    private static final float SPACING = 2;
    private static final float TIME_LABEL_MAX_WIDTH = 12;
    private static final float REAL_TIME_LABEL_MAX_WIDTH = 12;

    private boolean showInfoLine = false;
    private MutableComponent infoLineText = TextUtils.empty();
    private int maxLines = 0;
    
    private final BERLabel statusLabel = new BERLabel();        
    private final BERLabel tooSmallLabel = new BERLabel();
    private BERLabel[][] lines = new BERLabel[0][];
    private final BERLabel[] headlines;

    public BERDepartureBoardTable() {
        statusLabel.horizontalScale.set(Pair.of(0.4f, 0.4f));
        statusLabel.verticalScale.set(Pair.of(0.4f, 0.4f));
        statusLabel.horizontalAlign.set(ETextAlignment.CENTER);
        statusLabel.color.set(DARK_FONT_COLOR);
        statusLabel.backgroundColor.set(DLColor.WHITE);
        statusLabel.fullBackground.set(true);
        statusLabel.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
        statusLabel.horizontalScrollingSpeed.set(SCROLLING_SPEED);
        statusLabel.backgroundPadding.set(new PaddingF(0.5f));
        statusLabel.preferredHeight.set(Minecraft.getInstance().font.lineHeight * statusLabel.verticalMaxScale.get());

        tooSmallLabel.text.set(TextUtils.translate(keyTooSmall));
        tooSmallLabel.horizontalScale.set(Pair.of(0.4f, 0.4f));
        tooSmallLabel.verticalScale.set(Pair.of(0.4f, 0.4f));
        tooSmallLabel.position.set(Point.of(3, 3));
        headlines = new BERLabel[LineComponent.values().length];

        BERLabel timeLabel = headlines[LineComponent.TIME.i()] = new BERLabel();
        timeLabel.text.set(CustomLanguage.translate(keyDeparture).withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.ITALIC));
        timeLabel.horizontalScale.set(Pair.of(0.2f, 0.4f));
        timeLabel.verticalScale.set(Pair.of(0.4f, 0.4f));
        timeLabel.preferredWidth.set(0f);
        timeLabel.position.set(Point.of(3, 3));
        
        BERLabel trainNameLabel = headlines[LineComponent.TRAIN_NAME.i()] = new BERLabel();
        trainNameLabel.text.set(CustomLanguage.translate(keyTrain).withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.ITALIC));
        trainNameLabel.horizontalScale.set(Pair.of(0.2f, 0.4f));
        trainNameLabel.verticalScale.set(Pair.of(0.4f, 0.4f));
        trainNameLabel.preferredWidth.set(0f);
        trainNameLabel.position.set(Point.of(3, 3));
        
        BERLabel platformLabel = headlines[LineComponent.PLATFORM.i()] = new BERLabel();
        platformLabel.text.set(CustomLanguage.translate(keyPlatform).withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.ITALIC));
        platformLabel.horizontalScale.set(Pair.of(0.2f, 0.4f));
        platformLabel.verticalScale.set(Pair.of(0.4f, 0.4f));
        platformLabel.preferredWidth.set(0f);
        platformLabel.position.set(Point.of(3, 3));

        BERLabel destinationLabel = headlines[LineComponent.DESTINATION.i()] = new BERLabel();
        destinationLabel.text.set(CustomLanguage.translate(keyDestination).withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.ITALIC));
        destinationLabel.horizontalScale.set(Pair.of(0.2f, 0.4f));
        destinationLabel.verticalScale.set(Pair.of(0.4f, 0.4f));
        destinationLabel.preferredWidth.set(0f);
        destinationLabel.position.set(Point.of(3, 3));
        
        BERLabel stopoversLabel = headlines[LineComponent.STOPOVERS.i()] = new BERLabel();
        stopoversLabel.text.set(CustomLanguage.translate(keyVia).withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.ITALIC));
        stopoversLabel.horizontalScale.set(Pair.of(0.2f, 0.4f));
        stopoversLabel.verticalScale.set(Pair.of(0.4f, 0.4f));
        stopoversLabel.preferredWidth.set(0f);
        stopoversLabel.position.set(Point.of(3, 3));
        
        BERLabel infoLabel = headlines[LineComponent.INFO.i()] = new BERLabel();
        infoLabel.horizontalScale.set(Pair.of(0.2f, 0.4f));
        infoLabel.verticalScale.set(Pair.of(0.4f, 0.4f));
        infoLabel.preferredWidth.set(0f);
        infoLabel.position.set(Point.of(3, 3));
        infoLabel.fullBackground.set(true);
        infoLabel.backgroundPadding.set(new PaddingF(0.5f));
    }
    
    @Override
    public void render(BERGraphics<AdvancedDisplayBlockEntity> graphics, float pPartialTicks, AdvancedDisplayRenderInstance parent, int light, boolean backSide) {
        RenderUtils.fillColor(
            graphics,
            new Vector3f(2, 1.5f + LINE_HEIGHT, 0.0f),
            graphics.blockEntity().getXSizeScaled() * 16 - 4, 0.25f,
            getDisplaySettings(graphics.blockEntity()).getFontColor(),
            graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING)
        );

        if (graphics.blockEntity().getXSizeScaled() < MIN_SIZE) {
            tooSmallLabel.render(graphics);
            return;
        }

        for (int k = 0; k < headlines.length; k++) {
            DLUtils.doIfNotNull(headlines[k], y -> y.render(graphics));
        }

        for (int i = 0; i < lines.length && i < maxLines; i++) {
            graphics.poseStack().pushPose();
            if (i % 2 == 1) {
                RenderUtils.fillColor(
                    graphics,
                    new Vector3f(2, 2 + Y_OFFSET + i * LINE_HEIGHT, 0),
                    graphics.blockEntity().getXSizeScaled() * 16 - 4, LINE_HEIGHT,
                    getDisplaySettings(graphics.blockEntity()).getFontColor().withAlpha(40),
                    graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING),
                    graphics.blockEntity().isGlowing() ? LightTexture.FULL_BRIGHT : light,
                    false
                );
                graphics.poseStack().translate(0, 0, 0.05f);
            }
            for (int k = 0; k < lines[i].length; k++) {
                DLUtils.doIfNotNull(lines[i][k], x -> x.render(graphics));
            }
            graphics.poseStack().popPose();
        }

        if (showInfoLine) {
            statusLabel.render(graphics);
        }
    }

    private Optional<Component> getStatusInfo(AdvancedDisplayBlockEntity blockEntity, StationDisplayData data, boolean singleTrain) {
        if (!((data.getTrainData().hasStatusInfo() && data.getStationData().isDepartureDelayed()) || data.getStationData().isStationChanged() || data.isNextSectionExcluded())) {
            return Optional.empty();
        }
        Collection<Component> content = new ArrayList<>();
        if (data.getTrainData().isCancelled()) {
            content.add(CustomLanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.cancelled"));
        }
        // TRAIN TERMINATES
        if (data.isNextSectionExcluded()) {
            content.add(textTrainTerminates);
        }
        // DELAYED
        if (data.getStationData().isDepartureDelayed()) {            
            String delay = getDisplaySettings(blockEntity).getTimeDisplay() == ETimeDisplay.ETA ? ModUtils.timeRemainingString(data.getStationData().getDepartureTimeDeviation()) : String.valueOf((long)DLTime.fromTicks(data.getStationData().getDepartureTimeDeviation(), new ConfiguredTimeSystem()).toGameMinutes());
            MutableComponent delayComponent = CustomLanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.delayed", delay);
            if (getDisplaySettings(blockEntity).getTimeDisplay() == ETimeDisplay.ABS) {
                delayComponent.append(" ").append(CustomLanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.delay_abs_suffix"));
            }
            content.add(delayComponent);
        }       
        // PLATFORM CHANGED
        if (data.getStationData().isStationChanged()) {
            if (!data.getStationData().getScheduledStation().tagId().equals(data.getStationData().getRealTimeStation().tagId())) {
                content.add(CustomLanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.platform_and_station_changed", data.getStationData().getRealTimeStation().tagName(), data.getStationData().getRealTimeStation().info().platform()));
            } else {
                content.add(CustomLanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.platform_changed", data.getStationData().getRealTimeStation().info().platform()));
            }
        }
        // STATUS
        for (CompiledTrainStatus status : data.getTrainData().getStatus()) {
            content.add(status.text());
        }
        if (singleTrain) {
            return Optional.ofNullable(TextUtils.concat(content));
        } else {            
            return Optional.of(CustomLanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.information_about_train", data.getTrainData().getName(ETrainStopState.DEPARTURE))
                .append(": ")
                .append(TextUtils.concat(TextUtils.text(" - "), content))
            );
        }
    }

    @Override
    public void update(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent, EUpdateReason reason) {
        List<StationDisplayData> preds = blockEntity.getStops().stream().filter(x -> {
            boolean cancelled = x.getTrainData().isCancelled();
            boolean isStillValid = DragonLib.getCurrentWorldTime() < x.getStationData().getScheduledDepartureTime() + ModClientConfig.DISPLAY_LEAD_TIME.get();
            boolean terminus = x.isNextSectionExcluded();
            boolean start = x.isPrevSectionExcluded();

            ITrainStopTypeSetting.ETrainStopType type = getDisplaySettings(blockEntity).getTrainStopType();
            boolean showArrival = type.showArrivals(terminus) && !start;
            boolean showDeparture = type.showDepartures(start) && !terminus;

            boolean allowed = showArrival || showDeparture;
            return allowed && (!cancelled || isStillValid);
        }).toList();
        
        MutableBoolean shouldShowLine = new MutableBoolean(false);
        this.infoLineText = TextUtils.concat(TextUtils.text("  +++  "), preds.stream().limit(maxLines).filter(x -> 
            (x.getTrainData().hasStatusInfo() && x.getStationData().isDepartureDelayed()) ||
            x.getStationData().isStationChanged()
        ).flatMap(x -> {
            shouldShowLine.setTrue();
            return getStatusInfo(blockEntity, x, false).stream();
        }).toArray(Component[]::new));

        this.showInfoLine = shouldShowLine.isTrue();
        if (!showInfoLine) {
            infoLineText = TextUtils.empty();
        }

        int defaultMaxLines = blockEntity.getYSizeScaled() * 3 - 2;
        this.maxLines = defaultMaxLines - (showInfoLine ? 1 : 0);        
        int maxIndices = Math.max(0, Math.min(this.maxLines, preds.size()));
        if (reason == EUpdateReason.LAYOUT_CHANGED || this.lines == null || lines.length != maxIndices) {
            updateLayout(blockEntity, preds, maxIndices);
        }
            
        for (int i = 0; i < this.lines.length && i < preds.size(); i++) {
            StationDisplayData stop = preds.get(i);
            updateContent(blockEntity, stop, i, false);
        }

        statusLabel.text.set(infoLineText);
        statusLabel.position.set(Point.of(3, blockEntity.getYSizeScaled() * 16 - 12 * statusLabel.verticalMaxScale.get() - 2));
        statusLabel.preferredWidth.set((float)(blockEntity.getXSizeScaled() * 16 - 6));
        statusLabel.color.set(DLColor.pickBasedOnBrightness(getDisplaySettings(blockEntity).getFontColor(), LIGHT_FONT_COLOR, DARK_FONT_COLOR, 0.5f));
        
        // Update bounds
        statusLabel.clippingArea.set(Rectangle.withSize(2, 2, blockEntity.getXSizeScaled() * 16 - 4, blockEntity.getYSizeScaled() * 16 - 4));
        statusLabel.glowing.set(blockEntity.isGlowing());
        DLUtils.doIfNotNull(lines, x -> {
            for (int i = 0; i < x.length; i++) {
                BERLabel[] line = x[i];
                if (line == null) continue;
                for (int k = 0; k < line.length; k++) {
                    DLUtils.doIfNotNull(line[k], l -> {
                        l.clippingArea.set(Rectangle.withSize(2, 2, blockEntity.getXSizeScaled() * 16 - 4, blockEntity.getYSizeScaled() * 16 - 4));
                        l.glowing.set(blockEntity.isGlowing());
                    });
                }
            }
        });
        for (int k = 0; k < headlines.length; k++) {                    
            DLUtils.doIfNotNull(headlines[k], l -> {
                l.clippingArea.set(Rectangle.withSize(2, 2, blockEntity.getXSizeScaled() * 16 - 4, blockEntity.getYSizeScaled() * 16 - 4));
                l.glowing.set(blockEntity.isGlowing());
            });
        }
    }


    private void updateLayout(AdvancedDisplayBlockEntity blockEntity, List<StationDisplayData> preds, int maxIndices) {
        DepartureBoardDisplayTableSettings settings = getDisplaySettings(blockEntity);

        if (blockEntity.getXSizeScaled() < MIN_SIZE) {
            tooSmallLabel.clippingArea.set(Rectangle.withSize(2, 2, blockEntity.getXSizeScaled() * 16 - 4, blockEntity.getYSizeScaled() * 16 - 4));
            tooSmallLabel.glowing.set(blockEntity.isGlowing());
            tooSmallLabel.color.set(settings.getFontColor());
            tooSmallLabel.preferredWidth.set((float)tooSmallLabel.clippingArea.get().width());
            return;
        }

        boolean hasStopovers = blockEntity.getXSizeScaled() - 4 >= 4;
        boolean hasInfo = blockEntity.getXSizeScaled() - 4 >= 7;
        
        // Init headline
        BERLabel hTimeLabel = headlines[LineComponent.TIME.i()];
        hTimeLabel.x.set(3f);
        hTimeLabel.preferredWidth.set(TIME_LABEL_MAX_WIDTH + (!isSmall(blockEntity) ? REAL_TIME_LABEL_MAX_WIDTH : 0) + SPACING);
        hTimeLabel.color.set(settings.getFontColor());

        BERLabel hTrainLabel = headlines[LineComponent.TRAIN_NAME.i()];
        hTrainLabel.x.set(hTimeLabel.x.get() + hTimeLabel.preferredWidth.get() + SPACING);
        hTrainLabel.preferredWidth.set((float)settings.getTrainNameWidth());
        hTrainLabel.color.set(settings.getFontColor());
        
        BERLabel hPlatformLabel = headlines[LineComponent.PLATFORM.i()];
        float hPlatformLabelWidth = settings.getPlatformWidth();
        hPlatformLabel.x.set(blockEntity.getXSizeScaled() * 16 - 3 - hPlatformLabelWidth);
        hPlatformLabel.horizontalAlign.set(ETextAlignment.RIGHT);
        hPlatformLabel.preferredWidth.set(hPlatformLabelWidth);
        hPlatformLabel.color.set(settings.getFontColor());
        
        final float remainingSpace = blockEntity.getXSizeScaled() * 16 - 3 - hTrainLabel.x.get() - hTrainLabel.preferredWidth.get() - settings.getPlatformWidth() - SPACING; // No *2!
        final float infoSpace = hasInfo ? (remainingSpace * settings.getInfoWidthPercentage()) - SPACING : 0;
        final float stopoversSpace = hasStopovers ? (remainingSpace * settings.getStopoversWidthPercentage()) - SPACING : 0;
        final float destinationSpace = remainingSpace - infoSpace - stopoversSpace - SPACING * 3;

        BERLabel hStopoversLabel = headlines[LineComponent.STOPOVERS.i()];
        hStopoversLabel.x.set(hasStopovers ? hTrainLabel.x.get() + hTrainLabel.preferredWidth.get() + SPACING : 0);
        hStopoversLabel.preferredWidth.set(stopoversSpace);
        hStopoversLabel.color.set(settings.getFontColor());
        
        BERLabel hDestinationLabel = headlines[LineComponent.DESTINATION.i()];
        hDestinationLabel.x.set(hasStopovers ? hStopoversLabel.x.get() + hStopoversLabel.preferredWidth.get() + SPACING : hTrainLabel.x.get() + hTrainLabel.preferredWidth.get() + SPACING);
        hDestinationLabel.preferredWidth.set(destinationSpace);
        hDestinationLabel.color.set(settings.getFontColor());

        BERLabel hInfoLabel = headlines[LineComponent.INFO.i()];
        hInfoLabel.x.set(hasInfo ? hDestinationLabel.x.get() + hDestinationLabel.preferredWidth.get() + SPACING : 3);
        hInfoLabel.preferredWidth.set(infoSpace);
        hInfoLabel.color.set(settings.getFontColor());    
        hInfoLabel.fullBackground.set(true);


        this.lines = new BERLabel[maxIndices][];
        for (int i = 0; i < this.lines.length; i++) {
            StationDisplayData stop = preds.get(i);
            this.lines[i] = createLine(blockEntity, stop, i, hasStopovers, hasInfo);
            updateContent(blockEntity, stop, i, true);
        }

        statusLabel.backgroundColor.set(settings.getFontColor());
        statusLabel.color.set(DLColor.pickBasedOnBrightness(settings.getFontColor(), LIGHT_FONT_COLOR, DARK_FONT_COLOR, 0.5f));
    }
    

    private void updateContent(AdvancedDisplayBlockEntity blockEntity, StationDisplayData stop, int index, boolean layoutUpdate) {
        DepartureBoardDisplayTableSettings settings = getDisplaySettings(blockEntity);
        ETrainStopState stopState = ITrainStopTypeSetting.resolveStopState(stop, settings);

        boolean showInfoLine = (stop.getStationData().isDepartureDelayed() && stop.getTrainData().hasStatusInfo()) || stop.getStationData().isStationChanged() || stop.isNextSectionExcluded();
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

        BERLabel timeLabel = components[LineComponent.TIME.i()];
        timeLabel.text.set(scheduledTimeFormatted);
        
        BERLabel realTimeLabel = components[LineComponent.REAL_TIME.i()];
        if (stop.getTrainData().isCancelled()) {
            realTimeLabel.text.set(TextUtils.text(" \u274C ")); // X
        } else if (stop.getStationData().isDepartureDelayed()) {
            realTimeLabel.text.set(realTimeFormatted);
        } else {
            realTimeLabel.text.set(TextUtils.empty());
        }
        /*
        realTimeLabel.text.set(isSmall(blockEntity) ? realTimeFormatted :
            TextUtils.text(stop.getTrainData().isCancelled() ?
                " \u274C " : // X
                (stop.getStationData().isDepartureDelayed() ?
                    (ModUtils.formatTime(stop.getRealTime(), settings.getTimeDisplay() == ETimeDisplay.ETA)) : 
                    "")) // Nothing (not delayed)
        );

         */
        realTimeLabel.color.set(
            isSmall(blockEntity) ? 
                settings.getFontColor() :            
                DLColor.pickBasedOnBrightness(settings.getFontColor(), LIGHT_FONT_COLOR, DARK_FONT_COLOR, 0.5f)
        );

        BERLabel trainLabel = components[LineComponent.TRAIN_NAME.i()];
        trainLabel.text.set(TextUtils.text(stop.getTrainData().getName(stopState)));
        if (settings.showLineColor() && stop.getTrainData().hasColor(stopState)) {
            trainLabel.backgroundColor.set(stop.getTrainData().getColor(stopState));
            trainLabel.color.set(DLColor.pickBasedOnBrightness(stop.getTrainData().getColor(stopState), LIGHT_FONT_COLOR, DARK_FONT_COLOR, 0.5f));
        } else {            
            trainLabel.backgroundColor.set(DLColor.TRANSPARENT);
            trainLabel.color.set(settings.getFontColor());
        }

        BERLabel destinationLabel = components[LineComponent.DESTINATION.i()];
        destinationLabel.text.set(stopState == ETrainStopState.ARRIVAL ?
                CustomLanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".schedule_board.train_from", stop.getFirstStopName()) :
                TextUtils.text(stop.getStationData().getDestination())
        );

        BERLabel stopoversLabel = components[LineComponent.STOPOVERS.i()];
        BERLabel infoLabel = components[LineComponent.INFO.i()];
        boolean hasTransfers = stopoversLabel != null;
        boolean hasInfo = infoLabel != null;

        if (hasTransfers) {
            stopoversLabel.text.set(
                    stopState == ETrainStopState.ARRIVAL ?
                    TextUtils.empty() :
                    TextUtils.concat(TextUtils.text(" \u25CF "), stop.getStopovers().stream().map(a -> (Component)TextUtils.text(a)).toList())
            );
        }
        if (hasInfo) {
            if (showInfoLine) {
                infoLabel.text.set(getStatusInfo(blockEntity, stop, true).orElse(TextUtils.empty()));
                infoLabel.color.set(DLColor.pickBasedOnBrightness(settings.getFontColor(), LIGHT_FONT_COLOR, DARK_FONT_COLOR, 0.5f));
            } else {
                infoLabel.text.set(TextUtils.empty());
            }            
        }

        BERLabel platformLabel = components[LineComponent.PLATFORM.i()];
        platformLabel.text.set(TextUtils.text(stop.getStationData().getRealTimeStation().info().platform()));
        platformLabel.position.set(Point.of(headlines[LineComponent.PLATFORM.i()].x.get(), Y_OFFSET + 3 + index * LINE_HEIGHT));
        platformLabel.preferredWidth.set(headlines[LineComponent.PLATFORM.i()].preferredWidth.get());
        
        if (stop.getStationData().isStationChanged()) {
            platformLabel.backgroundColor.set(getDisplaySettings(blockEntity).getFontColor());
            platformLabel.color.set(DARK_FONT_COLOR);
        } else {            
            platformLabel.backgroundColor.set(DLColor.TRANSPARENT);
            platformLabel.color.set(getDisplaySettings(blockEntity).getFontColor());
        }


        if (layoutUpdate) {
            timeLabel.position.set(Point.of(headlines[LineComponent.TIME.i()].x.get(), Y_OFFSET + 3 + index * LINE_HEIGHT));
            realTimeLabel.position.set(Point.of(timeLabel.x.get() + timeLabel.preferredWidth.get() + SPACING, Y_OFFSET + 3 + index * LINE_HEIGHT));

            trainLabel.position.set(Point.of(headlines[LineComponent.TRAIN_NAME.i()].x.get(), Y_OFFSET + 3 + index * LINE_HEIGHT));
            trainLabel.preferredSize.set(headlines[LineComponent.TRAIN_NAME.i()].preferredSize.get());            
            destinationLabel.position.set(Point.of(headlines[LineComponent.DESTINATION.i()].x.get(), Y_OFFSET + 3 + index * LINE_HEIGHT));
            destinationLabel.preferredSize.set(headlines[LineComponent.DESTINATION.i()].preferredSize.get());
            
            if (hasTransfers) {
                stopoversLabel.position.set(Point.of(headlines[LineComponent.STOPOVERS.i()].x.get(), Y_OFFSET + 3 + index * LINE_HEIGHT));
                stopoversLabel.preferredSize.set(headlines[LineComponent.STOPOVERS.i()].preferredSize.get());
            }
            if (hasInfo) {
                infoLabel.position.set(Point.of(headlines[LineComponent.INFO.i()].x.get(), Y_OFFSET + 3 + index * LINE_HEIGHT));
                infoLabel.preferredSize.set(Size.of(headlines[LineComponent.INFO.i()].preferredWidth.get(), Minecraft.getInstance().font.lineHeight * infoLabel.verticalMaxScale.get()));
            }
        }
    }


    private BERLabel[] createLine(AdvancedDisplayBlockEntity blockEntity, StationDisplayData stop, int index, boolean withStopovers, boolean withInfo) {
        BERLabel[] components = new BERLabel[LineComponent.values().length];
        boolean isSmall = isSmall(blockEntity);

        BERLabel timeLabel = components[LineComponent.TIME.i()] = new BERLabel();
        timeLabel.verticalScale.set(Pair.of(0.4f, 0.4f));
        timeLabel.horizontalScale.set(Pair.of(0.2f, 0.4f));
        timeLabel.preferredWidth.set(isSmall ? -2 : TIME_LABEL_MAX_WIDTH);
        timeLabel.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
        timeLabel.horizontalScrollingSpeed.set(SCROLLING_SPEED);
        timeLabel.color.set(getDisplaySettings(blockEntity).getFontColor());
        
        BERLabel realTimeLabel = components[LineComponent.REAL_TIME.i()] = new BERLabel();
        realTimeLabel.verticalScale.set(Pair.of(0.4f, 0.4f));
        realTimeLabel.horizontalScale.set(Pair.of(0.2f, 0.4f));
        realTimeLabel.preferredWidth.set(REAL_TIME_LABEL_MAX_WIDTH);
        realTimeLabel.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
        realTimeLabel.horizontalScrollingSpeed.set(SCROLLING_SPEED);
        realTimeLabel.color.set(getDisplaySettings(blockEntity).getFontColor());
        realTimeLabel.backgroundPadding.set(new PaddingF(0.5f));
        
        if (!isSmall) {
            realTimeLabel.backgroundColor.set(getDisplaySettings(blockEntity).getFontColor());
            realTimeLabel.color.set(DARK_FONT_COLOR);
        }
        BERLabel trainNameLabel = components[LineComponent.TRAIN_NAME.i()] = new BERLabel();
        trainNameLabel.verticalScale.set(Pair.of(0.4f, 0.4f));
        trainNameLabel.horizontalScale.set(Pair.of(0.2f, 0.4f));
        trainNameLabel.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
        trainNameLabel.horizontalScrollingSpeed.set(SCROLLING_SPEED);
        trainNameLabel.color.set(getDisplaySettings(blockEntity).getFontColor());
        trainNameLabel.backgroundPadding.set(new PaddingF(0.5f));
        
        BERLabel platformLabel = components[LineComponent.PLATFORM.i()] = new BERLabel();        
        platformLabel.verticalScale.set(Pair.of(0.4f, 0.4f));
        platformLabel.horizontalScale.set(Pair.of(0.2f, 0.4f));
        platformLabel.horizontalAlign.set(ETextAlignment.RIGHT);
        platformLabel.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
        platformLabel.horizontalScrollingSpeed.set(SCROLLING_SPEED);
        platformLabel.color.set(getDisplaySettings(blockEntity).getFontColor());
        
        BERLabel destinationLabel = components[LineComponent.DESTINATION.i()] = new BERLabel();
        destinationLabel.verticalScale.set(Pair.of(0.4f, 0.4f));
        destinationLabel.horizontalScale.set(Pair.of(0.2f, 0.4f));
        destinationLabel.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
        destinationLabel.horizontalScrollingSpeed.set(SCROLLING_SPEED);
        destinationLabel.color.set(getDisplaySettings(blockEntity).getFontColor());

        if (withStopovers) {
            BERLabel stopoversLabel = components[LineComponent.STOPOVERS.i()] = new BERLabel();
            stopoversLabel.verticalScale.set(Pair.of(0.3f, 0.3f));
            stopoversLabel.horizontalScale.set(Pair.of(0.2f, 0.3f));
            stopoversLabel.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
            stopoversLabel.horizontalScrollingSpeed.set(SCROLLING_SPEED);
            stopoversLabel.color.set(getDisplaySettings(blockEntity).getFontColor());
        }
        if (withInfo) {
            BERLabel infoLabel = components[LineComponent.INFO.i()] = new BERLabel();
            infoLabel.verticalScale.set(Pair.of(0.4f, 0.4f));
            infoLabel.horizontalScale.set(Pair.of(0.2f, 0.4f));
            infoLabel.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
            infoLabel.horizontalScrollingSpeed.set(SCROLLING_SPEED);
            infoLabel.color.set(DARK_FONT_COLOR);
            infoLabel.backgroundColor.set(getDisplaySettings(blockEntity).getFontColor());
            infoLabel.fullBackground.set(true);
            infoLabel.backgroundPadding.set(new PaddingF(0.5f, 0.5f, 0, 0.5f));
            infoLabel.preferredHeight.set(Minecraft.getInstance().font.lineHeight * infoLabel.verticalMaxScale.get());
        }

        return components;
    }

    private boolean isSmall(AdvancedDisplayBlockEntity blockEntity) {
        return blockEntity.getXSizeScaled() <= MIN_SIZE;
    }

    private static enum LineComponent {
        TIME(0),
        REAL_TIME(1),
        TRAIN_NAME(2),
        DESTINATION(3),
        PLATFORM(4),
        STOPOVERS(5),
        INFO(6);

        int index;
        LineComponent(int index) {
            this.index = index;
        }
        public int i() {
            return index;
        }
    }
}
