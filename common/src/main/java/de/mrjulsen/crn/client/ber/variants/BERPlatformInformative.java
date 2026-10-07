package de.mrjulsen.crn.client.ber.variants;

import java.util.List;

import de.mrjulsen.crn.block.display.properties.components.ITrainStopTypeSetting;
import org.joml.Vector3f;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.api.core.snapshot.BoardEntry;
import de.mrjulsen.crn.api.core.CallDirection;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity.EUpdateReason;
import de.mrjulsen.crn.block.properties.ETimeDisplay;
import de.mrjulsen.crn.block.display.properties.PlatformDisplayFocusSettings;
import de.mrjulsen.crn.client.ber.AdvancedDisplayRenderInstance;
import de.mrjulsen.crn.client.lang.CustomLanguage;
import de.mrjulsen.crn.util.ModUtils;
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
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class BERPlatformInformative implements AbstractAdvancedDisplayRenderer<PlatformDisplayFocusSettings> {

    private static final String keyFollowingTrains = "gui.createrailwaysnavigator.following_trains";

    private static final float LINE_HEIGHT = 5.4f;

    private int maxLines = 0;
    private boolean showInfoLine = false;
    private Component infoLineText = TextUtils.empty();

    private BERLabel statusLabel;
    private BERLabel[] focusArea;
    private BERLabel[][] lines;
    private final BERLabel platformLabel = new BERLabel();
    private final BERLabel followingTrainsLabel = new BERLabel();

    public BERPlatformInformative() {
        platformLabel.verticalScale.set(Pair.of(0.8f, 0.8f));
        platformLabel.horizontalScale.set(Pair.of(0.4f, 0.6f));

        followingTrainsLabel.text.set(CustomLanguage.translate(keyFollowingTrains));
        followingTrainsLabel.position.set(Point.of(3, 16));
        followingTrainsLabel.verticalScale.set(Pair.of(0.2f, 0.2f));
        followingTrainsLabel.horizontalScale.set(Pair.of(0.2f, 0.2f));
    }

    private boolean isExtendedDisplay(AdvancedDisplayBlockEntity blockEntity) {
        return blockEntity.getYSize() > 1;
    }

    @Override
    public void render(BERGraphics<AdvancedDisplayBlockEntity> graphics, float pPartialTicks, AdvancedDisplayRenderInstance parent, int light, boolean backSide) {
        if (isExtendedDisplay(graphics.blockEntity())) {
            RenderUtils.fillColor(
                graphics,
                new Vector3f(2.5f, 15.5f, 0.0f),
                graphics.blockEntity().getXSizeScaled() * 16 - 5, 0.25f,
                getDisplaySettings(graphics.blockEntity()).getFontColor(),
                graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING),
                graphics.blockEntity().isGlowing() ? LightTexture.FULL_BRIGHT : light,
                false
            );
            followingTrainsLabel.render(graphics);
        }

        DLUtils.doIfNotNull(focusArea, a -> {
            BERLabel label;
            for (int i = 0; i < a.length; i++) {
                label = a[i];
                if (label == null) continue;

                graphics.poseStack().pushPose();
                if (backSide) {
                    float maxWidth = graphics.blockEntity().getXSizeScaled() * 16;
                    if (i == LineComponent.TIME.i()) {
                        graphics.poseStack().translate(-label.x.get() + maxWidth - 3 - label.getRenderedWidth(), 0, 0);
                    } else if (i == LineComponent.REAL_TIME.i()) {
                        graphics.poseStack().translate(-label.x.get() + maxWidth - 3 - label.getRenderedWidth(), 0, 0);
                    } else if (i == LineComponent.TRAIN_NAME.i()) {
                        graphics.poseStack().translate(-label.x.get() + maxWidth - 3 - label.getRenderedWidth(), 0, 0);
                    } else if (i == LineComponent.DESTINATION.i()) {
                        graphics.poseStack().translate(-label.x.get() + 5 + platformLabel.preferredWidth.get(), 0, 0);
                    } else if (i == LineComponent.STOPOVERS.i()) {
                        graphics.poseStack().translate(-label.x.get() + 5 + platformLabel.preferredWidth.get(), 0, 0);
                    } else if (i == LineComponent.PLATFORM.i()) {
                        graphics.poseStack().translate(-label.x.get() + 3, 0, 0);
                    }
                }

                label.render(graphics);
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

        if (statusLabel != null && !statusLabel.text.get().getString().isBlank()) {
            graphics.poseStack().pushPose();
            if (backSide) {
                graphics.poseStack().translate(-statusLabel.x.get() + 5 + platformLabel.preferredWidth.get(), 0, 0);
            }
            DLUtils.doIfNotNull(statusLabel, x -> x.render(graphics, light));
            graphics.poseStack().popPose();
        }
        if (backSide && platformLabel != null) {
            graphics.poseStack().translate(-graphics.blockEntity().getXSizeScaled() * 16 + 6 + platformLabel.getRenderedWidth(), 0, 0);
        }
        platformLabel.render(graphics, light);
        graphics.poseStack().popPose();
    }

    @Override
    public void update(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent, EUpdateReason reason) {
        long now = ModUtils.getTransformedWorldTime();
        ITrainStopTypeSetting.ETrainStopType stopType = getDisplaySettings(blockEntity).getTrainStopType();
        List<BoardEntry> preds = blockEntity.getStops().stream()
            .filter(x -> ITrainStopTypeSetting.accepts(x, stopType, now))
            .toList();

        if (preds.isEmpty()) {
            lines = null;
            focusArea = null;
            statusLabel = null;


            platformLabel.text.set(TextUtils.text(blockEntity.isPlatformFixed() && blockEntity.getStationInfo() != null ? blockEntity.getStationInfo().platform() : "").withStyle(ChatFormatting.BOLD));
            float platformWidth = getDisplaySettings(blockEntity).isAutoPlatformWidthNextStop() ? (float)platformLabel.getRenderedWidth() : getDisplaySettings(blockEntity).getPlatformWidthNextStop();
            platformLabel.position.set(Point.of(blockEntity.getXSizeScaled() * 16 - 3 - platformWidth, 3));
            platformLabel.horizontalAlign.set(ETextAlignment.RIGHT);
            platformLabel.preferredWidth.set(platformWidth);
            platformLabel.color.set(getDisplaySettings(blockEntity).getFontColor());
            return;
        }

        if (reason == EUpdateReason.LAYOUT_CHANGED || this.lines == null || this.focusArea == null) {
            updateLayout(blockEntity, preds);
        }

        BoardEntry focus = preds.get(0);
        CallDirection focusDirection = ITrainStopTypeSetting.resolveDirection(focus, getDisplaySettings(blockEntity));

        List<Component> content = announcements(blockEntity, focus, focusDirection);
        showInfoLine = !content.isEmpty();
        infoLineText = showInfoLine ? TextUtils.concat(TextUtils.text("  +++  "), content) : TextUtils.empty();

        updateFocusContent(blockEntity, focus, focusDirection);
        for (int i = 1; i < this.lines.length && i < preds.size(); i++) {
            updateTableContent(blockEntity, preds.get(i), i);
        }
    }

    private void updateLayout(AdvancedDisplayBlockEntity blockEntity, List<BoardEntry> preds) {
        this.focusArea = new BERLabel[7];
        this.lines = new BERLabel[0][];

        platformLabel.clippingArea.set(Rectangle.withSize(2, 2, blockEntity.getXSizeScaled() * 16 - 4, blockEntity.getYSizeScaled() * 16 - 4));
        platformLabel.glowing.set(blockEntity.isGlowing());
        followingTrainsLabel.clippingArea.set(Rectangle.withSize(2, 2, blockEntity.getXSizeScaled() * 16 - 4, blockEntity.getYSizeScaled() * 16 - 4));
        followingTrainsLabel.glowing.set(blockEntity.isGlowing());

        followingTrainsLabel.color.set(getDisplaySettings(blockEntity).getFontColor());
        platformLabel.color.set(getDisplaySettings(blockEntity).getFontColor());

        BERLabel timeLabel = focusArea[LineComponent.TIME.i()] = new BERLabel();
        timeLabel.position.set(Point.of(3, 3));
        timeLabel.verticalScale.set(Pair.of(0.4f, 0.4f));
        timeLabel.horizontalScale.set(Pair.of(0.2f, 0.4f));
        timeLabel.color.set(getDisplaySettings(blockEntity).getFontColor());

        BERLabel realTimeLabel = focusArea[LineComponent.REAL_TIME.i()] = new BERLabel();
        realTimeLabel.position.set(Point.of(3, 7));
        realTimeLabel.verticalScale.set(Pair.of(0.4f, 0.4f));
        realTimeLabel.horizontalScale.set(Pair.of(0.2f, 0.4f));
        realTimeLabel.backgroundColor.set(getDisplaySettings(blockEntity).getFontColor());
        realTimeLabel.backgroundPadding.set(new PaddingF(0.5f));
        realTimeLabel.color.set(DARK_FONT_COLOR);

        BERLabel trainNameLabel = focusArea[LineComponent.TRAIN_NAME.i()] = new BERLabel();
        trainNameLabel.position.set(Point.of(3, 7));
        trainNameLabel.verticalScale.set(Pair.of(0.3f, 0.3f));
        trainNameLabel.horizontalScale.set(Pair.of(0.15f, 0.3f));
        trainNameLabel.preferredWidth.set(12f);
        trainNameLabel.horizontalScrollMode.set(EScrollMode.FLEX_FIT);
        trainNameLabel.backgroundPadding.set(new PaddingF(0.5f));
        trainNameLabel.color.set(getDisplaySettings(blockEntity).getFontColor());

        BERLabel destinationLabel = focusArea[LineComponent.DESTINATION.i()] = new BERLabel();
        destinationLabel.verticalScale.set(Pair.of(0.6f, 0.6f));
        destinationLabel.horizontalScale.set(Pair.of(0.4f, 0.6f));
        destinationLabel.horizontalScrollingSpeed.set(SCROLLING_SPEED);
        destinationLabel.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
        destinationLabel.color.set(getDisplaySettings(blockEntity).getFontColor());

        BERLabel stopoversLabel = focusArea[LineComponent.STOPOVERS.i()] = new BERLabel();
        stopoversLabel.verticalScale.set(Pair.of(0.2f, 0.2f));
        stopoversLabel.horizontalScale.set(Pair.of(0.1f, 0.2f));
        stopoversLabel.horizontalScrollingSpeed.set(SCROLLING_SPEED);
        stopoversLabel.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
        stopoversLabel.color.set(getDisplaySettings(blockEntity).getFontColor());

        statusLabel = new BERLabel();
        statusLabel.text.set(infoLineText);
        statusLabel.verticalScale.set(Pair.of(0.3f, 0.3f));
        statusLabel.horizontalScale.set(Pair.of(0.3f, 0.3f));
        statusLabel.preferredHeight.set(Minecraft.getInstance().font.lineHeight * statusLabel.verticalMaxScale.get());
        statusLabel.horizontalScrollingSpeed.set(SCROLLING_SPEED);
        statusLabel.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
        statusLabel.backgroundColor.set(getDisplaySettings(blockEntity).getFontColor());
        statusLabel.backgroundPadding.set(new PaddingF(0.5f));
        statusLabel.fullBackground.set(true);
        statusLabel.color.set(DARK_FONT_COLOR);

        if (isExtendedDisplay(blockEntity)) {
            maxLines = (blockEntity.getYSizeScaled() - 1) * 3;
            int maxIndices = Math.min(this.maxLines, preds.size());
            this.lines = new BERLabel[Math.max(maxIndices, 0)][];
            for (int i = 1; i < this.lines.length; i++) {
                BoardEntry stop = preds.get(i);
                this.lines[i] = createTableLine(blockEntity, stop, i);
            }
        }

        DLUtils.doIfNotNull(statusLabel, l -> {
            l.clippingArea.set(Rectangle.withSize(2, 2, blockEntity.getXSizeScaled() * 16 - 4, blockEntity.getYSizeScaled() * 16 - 4));
            l.glowing.set(blockEntity.isGlowing());
        });
        DLUtils.doIfNotNull(focusArea, x -> {
            for (int i = 0; i < x.length; i++) {
                DLUtils.doIfNotNull(x[i], l -> {
                    l.clippingArea.set(Rectangle.withSize(2, 2, blockEntity.getXSizeScaled() * 16 - 4, blockEntity.getYSizeScaled() * 16 - 4));
                    l.glowing.set(blockEntity.isGlowing());
                });
            }
        });
        DLUtils.doIfNotNull(lines, x -> {
            for (int i = 0; i < x.length; i++) {
                DLUtils.doIfNotNull(x[i], y -> {
                    for (int j = 0; j < y.length; j++) {
                        DLUtils.doIfNotNull(y[j], l -> {
                            l.clippingArea.set(Rectangle.withSize(2, 2, blockEntity.getXSizeScaled() * 16 - 4, blockEntity.getYSizeScaled() * 16 - 4));
                            l.glowing.set(blockEntity.isGlowing());
                        });
                    }
                });
            }
        });
    }

    private BERLabel[] createTableLine(AdvancedDisplayBlockEntity blockEntity, BoardEntry stop, int index) {
        BERLabel[] components = new BERLabel[5];

        BERLabel timeLabel = components[LineComponent.TIME.i()] = new BERLabel();
        timeLabel.verticalScale.set(Pair.of(0.4f, 0.4f));
        timeLabel.horizontalScale.set(Pair.of(0.2f, 0.4f));
        timeLabel.horizontalScrollingSpeed.set(SCROLLING_SPEED);
        timeLabel.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
        timeLabel.preferredWidth.set(12f);
        timeLabel.color.set(getDisplaySettings(blockEntity).getFontColor());

        BERLabel realTimeLabel = components[LineComponent.REAL_TIME.i()] = new BERLabel();
        realTimeLabel.verticalScale.set(Pair.of(0.4f, 0.4f));
        realTimeLabel.horizontalScale.set(Pair.of(0.2f, 0.4f));
        realTimeLabel.horizontalScrollingSpeed.set(SCROLLING_SPEED);
        realTimeLabel.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
        realTimeLabel.preferredWidth.set(12f);
        realTimeLabel.backgroundColor.set(getDisplaySettings(blockEntity).getFontColor());
        realTimeLabel.backgroundPadding.set(new PaddingF(0.5f));
        realTimeLabel.color.set(DARK_FONT_COLOR);

        BERLabel trainNameLabel = components[LineComponent.TRAIN_NAME.i()] = new BERLabel();
        trainNameLabel.verticalScale.set(Pair.of(0.4f, 0.4f));
        trainNameLabel.horizontalScale.set(Pair.of(0.2f, 0.4f));
        trainNameLabel.horizontalScrollingSpeed.set(SCROLLING_SPEED);
        trainNameLabel.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
        trainNameLabel.color.set(getDisplaySettings(blockEntity).getFontColor());
        trainNameLabel.backgroundPadding.set(new PaddingF(0.5f));

        BERLabel platformLabel = components[LineComponent.PLATFORM.i()] = new BERLabel();
        platformLabel.verticalScale.set(Pair.of(0.4f, 0.4f));
        platformLabel.horizontalScale.set(Pair.of(0.2f, 0.4f));
        platformLabel.color.set(getDisplaySettings(blockEntity).getFontColor());
        platformLabel.backgroundPadding.set(new PaddingF(0.5f));

        BERLabel destinationLabel = components[LineComponent.DESTINATION.i()] = new BERLabel();
        destinationLabel.verticalScale.set(Pair.of(0.4f, 0.4f));
        destinationLabel.horizontalScale.set(Pair.of(0.2f, 0.4f));
        destinationLabel.horizontalScrollingSpeed.set(SCROLLING_SPEED);
        destinationLabel.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
        destinationLabel.color.set(getDisplaySettings(blockEntity).getFontColor());

        return components;
    }

    private void updateFocusContent(AdvancedDisplayBlockEntity blockEntity, BoardEntry stop, CallDirection direction) {
        PlatformDisplayFocusSettings settings = getDisplaySettings(blockEntity);

        followingTrainsLabel.color.set(getDisplaySettings(blockEntity).getFontColor());

        Component scheduledTimeFormatted = TextUtils.text(ModUtils.formatTime(
                stop.scheduledTime(direction), settings.getTimeDisplay() == ETimeDisplay.ETA));
        Component realTimeFormatted = TextUtils.text(ModUtils.formatTime(
                stop.realtimeTime(direction), settings.getTimeDisplay() == ETimeDisplay.ETA));

        BERLabel timeLabel = focusArea[LineComponent.TIME.i()];
        timeLabel.text.set(scheduledTimeFormatted);

        BERLabel realTimeLabel = focusArea[LineComponent.REAL_TIME.i()];
        if (stop.isCancelled()) {
            realTimeLabel.text.set(TextUtils.text(" \u274C "));
        } else if (stop.isDelayed(direction)) {
            realTimeLabel.text.set(realTimeFormatted);
        } else {
            realTimeLabel.text.set(TextUtils.empty());
        }
        realTimeLabel.color.set(DLColor.pickBasedOnBrightness(getDisplaySettings(blockEntity).getFontColor(), LIGHT_FONT_COLOR, DARK_FONT_COLOR, 0.5f));

        int trainNameWidth = settings.getTrainNameWidthNextStop();
        BERLabel trainNameLabel = focusArea[LineComponent.TRAIN_NAME.i()];
        trainNameLabel.text.set(TextUtils.text(stop.displayName(direction)));
        trainNameLabel.position.set(Point.of(3, 7 + (stop.isDelayed(direction) ? 4.5f : 0)));
        trainNameLabel.preferredWidth.set(settings.isAutoTrainNameWidthNextStop() ? (float)trainNameLabel.clippingArea.get().width() : trainNameWidth);
        trainNameLabel.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);

        if (settings.showLineColor() && stop.hasColor(direction)) {
            trainNameLabel.backgroundColor.set(stop.displayColor(direction));
            trainNameLabel.color.set(DLColor.pickBasedOnBrightness(stop.displayColor(direction), LIGHT_FONT_COLOR, DARK_FONT_COLOR, 0.5f));
        } else {
            trainNameLabel.backgroundColor.set(DLColor.TRANSPARENT);
            trainNameLabel.color.set(getDisplaySettings(blockEntity).getFontColor());
        }

        platformLabel.text.set(TextUtils.text(getFocusPlatformLabelText(blockEntity, stop)).withStyle(ChatFormatting.BOLD));


        float x = 5 + Math.max(trainNameLabel.getRenderedWidth(), Math.max(timeLabel.getRenderedWidth(), realTimeLabel.getRenderedWidth()));

        float platformWidth = getDisplaySettings(blockEntity).isAutoPlatformWidthNextStop() ? (float)platformLabel.getRenderedWidth() : getDisplaySettings(blockEntity).getPlatformWidthNextStop();
        platformLabel.position.set(Point.of(blockEntity.getXSizeScaled() * 16 - 3 - platformWidth, 3));
        platformLabel.preferredWidth.set(platformWidth);
        platformLabel.horizontalAlign.set(ETextAlignment.RIGHT);
        platformLabel.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);

        float w = blockEntity.getXSizeScaled() * 16 - 5 - platformLabel.getRenderedWidth() - x;
        BERLabel destinationLabel = focusArea[LineComponent.DESTINATION.i()];
        destinationLabel.text.set(
                direction.isArrival() ?
                CustomLanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.arrival") :
                TextUtils.text(stop.destinationText())
        );
        destinationLabel.position.set(Point.of(x, 9f));
        destinationLabel.preferredWidth.set(w);
        destinationLabel.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);

        BERLabel stopoversLabel = focusArea[LineComponent.STOPOVERS.i()];
        stopoversLabel.text.set(
                direction.isArrival() ?
                CustomLanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".schedule_board.train_from", stop.origin().displayName()) :
                TextUtils.concat(TextUtils.text(" \u25CF "), stop.stopovers().stream().map(a -> (Component)TextUtils.text(a.displayName())).toList())
        );
        stopoversLabel.position.set(Point.of(x, 6.5f));
        stopoversLabel.preferredWidth.set(w);
        stopoversLabel.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);

        statusLabel.text.set(infoLineText);
        statusLabel.position.set(Point.of(x, 2.5f));
        statusLabel.preferredWidth.set(w);
        statusLabel.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
        statusLabel.color.set(DLColor.pickBasedOnBrightness(getDisplaySettings(blockEntity).getFontColor(), LIGHT_FONT_COLOR, DARK_FONT_COLOR, 0.5f));
    }

    private String getFocusPlatformLabelText(AdvancedDisplayBlockEntity blockEntity, BoardEntry stop) {
        if (blockEntity.isPlatformFixed()) {
            return blockEntity.getStationInfo().platform();
        }
        return blockEntity.isAllowedOnDisplay(stop.station())
            ? stop.station().platform()
            : stop.scheduledStation().platform();
    }

    private void updateTableContent(AdvancedDisplayBlockEntity blockEntity, BoardEntry stop, int index) {
        PlatformDisplayFocusSettings settings = getDisplaySettings(blockEntity);
        CallDirection direction = ITrainStopTypeSetting.resolveDirection(stop, settings);

        BERLabel[] components = lines[index];
        Component scheduledTimeFormatted = TextUtils.text(ModUtils.formatTime(
                stop.scheduledTime(direction), settings.getTimeDisplay() == ETimeDisplay.ETA));
        Component realTimeFormatted = TextUtils.text(ModUtils.formatTime(
                stop.realtimeTime(direction), settings.getTimeDisplay() == ETimeDisplay.ETA));

        BERLabel timeLabel = components[LineComponent.TIME.i()];
        timeLabel.text.set(scheduledTimeFormatted);

        BERLabel realTimeLabel = components[LineComponent.REAL_TIME.i()];
        if (stop.isCancelled()) {
            realTimeLabel.text.set(TextUtils.text(" \u274C "));
        } else if (stop.isDelayed(direction)) {
            realTimeLabel.text.set(realTimeFormatted);
        } else {
            realTimeLabel.text.set(TextUtils.empty());
        }
        realTimeLabel.color.set(DLColor.pickBasedOnBrightness(getDisplaySettings(blockEntity).getFontColor(), LIGHT_FONT_COLOR, DARK_FONT_COLOR, 0.5f));

        BERLabel trainNameLabel = components[LineComponent.TRAIN_NAME.i()];
        trainNameLabel.text.set(TextUtils.text(stop.displayName(direction)));

        if (settings.showLineColor() && stop.hasColor(direction)) {
            trainNameLabel.backgroundColor.set(stop.displayColor(direction));
            trainNameLabel.color.set(DLColor.pickBasedOnBrightness(stop.displayColor(direction), LIGHT_FONT_COLOR, DARK_FONT_COLOR, 0.5f));
        } else {
            trainNameLabel.backgroundColor.set(DLColor.TRANSPARENT);
            trainNameLabel.color.set(getDisplaySettings(blockEntity).getFontColor());
        }

        BERLabel platformLabel = components[LineComponent.PLATFORM.i()];
        platformLabel.text.set(
            blockEntity.isPlatformFixed() ? TextUtils.empty() : TextUtils.text(stop.station().platform())
        );

        BERLabel destinationLabel = components[LineComponent.DESTINATION.i()];
        destinationLabel.text.set(
                direction.isArrival() ?
                CustomLanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".schedule_board.train_from", stop.origin().displayName()) :
                TextUtils.text(stop.destinationText())
        );

        int x = 3;
        timeLabel.position.set(Point.of(x, 11 + 3 + index * LINE_HEIGHT));
        x += timeLabel.getRenderedWidth() + 2;
        realTimeLabel.position.set(Point.of(x, 11 + 3 + index * LINE_HEIGHT));
        x += realTimeLabel.getRenderedWidth() + 2 + (!realTimeLabel.text.get().getString().isEmpty() ? 2 : 0);

        float trainNameWidth = settings.isAutoTrainNameWidth() ? trainNameLabel.getRenderedWidth() : settings.getTrainNameWidth();
        trainNameLabel.position.set(Point.of(x, 11 + 3 + index * LINE_HEIGHT));
        trainNameLabel.preferredWidth.set(trainNameWidth);
        trainNameLabel.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
        x += trainNameWidth + 2;


        float platformWidth = settings.isAutoPlatformWidth() ? platformLabel.getRenderedWidth() : settings.getPlatformWidth();
        platformLabel.position.set(Point.of(blockEntity.getXSizeScaled() * 16 - 3 - platformWidth, 11 + 3 + index * LINE_HEIGHT));
        platformLabel.horizontalAlign.set(ETextAlignment.RIGHT);
        platformLabel.preferredWidth.set(platformWidth);
        platformLabel.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
        platformLabel.backgroundPadding.set(new PaddingF(0.5f));

        if (stop.isDiverted()) {
            platformLabel.backgroundColor.set(getDisplaySettings(blockEntity).getFontColor());
            platformLabel.color.set(DARK_FONT_COLOR);
        } else {
            platformLabel.backgroundColor.set(DLColor.TRANSPARENT);
            platformLabel.color.set(getDisplaySettings(blockEntity).getFontColor());
        }

        destinationLabel.position.set(Point.of(x, 11 + 3 + index * LINE_HEIGHT));
        destinationLabel.preferredWidth.set(blockEntity.getXSizeScaled() * 16 - 3 - x - platformWidth - 3);
        destinationLabel.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
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
