package de.mrjulsen.crn.client.ber.variants;

import java.util.List;

import de.mrjulsen.crn.data.train.ETrainStopState;
import org.joml.Vector3f;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity.EUpdateReason;
import de.mrjulsen.crn.block.properties.ETimeDisplay;
import de.mrjulsen.crn.block.display.properties.PassengerInformationDetailedSettings;
import de.mrjulsen.crn.client.CRNGui;
import de.mrjulsen.crn.client.ber.AdvancedDisplayRenderInstance;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.lang.CustomLanguage;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.TrainExitSide;
import de.mrjulsen.crn.data.train.portable.NextConnectionsDisplayData;
import de.mrjulsen.crn.data.train.portable.TrainDisplayData;
import de.mrjulsen.crn.data.train.portable.TrainDisplayData.State;
import de.mrjulsen.crn.data.train.portable.TrainStopDisplayData;
import de.mrjulsen.crn.network.packets.pain.GetNextConnectionsDisplayDataPacketData;
import de.mrjulsen.crn.registry.ModNetworkManager;
import de.mrjulsen.crn.registry.data.NextConnectionsRequestData;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.ber.BERGraphics;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel.EScrollMode;
import de.mrjulsen.mcdragonlib.client.gui.widgets.richtext.PaddingF;
import de.mrjulsen.mcdragonlib.client.util.RenderUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.network.NetworkDirection;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.Pair;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Point;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class BERPassengerInfoInformative implements AbstractAdvancedDisplayRenderer<PassengerInformationDetailedSettings> {

    private final MutableComponent textTrainTerminates = CustomLanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.train_terminates");
    private static final ResourceLocation CARRIAGE_ICON = new ResourceLocation("create:textures/gui/assemble.png");  
    private static final ResourceLocation ICONS = new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "textures/gui/icons.png");  
    private static final String keyDate = "gui.createrailwaysnavigator.route_overview.date";
    private static final String keyNextStop = "gui.createrailwaysnavigator.route_overview.next_stop";
    private static final String keyNextConnections = "gui.createrailwaysnavigator.route_overview.next_connections";
    private static final int MAX_LINES = 4;

    private NextConnectionsDisplayData nextConnections = null;
    private boolean nextStopAnnounced = false;
    private TrainExitSide exitSide = TrainExitSide.UNKNOWN;

    private final List<BERLabel> staticLabels;
    private final BERLabel timeLabel = new BERLabel();
    private final BERLabel carriageLabel = new BERLabel();
    private final BERLabel trainLineLabel = new BERLabel();
    private final BERLabel speedLabel = new BERLabel();
    private final BERLabel dateLabel = new BERLabel();
    private final BERLabel carriageInfoLabel = new BERLabel();
    private final BERLabel nextConnectionsTitleLabel = new BERLabel();
    private final BERLabel pageIndicatorLabel = new BERLabel();

    private BERLabel[][] scheduleLines;
    private BERLabel[][] nextConnectionsLines = new BERLabel[MAX_LINES - 1][];

    private float listDestinationLabelX = 0;


    public BERPassengerInfoInformative() {
        timeLabel.horizontalScale.set(Pair.of(0.25f, 0.25f));
        timeLabel.verticalScale.set(Pair.of(0.25f, 0.25f));
        
        carriageLabel.horizontalScale.set(Pair.of(0.25f, 0.25f));
        carriageLabel.verticalScale.set(Pair.of(0.25f, 0.25f));
        
        trainLineLabel.position.set(Point.of(3, 2.5f));
        trainLineLabel.horizontalScale.set(Pair.of(0.15f, 0.25f));
        trainLineLabel.verticalScale.set(Pair.of(0.25f, 0.25f));
        trainLineLabel.backgroundPadding.set(new PaddingF(0.5f));
        
        speedLabel.position.set(Point.of(3, 6));
        speedLabel.horizontalScale.set(Pair.of(0.2f, 0.25f));
        speedLabel.verticalScale.set(Pair.of(0.3f, 0.3f));
        speedLabel.horizontalAlign.set(ETextAlignment.CENTER);
                
        dateLabel.position.set(Point.of(3, 9));
        dateLabel.horizontalScale.set(Pair.of(0.15f, 0.2f));
        dateLabel.verticalScale.set(Pair.of(0.2f, 0.2f));
        dateLabel.horizontalAlign.set(ETextAlignment.CENTER);
                        
        carriageInfoLabel.position.set(Point.of(4.5f, 11));
        carriageInfoLabel.horizontalScale.set(Pair.of(0.15f, 0.2f));
        carriageInfoLabel.verticalScale.set(Pair.of(0.2f, 0.2f));
        carriageInfoLabel.horizontalAlign.set(ETextAlignment.CENTER);

        nextConnectionsTitleLabel.text.set(CustomLanguage.translate(keyNextConnections).withStyle(ChatFormatting.BOLD));
        nextConnectionsTitleLabel.position.set(Point.of(3, 5.5f));
        nextConnectionsTitleLabel.horizontalScale.set(Pair.of(0.15f, 0.15f));
        nextConnectionsTitleLabel.verticalScale.set(Pair.of(0.15f, 0.15f));
                                        
        pageIndicatorLabel.position.set(Point.of(3, 12.5f));
        pageIndicatorLabel.horizontalScale.set(Pair.of(0.15f, 0.15f));
        pageIndicatorLabel.verticalScale.set(Pair.of(0.15f, 0.15f));
        pageIndicatorLabel.horizontalAlign.set(ETextAlignment.CENTER);

        staticLabels = List.of(timeLabel, carriageLabel, trainLineLabel, speedLabel, dateLabel, carriageInfoLabel, nextConnectionsTitleLabel, pageIndicatorLabel);
    }

    private boolean shouldRenderNextConnections() {
        return nextConnections != null && !nextConnections.getConnections().isEmpty() && nextStopAnnounced;
    }

    private String generatePageIndexString(int current, int max) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < current; i++) {
            sb.append(" □");
        }
        sb.append(" ■");
        for (int i = current + 1; i < max; i++) {
            sb.append(" □");
        }
        return sb.toString();
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent) {
        timeLabel.text.set(blockEntity.getXSizeScaled() > 1 && !nextStopAnnounced ? TextUtils.text(ModUtils.formatTime(DragonLib.getCurrentWorldTime(), false)).withStyle(ChatFormatting.BOLD) : TextUtils.empty());
        timeLabel.horizontalAlign.set(ETextAlignment.RIGHT);
        timeLabel.color.set(getDisplaySettings(blockEntity).getFontColor());
    }

    public void renderHeader(BERGraphics<AdvancedDisplayBlockEntity> graphics, float partialTick, AdvancedDisplayRenderInstance parent, int light, boolean backSide) {
        final float uv255 = 1f / 256f;
        TrainExitSide side = exitSide;
        if (backSide) {
            side = side.getOpposite();
        }

        graphics.poseStack().pushPose();
        if (side == TrainExitSide.LEFT) {
            graphics.poseStack().translate(4, 0, 0);
        }

        // Render time
        if (graphics.blockEntity().getXSizeScaled() > 1 && !nextStopAnnounced) {            
            timeLabel.render(graphics);
            RenderUtils.renderTexture(
                ICONS,
                graphics,
                new Vector3f(timeLabel.x.get() - 2.5f, 2.5f, 0),
                2, 2,
                uv255 * 227, uv255 * 19,
                uv255 * 10, uv255 * 10,
                graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING),
                getDisplaySettings(graphics.blockEntity()).getFontColor(),
                false
            );
        }

        if (graphics.blockEntity().getTrainData() == null || graphics.blockEntity().getTrainData().getState().isOutOfService()) {
            graphics.poseStack().popPose();
            return;
        }

        trainLineLabel.render(graphics);
                
        // Carriage label
        if (graphics.blockEntity().getXSizeScaled() > 2 && !nextStopAnnounced) {            
            carriageLabel.render(graphics);
            RenderUtils.renderTexture(
                CARRIAGE_ICON,
                graphics,
                new Vector3f(carriageLabel.x.get() - 3.5f, 2.5f, 0.0f),
                3, 2,
                uv255 * 22, uv255 * 231,
                uv255 * 13, uv255 * 5,
                graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING),
                getDisplaySettings(graphics.blockEntity()).getFontColor(),
                false
            );
        }
        graphics.poseStack().popPose();

        if (nextStopAnnounced || graphics.blockEntity().getTrainData().isWaitingAtStation()) {            
            switch (side) {
                case RIGHT:
                    RenderUtils.renderTexture(
                        ModGuiIcons.ICON.getTexture().get(),
                        graphics,
                        new Vector3f(graphics.blockEntity().getXSizeScaled() * 16 - 3 - 3, 2.05f, 0),
                        3, 3,
                        uv255 * ModGuiIcons.ARROW_RIGHT.getU(), uv255 * ModGuiIcons.ARROW_RIGHT.getV(),
                        uv255 * ModGuiIcons.ICON_SIZE, uv255 * ModGuiIcons.ICON_SIZE,
                        graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING),
                        getDisplaySettings(graphics.blockEntity()).getFontColor(),
                        false
                    );
                    break;
                case LEFT:
                    RenderUtils.renderTexture(
                        ModGuiIcons.ICON.getTexture().get(),
                        graphics,
                        new Vector3f(3, 2.05f, 0),
                        3, 3,
                        uv255 * ModGuiIcons.ARROW_LEFT.getU(), uv255 * ModGuiIcons.ARROW_LEFT.getV(),
                        uv255 * ModGuiIcons.ICON_SIZE, uv255 * ModGuiIcons.ICON_SIZE,
                        graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING),
                        getDisplaySettings(graphics.blockEntity()).getFontColor(),
                        false
                    );
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void render(BERGraphics<AdvancedDisplayBlockEntity> graphics, float partialTick, AdvancedDisplayRenderInstance parent, int light, boolean backSide) {
        final float uv255 = 1f / 256f;
        renderHeader(graphics, partialTick, parent, light, backSide);
        RenderUtils.fillColor(
            graphics,
            new Vector3f(2.5f, 5.0f, 0.01f),
            graphics.blockEntity().getXSizeScaled() * 16 - 5, 0.25f,
            getDisplaySettings(graphics.blockEntity()).getFontColor(),
            graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING)
        );

        if (graphics.blockEntity().getTrainData() == null || graphics.blockEntity().getTrainData().getState().isOutOfService()) {
            return;
        }

        if (shouldRenderNextConnections()) {
            DLUtils.doIfNotNull(nextConnectionsLines, x -> {
                for (int i = 0; i < x.length; i++) {
                    DLUtils.doIfNotNull(x[i], a -> {
                        for (int j = 0; j < a.length; j++) {
                            DLUtils.doIfNotNull(a[j], b -> b.render(graphics));
                        }
                    });
                }
            });
            nextConnectionsTitleLabel.render(graphics);
            pageIndicatorLabel.render(graphics);
        } else if (getDisplaySettings(graphics.blockEntity()).showStats() && DragonLib.getCurrentWorldTime() % 500 < 200 && !graphics.blockEntity().getTrainData().isWaitingAtStation()) {
            // render stats
            speedLabel.render(graphics, light);
            dateLabel.render(graphics, light);
            carriageInfoLabel.render(graphics, light);
            RenderUtils.renderTexture(
                CARRIAGE_ICON,
                graphics,
                new Vector3f(graphics.blockEntity().getXSizeScaled() * 16 / 2f - carriageInfoLabel.getRenderedWidth() / 2f - 1.5f, carriageInfoLabel.y.get(), 0.0f),
                2.25f, 1.5f,
                uv255 * 22, uv255 * 231,
                uv255 * 13, uv255 * 5,
                graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING),
                getDisplaySettings(graphics.blockEntity()).getFontColor(),
                false
            );
        } else {
            // Render schedule
            DLUtils.doIfNotNull(scheduleLines, x -> {
                for (int i = 0; i < x.length; i++) {
                    final int idx = i;
                    DLUtils.doIfNotNull(x[i], a -> {
                        for (int j = 0; j < a.length; j++) {
                            DLUtils.doIfNotNull(a[j], b -> b.render(graphics));
                        }

                        final float uv32 = 1f / CRNGui.GUI.width();

                        if (idx == 0 && scheduleLines.length > 1) {
                            RenderUtils.renderTexture(
                                CRNGui.GUI.getTexture().get(),
                                graphics,
                                new Vector3f(listDestinationLabelX - 2, a[LineComponent.SCHEDULED_TIME.i()].y.get() - 1, 0.0f),
                                1, 2,
                                uv32 * 21, uv32 * 30,
                                uv32 * 7, uv32 * 14,
                                graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING),
                                getDisplaySettings(graphics.blockEntity()).getFontColor(),
                                false
                            );
                        } else if (idx >= MAX_LINES - 1) {
                            RenderUtils.renderTexture(
                                CRNGui.GUI.getTexture().get(),
                                graphics,
                                new Vector3f(listDestinationLabelX - 2, a[LineComponent.SCHEDULED_TIME.i()].y.get() - 1, 0.0f),
                                1, 2,
                                uv32 * 35, uv32 * 30,
                                uv32 * 7, uv32 * 14,
                                graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING),
                                getDisplaySettings(graphics.blockEntity()).getFontColor(),
                                false
                            );
                        } else {
                            RenderUtils.renderTexture(
                                CRNGui.GUI.getTexture().get(),
                                graphics,
                                new Vector3f(listDestinationLabelX - 2, a[LineComponent.SCHEDULED_TIME.i()].y.get() - 1, 0.0f),
                                1, 2,
                                uv32 * 28, uv32 * 30,
                                uv32 * 7, uv32 * 14,
                                graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING),
                                getDisplaySettings(graphics.blockEntity()).getFontColor(),
                                false
                            );
                        }
                    });
                }
            });
        }
        
    }

    @Override
    public void update(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent, EUpdateReason reason) {        
        boolean oos = blockEntity.getTrainData() == null || blockEntity.getTrainData().getState().isOutOfService();

        TrainDisplayData data = blockEntity.getTrainData();
        boolean wasNextStopAnnounced = nextStopAnnounced;
        nextStopAnnounced = !data.isWaitingAtStation() && data.getNextStop().isPresent() && data.getNextStop().get().getRealTimeArrivalTime() - DragonLib.getCurrentWorldTime() < ModClientConfig.NEXT_STOP_ANNOUNCEMENT.get();
        this.exitSide = (!nextStopAnnounced && !data.isWaitingAtStation()) || !getDisplaySettings(blockEntity).showExit() ? TrainExitSide.UNKNOWN : (data.isWaitingAtStation() ? exitSide : blockEntity.relativeExitDirection.get());
        
        if (oos) {
            this.nextStopAnnounced = false;
            this.exitSide = TrainExitSide.UNKNOWN;
        }


        timeLabel.text.set(blockEntity.getXSizeScaled() > 1 && !nextStopAnnounced ? TextUtils.text(ModUtils.formatTime(DragonLib.getCurrentWorldTime(), false)).withStyle(ChatFormatting.BOLD) : TextUtils.empty());
        float timeLabelW = timeLabel.getRenderedWidth();
        timeLabel.position.set(Point.of(blockEntity.getXSizeScaled() * 16 - 3 - timeLabelW - (this.exitSide != TrainExitSide.UNKNOWN ? 4 : 0), 2.5f));
        timeLabel.preferredWidth.set(timeLabelW);
        timeLabel.color.set(getDisplaySettings(blockEntity).getFontColor());

        if (oos) {
            return;
        }

        if (getDisplaySettings(blockEntity).showConnections() && blockEntity.getXSizeScaled() > 1 && nextStopAnnounced && !wasNextStopAnnounced && data.getNextStop().isPresent()) {
            ModNetworkManager.GET_NEXT_CONNECTIONS_DISPLAY_DATA.send(NetworkDirection.toServer(), new GetNextConnectionsDisplayDataPacketData.Request(new NextConnectionsRequestData(data.getNextStop().get().getRealTimeStation().stationName(), data.getTrainData().getId(), getDisplaySettings(blockEntity).showTrainMultipleTimes())), (response) -> {
                nextConnections = response.getData();
                updateLayout(blockEntity, data);
                updateContent(blockEntity, data);
            }, () -> {});
        }

        if (reason == EUpdateReason.LAYOUT_CHANGED || !nextStopAnnounced) {
            updateLayout(blockEntity, data);
            nextConnections = null;
        }
        updateContent(blockEntity, data);


        for (BERLabel label : staticLabels) {
            label.clippingArea.set(Rectangle.withSize(2, 2, blockEntity.getXSizeScaled() * 16 - 4, blockEntity.getYSizeScaled() * 16 - 4));
            label.glowing.set(blockEntity.isGlowing());
        }
        DLUtils.doIfNotNull(scheduleLines, x -> {
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
        DLUtils.doIfNotNull(nextConnectionsLines, x -> {
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

    private int getCarriageIndex(AdvancedDisplayBlockEntity blockEntity) {        
        PassengerInformationDetailedSettings settings = getDisplaySettings(blockEntity);
        return (settings.shouldOverwriteCarriageIndex() ? 0 : blockEntity.getCarriageData().index() + 1) + settings.getCarriageIndex();
    }

    private void updateContent(AdvancedDisplayBlockEntity blockEntity, TrainDisplayData displayData) {
        PassengerInformationDetailedSettings settings = getDisplaySettings(blockEntity);
        int carriageIndex = getCarriageIndex(blockEntity);

        carriageLabel.text.set(blockEntity.getXSizeScaled() > 1 && !nextStopAnnounced ? TextUtils.text(String.format("%02d", carriageIndex)).withStyle(ChatFormatting.BOLD) : TextUtils.empty());
        float carriageLabelW = carriageLabel.getRenderedWidth();
        carriageLabel.position.set(Point.of(timeLabel.x.get() - 4 - carriageLabelW, 2.5f));
        carriageLabel.preferredWidth.set(carriageLabelW);
        carriageLabel.color.set(getDisplaySettings(blockEntity).getFontColor());
        
        boolean atTerminus = blockEntity.getTrainData().getState() == State.AT_TERMINUS;
        ETrainStopState stopState = ETrainStopState.beforeArrival(!blockEntity.getTrainData().isWaitingAtStation());
        MutableComponent labelText;
        if (atTerminus) {
            labelText = textTrainTerminates;
        } else if (nextStopAnnounced) {
            labelText = CustomLanguage.translate(keyNextStop, displayData.getNextStop().get().getRealTimeStation().tagName());
        } else {
            labelText = TextUtils.text((settings.getTrainTextComponents().showTrainName() ? displayData.getTrainData().getName(stopState) + " " : "") + (settings.getTrainTextComponents().showDestination() ? displayData.getNextStop().get().getDestination() : "")).withStyle(ChatFormatting.BOLD);
        }

        trainLineLabel.text.set(labelText);
        trainLineLabel.preferredWidth.set((nextStopAnnounced ? blockEntity.getXSizeScaled() * 16 - 6 - (this.exitSide != TrainExitSide.UNKNOWN ? 4 : 0) : (blockEntity.getXSizeScaled() > 2 ? carriageLabel.x.get() - 9 : timeLabel.x.get() - 7)));
        trainLineLabel.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
        trainLineLabel.horizontalScrollingSpeed.set(SCROLLING_SPEED);
        
        if (settings.showLineColor() && blockEntity.getTrainData().getTrainData().hasColor(stopState) && !nextStopAnnounced && !atTerminus) {
            trainLineLabel.backgroundColor.set(blockEntity.getTrainData().getTrainData().getColor(stopState));
            trainLineLabel.color.set(DLColor.pickBasedOnBrightness(blockEntity.getTrainData().getTrainData().getColor(stopState), LIGHT_FONT_COLOR, DARK_FONT_COLOR, 0.5f));
        } else {            
            trainLineLabel.backgroundColor.set(DLColor.TRANSPARENT);
            trainLineLabel.color.set(settings.getFontColor());
        }

        speedLabel.text.set(ModUtils.calcSpeedString(displayData.getSpeed(), ModClientConfig.SPEED_UNIT.get()).withStyle(ChatFormatting.BOLD));
        speedLabel.preferredWidth.set((float)speedLabel.clippingArea.get().width());
        speedLabel.color.set(getDisplaySettings(blockEntity).getFontColor());
        
        dateLabel.text.set(CustomLanguage.translate(keyDate, blockEntity.getLevel().getDayTime() / Level.TICKS_PER_DAY, ModUtils.formatTime(DragonLib.getCurrentWorldTime(), getDisplaySettings(blockEntity).getTimeDisplay() == ETimeDisplay.ETA)));
        dateLabel.preferredWidth.set((float)dateLabel.clippingArea.get().width());
        dateLabel.color.set(getDisplaySettings(blockEntity).getFontColor());

        carriageInfoLabel.text.set(TextUtils.text(String.format("%02d", carriageIndex)));
        carriageInfoLabel.preferredWidth.set((float)carriageInfoLabel.clippingArea.get().width());
        carriageInfoLabel.color.set(getDisplaySettings(blockEntity).getFontColor());

        if (shouldRenderNextConnections() && !nextConnections.getConnections().isEmpty()) {
            final int pages = (int)Math.ceil((float)nextConnections.getConnections().size() / (MAX_LINES - 1));
            final int page = (int)((DragonLib.getCurrentWorldTime() % (100 * pages)) / 100);

            pageIndicatorLabel.text.set(TextUtils.text(generatePageIndexString(page, pages)));
            pageIndicatorLabel.preferredWidth.set((float)pageIndicatorLabel.clippingArea.get().width());
            pageIndicatorLabel.color.set(getDisplaySettings(blockEntity).getFontColor());

            nextConnectionsTitleLabel.preferredWidth.set((float)nextConnectionsTitleLabel.clippingArea.get().width());
            nextConnectionsTitleLabel.color.set(getDisplaySettings(blockEntity).getFontColor());
            
            DLUtils.doIfNotNull(nextConnectionsLines, x -> {
                for (int i = 0; i < MAX_LINES - 1; i++) {
                    final int k = i;
                    final int connectionIdx = i + (page * (MAX_LINES - 1));
                    DLUtils.doIfNotNull(nextConnectionsLines[i], a -> {
                        BERLabel scheduledTimeLabel = a[LineComponent.SCHEDULED_TIME.i()];
                        BERLabel realTimeLabel = a[LineComponent.REAL_TIME.i()];
                        BERLabel trainNameLabel = a[LineComponent.TRAIN_NAME.i()];
                        BERLabel destinationLabel = a[LineComponent.DESTINATION.i()];
                        BERLabel platformLabel = a[LineComponent.PLATFORM.i()];

                        if (connectionIdx >= nextConnections.getConnections().size()) {
                            scheduledTimeLabel.text.set(TextUtils.empty());
                            if (realTimeLabel != null) {
                                realTimeLabel.text.set(TextUtils.empty());
                            }
                            trainNameLabel.text.set(TextUtils.empty());
                            destinationLabel.text.set(TextUtils.empty());
                            platformLabel.text.set(TextUtils.empty());
                            return;
                        }

                        TrainStopDisplayData stop = nextConnections.getConnections().get(connectionIdx);                        
                        platformLabel.text.set(TextUtils.text(stop.getRealTimeStation().info().platform()));
                        float platformLabelW = platformLabel.getRenderedWidth();
                        platformLabel.position.set(Point.of(blockEntity.getXSizeScaled() * 16 - 3 - platformLabelW, 7.5f + k * 1.7f));
                        platformLabel.preferredWidth.set(platformLabelW);
                        
                        if (realTimeLabel != null) {
                            scheduledTimeLabel.text.set(TextUtils.text(ModUtils.formatTime(stop.getScheduledDepartureTime(), getDisplaySettings(blockEntity).getTimeDisplay() == ETimeDisplay.ETA)));
                            scheduledTimeLabel.position.set(Point.of(3, 7.5f + k * 1.7f));
                            scheduledTimeLabel.color.set(getDisplaySettings(blockEntity).getFontColor());
                            
                            realTimeLabel.text.set(TextUtils.text(ModUtils.formatTime(stop.getRealTimeDepartureTime(), getDisplaySettings(blockEntity).getTimeDisplay() == ETimeDisplay.ETA)));
                            realTimeLabel.position.set(Point.of(scheduledTimeLabel.x.get() + scheduledTimeLabel.getRenderedWidth() + 1, 7.5f + k * 1.7f));
                            realTimeLabel.color.set(stop.isDepartureDelayed() ? Constants.COLOR_DELAYED : Constants.COLOR_ON_TIME);
                        } else {
                            scheduledTimeLabel.text.set(TextUtils.text(ModUtils.formatTime(stop.getRealTimeDepartureTime(), getDisplaySettings(blockEntity).getTimeDisplay() == ETimeDisplay.ETA)));
                            scheduledTimeLabel.position.set(Point.of(3, 7.5f + k * 1.7f));
                            scheduledTimeLabel.color.set(stop.isDepartureDelayed() ? Constants.COLOR_DELAYED : Constants.COLOR_ON_TIME);
                        }

                        float pX = scheduledTimeLabel.x.get() + scheduledTimeLabel.getRenderedWidth() + 1 + (realTimeLabel == null ? 0 : realTimeLabel.getRenderedWidth() + 1);
                        trainNameLabel.text.set(TextUtils.text(stop.getTrainName()));
                        trainNameLabel.position.set(Point.of(pX, 7.5f + k * 1.7f));
                        trainNameLabel.preferredWidth.set(6f);
                        trainNameLabel.color.set(getDisplaySettings(blockEntity).getFontColor());
                        
                        destinationLabel.text.set(TextUtils.text(stop.getDestination()));
                        destinationLabel.position.set(Point.of(pX + 7, 7.5f + k * 1.7f));
                        destinationLabel.preferredWidth.set(platformLabel.x.get() - 1 - pX - 7);
                        destinationLabel.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
                        destinationLabel.color.set(getDisplaySettings(blockEntity).getFontColor());
                    });
                }
            });
        } else {
            DLUtils.doIfNotNull(scheduleLines, x -> {
                int totalStationsCount = displayData.getStopsFromCurrentStation().size();
                int linesCount = Math.min(scheduleLines.length, totalStationsCount);
                listDestinationLabelX = 0;
                for (int i = 0; i < linesCount; i++) {
                    final int j = i;
                    int k = i >= linesCount - 1 ? totalStationsCount - 1 : i;
                    DLUtils.doIfNotNull(scheduleLines[i], a -> {
                        TrainStopDisplayData stop = displayData.getStopsFromCurrentStation().get(k);
                        boolean showDeparture = displayData.isWaitingAtStation() && displayData.getCurrentScheduleIndex() == stop.getStationEntryIndex();
                        
                        BERLabel scheduledTimeLabel = a[LineComponent.SCHEDULED_TIME.i()];
                        BERLabel realTimeLabel = a[LineComponent.REAL_TIME.i()];
                        
                        if (realTimeLabel != null) {
                            scheduledTimeLabel.text.set(TextUtils.text(ModUtils.formatTime(showDeparture ? stop.getScheduledDepartureTime() : stop.getScheduledArrivalTime(), getDisplaySettings(blockEntity).getTimeDisplay() == ETimeDisplay.ETA)));
                            scheduledTimeLabel.color.set(getDisplaySettings(blockEntity).getFontColor());

                            realTimeLabel.text.set(TextUtils.text(ModUtils.formatTime(showDeparture ? stop.getRealTimeDepartureTime() : stop.getRealTimeArrivalTime(), getDisplaySettings(blockEntity).getTimeDisplay() == ETimeDisplay.ETA)));
                            realTimeLabel.position.set(Point.of(scheduledTimeLabel.x.get() + scheduledTimeLabel.getRenderedWidth() + 1, 6 + j * 2));
                            realTimeLabel.color.set(stop.isArrivalDelayed() ? Constants.COLOR_DELAYED : Constants.COLOR_ON_TIME);
                        } else {
                            scheduledTimeLabel.text.set(TextUtils.text(ModUtils.formatTime(stop.getRealTimeArrivalTime(), getDisplaySettings(blockEntity).getTimeDisplay() == ETimeDisplay.ETA)));
                            scheduledTimeLabel.color.set(stop.isArrivalDelayed() ? Constants.COLOR_DELAYED : Constants.COLOR_ON_TIME);
                        }
                        listDestinationLabelX = Math.max(listDestinationLabelX, scheduledTimeLabel.x.get() + scheduledTimeLabel.getRenderedWidth() + 3 + (realTimeLabel == null ? 0 : realTimeLabel.getRenderedWidth() + 1));
                    });
                }

                for (int i = 0; i < linesCount; i++) {
                    final int j = i;
                    int k = i >= linesCount - 1 ? totalStationsCount - 1 : i;
                    DLUtils.doIfNotNull(scheduleLines[i], a -> {
                        TrainStopDisplayData stop = displayData.getStopsFromCurrentStation().get(k);
                        BERLabel destinationLabel = a[LineComponent.DESTINATION.i()];
                        destinationLabel.text.set(TextUtils.text(stop.getRealTimeStation().tagName()).withStyle(j >= linesCount - 1 ? ChatFormatting.BOLD : ChatFormatting.RESET));
                        destinationLabel.position.set(Point.of(listDestinationLabelX, 6 + j * 2));
                        destinationLabel.preferredWidth.set(blockEntity.getXSizeScaled() * 16 - 3 - listDestinationLabelX);
                        destinationLabel.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
                        destinationLabel.horizontalScrollingSpeed.set(SCROLLING_SPEED);
                        destinationLabel.color.set(getDisplaySettings(blockEntity).getFontColor());
                    });
                }
            });
        }
    }
    
    private BERLabel[] createStationLine(AdvancedDisplayBlockEntity blockEntity, int index) {
        BERLabel timeLabel = new BERLabel();
        timeLabel.position.set(Point.of(3, 6 + index * 2));
        timeLabel.horizontalScale.set(Pair.of(0.1f, 0.15f));
        timeLabel.verticalScale.set(Pair.of(0.15f, 0.15f));
        timeLabel.preferredWidth.set(6f);
        
        BERLabel realTimeLabel = null;
        if (blockEntity.getXSizeScaled() > 1) {
            realTimeLabel = new BERLabel();
            realTimeLabel.horizontalScale.set(Pair.of(0.1f, 0.15f));
            realTimeLabel.verticalScale.set(Pair.of(0.15f, 0.15f));
            realTimeLabel.preferredWidth.set(6f);
        }
        BERLabel destinationLabel = new BERLabel();
        destinationLabel.horizontalScale.set(Pair.of(0.08f, 0.15f));
        destinationLabel.verticalScale.set(Pair.of(0.15f, 0.15f));

        return new BERLabel[] { timeLabel, realTimeLabel, null, destinationLabel };
    }

    private BERLabel[] createNextConnectionsLine(AdvancedDisplayBlockEntity blockEntity, int index) {
        BERLabel timeLabel = new BERLabel();
        timeLabel.position.set(Point.of(3, 7 + index * 2));
        timeLabel.horizontalScale.set(Pair.of(0.1f, 0.15f));
        timeLabel.verticalScale.set(Pair.of(0.15f, 0.15f));
        timeLabel.preferredWidth.set(6f);
        
        BERLabel realTimeLabel = null;
        if (blockEntity.getXSizeScaled() > 2) {
            realTimeLabel = new BERLabel();
            realTimeLabel.horizontalScale.set(Pair.of(0.1f, 0.15f));
            realTimeLabel.verticalScale.set(Pair.of(0.15f, 0.15f));
            realTimeLabel.preferredWidth.set(6f);
        }
        BERLabel trainNameLabel = new BERLabel();
        trainNameLabel.horizontalScale.set(Pair.of(0.08f, 0.15f));
        trainNameLabel.verticalScale.set(Pair.of(0.15f, 0.15f));
        
        BERLabel destinationLabel = new BERLabel();        
        destinationLabel.horizontalScale.set(Pair.of(0.08f, 0.15f));
        destinationLabel.verticalScale.set(Pair.of(0.15f, 0.15f));
        
        BERLabel platformLabel = new BERLabel();
        platformLabel.horizontalScale.set(Pair.of(0.08f, 0.15f));
        platformLabel.verticalScale.set(Pair.of(0.15f, 0.15f));

        return new BERLabel[] { timeLabel, realTimeLabel, trainNameLabel, destinationLabel, platformLabel };
    }

    private void updateLayout(AdvancedDisplayBlockEntity blockEntity, TrainDisplayData data) {
        if (shouldRenderNextConnections()) {
            for (int i = 0; i < MAX_LINES - 1; i++) {
                this.nextConnectionsLines[i] = createNextConnectionsLine(blockEntity, i);
            }
            return;
        }

        int totalStationsCount = data.getStopsFromCurrentStation().size();
        int linesCount = Math.min(MAX_LINES, totalStationsCount);
        this.scheduleLines = new BERLabel[linesCount][];
        for (int i = 0; i < linesCount; i++) {
            this.scheduleLines[i] = createStationLine(blockEntity, i);
        }
    }

    private static enum LineComponent {
        SCHEDULED_TIME(0),
        REAL_TIME(1),
        TRAIN_NAME(2),
        DESTINATION(3),
        PLATFORM(4);
        int i;
        LineComponent(int i) {
            this.i = i;
        }
        public int i() {
            return i;
        }
    }
}
