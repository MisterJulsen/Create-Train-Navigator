package de.mrjulsen.crn.client.ber.variants;

import java.util.ArrayList;
import java.util.List;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity.EUpdateReason;
import de.mrjulsen.crn.block.display.properties.components.ITrainStopTypeSetting;
import de.mrjulsen.crn.block.properties.ETimeDisplay;
import de.mrjulsen.crn.block.display.properties.PlatformDisplayTableSettings;
import de.mrjulsen.crn.client.ber.AdvancedDisplayRenderInstance;
import de.mrjulsen.crn.client.lang.CustomLanguage;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.backend.api.BoardEntry;
import de.mrjulsen.crn.backend.api.CallDirection;
import de.mrjulsen.crn.util.ModUtils;
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
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BERPlatformDetailed implements AbstractAdvancedDisplayRenderer<PlatformDisplayTableSettings> {

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
                ? CustomLanguage.translate(keyTime, ModUtils.formatTime(ModUtils.getTransformedWorldTime(), false))
                : TextUtils.text(ModUtils.formatTime(ModUtils.getTransformedWorldTime(), false))
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
                if (getDisplaySettings(graphics.blockEntity()).showTimeAndDate() && i >= maxLines - 1 && (ModUtils.getTransformedWorldTime() % 200 > 100)) {
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
        long now = ModUtils.getTransformedWorldTime();
        ITrainStopTypeSetting.ETrainStopType stopType = getDisplaySettings(blockEntity).getTrainStopType();
        List<BoardEntry> preds = new ArrayList<>();

        for (int i = 0; i < blockEntity.getStops().size(); i++) {
            BoardEntry data = blockEntity.getStops().get(i);
            // The topmost train keeps its line even once its time has passed, so the board never goes
            // blank while the one train it is about is still standing there.
            boolean shouldShow = i == 0 || data.realtime().arrival() < now + ModCommonConfig.DISPLAY_LEAD_TIME.get();
            if (shouldShow && ITrainStopTypeSetting.accepts(data, stopType, now)) {
                preds.add(data);
            }
        }

        List<Component> lineTexts = new ArrayList<>();
        for (BoardEntry entry : preds.stream().limit(Math.max(0, maxLines)).toList()) {
            List<Component> content = announcements(blockEntity, entry, ITrainStopTypeSetting.resolveDirection(entry, getDisplaySettings(blockEntity)));
            if (content.isEmpty()) {
                continue;
            }
            lineTexts.add(CustomLanguage
                .translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.information_about_train", entry.displayName())
                .append(TextUtils.text(": "))
                .append(TextUtils.concat(TextUtils.text(" - "), content)));
        }

        showInfoLine = !lineTexts.isEmpty();
        infoLineText = showInfoLine
            ? TextUtils.concat(TextUtils.text("  +++  "), lineTexts.toArray(Component[]::new))
            : TextUtils.empty();

        int defaultMaxLines = blockEntity.getYSizeScaled() * 3 - 1;
        this.maxLines = defaultMaxLines - (showInfoLine ? 1 : 0);        
        int maxIndices = Math.max(0, Math.min(this.maxLines, preds.size()));
        if (reason == EUpdateReason.LAYOUT_CHANGED || this.lines == null || lines.length != maxIndices) {
            updateLayout(blockEntity, preds, maxIndices);
        }
            
        for (int i = 0; i < this.lines.length; i++) {
            BoardEntry stop = preds.get(i);
            updateContent(blockEntity, stop, i);
        }

        statusLabel.text.set(infoLineText);
        statusLabel.position.set(Point.of(3, blockEntity.getYSizeScaled() * 16 - 12 * statusLabel.verticalMaxScale.get() - 2));
        statusLabel.preferredWidth.set((float)statusLabel.clippingArea.get().width() - 2);
        statusLabel.preferredHeight.set(Minecraft.getInstance().font.lineHeight * statusLabel.verticalMaxScale.get());
        statusLabel.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
        
        timeLabel.position.set(Point.of(3, 3 + (Math.min(lines.length, maxLines) - (lines.length < maxLines ? 0 : 1)) * LINE_HEIGHT));
    }

    private void updateLayout(AdvancedDisplayBlockEntity blockEntity, List<BoardEntry> preds, int maxIndices) {
        this.lines = new BERLabel[maxIndices][];
        for (int i = 0; i < this.lines.length; i++) {
            BoardEntry stop = preds.get(i);
            this.lines[i] = createLine(blockEntity, stop, i);
            updateContent(blockEntity, stop, i);
        }
        statusLabel.backgroundColor.set(getDisplaySettings(blockEntity).getFontColor());
        statusLabel.color.set(DLColor.pickBasedOnBrightness(getDisplaySettings(blockEntity).getFontColor(), LIGHT_FONT_COLOR, DARK_FONT_COLOR, 0.5f));

        timeLabel.preferredWidth.set((float)timeLabel.clippingArea.get().width());
        timeLabel.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
        timeLabel.color.set(getDisplaySettings(blockEntity).getFontColor());
    }

    private void updateContent(AdvancedDisplayBlockEntity blockEntity, BoardEntry stop, int index) {
        PlatformDisplayTableSettings settings = getDisplaySettings(blockEntity);
        CallDirection direction = ITrainStopTypeSetting.resolveDirection(stop, settings);

        BERLabel[] components = lines[index];
        Component scheduledTimeFormatted = TextUtils.text(ModUtils.formatTime(
                stop.scheduledTime(direction), settings.getTimeDisplay() == ETimeDisplay.ETA));
        Component realTimeFormatted = TextUtils.text(ModUtils.formatTime(
                stop.realtimeTime(direction), settings.getTimeDisplay() == ETimeDisplay.ETA));

        BERLabel timeComponent = components[LineComponent.TIME.i()];
        timeComponent.text.set(scheduledTimeFormatted);

        BERLabel realTimeComponent = components[LineComponent.REAL_TIME.i()];
        if (stop.isCancelled()) {
            realTimeComponent.text.set(TextUtils.text(" \u274C ")); // X
        } else if (stop.isDelayed(direction)) {
            realTimeComponent.text.set(realTimeFormatted);
        } else {
            realTimeComponent.text.set(TextUtils.empty());
        }
        realTimeComponent.color.set(DLColor.pickBasedOnBrightness(getDisplaySettings(blockEntity).getFontColor(), LIGHT_FONT_COLOR, DARK_FONT_COLOR, 0.5f));

        BERLabel trainNameComponent = components[LineComponent.TRAIN_NAME.i()];
        trainNameComponent.text.set(TextUtils.text(stop.displayName(direction)));

        if (settings.showLineColor() && stop.hasColor(direction)) {
            trainNameComponent.backgroundColor.set(stop.displayColor(direction));
            trainNameComponent.color.set(DLColor.pickBasedOnBrightness(stop.displayColor(direction), LIGHT_FONT_COLOR, DARK_FONT_COLOR, 0.5f));
        } else {
            trainNameComponent.backgroundColor.set(DLColor.TRANSPARENT);
            trainNameComponent.color.set(settings.getFontColor());
        }

        BERLabel platformComponent = components[LineComponent.PLATFORM.i()];
        platformComponent.text.set(TextUtils.text(stop.station().platform()));

        BERLabel destinationComponent = components[LineComponent.DESTINATION.i()];
        destinationComponent.text.set(direction.isArrival() ?
            CustomLanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".schedule_board.train_from", stop.origin().displayName()) :
            TextUtils.text(stop.destinationText())
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

        if (stop.isDiverted()) {
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

    private BERLabel[] createLine(AdvancedDisplayBlockEntity blockEntity, BoardEntry stop, int index) {
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
