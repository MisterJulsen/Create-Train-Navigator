package de.mrjulsen.crn.client.ber.variants;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity.EUpdateReason;
import de.mrjulsen.crn.block.display.AdvancedDisplaySource.ETimeDisplay;
import de.mrjulsen.crn.client.CRNGui;
import de.mrjulsen.crn.client.ber.AdvancedDisplayRenderInstance;
import de.mrjulsen.crn.client.ber.IBERRenderSubtype;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.lang.ELanguage;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.TrainExitSide;
import de.mrjulsen.crn.data.train.portable.NextConnectionsDisplayData;
import de.mrjulsen.crn.data.train.portable.TrainDisplayData;
import de.mrjulsen.crn.data.train.portable.TrainStopDisplayData;
import de.mrjulsen.crn.registry.ModAccessorTypes;
import de.mrjulsen.crn.registry.data.NextConnectionsRequestData;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.ber.BERGraphics;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel.BoundsHitReaction;
import de.mrjulsen.mcdragonlib.client.util.BERUtils;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.accessor.DataAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class BERPassengerInfoInformative implements IBERRenderSubtype<AdvancedDisplayBlockEntity, AdvancedDisplayRenderInstance, Boolean> {

    private static final ResourceLocation CARRIAGE_ICON = new ResourceLocation("create:textures/gui/assemble.png");  
    private static final ResourceLocation ICONS = new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "textures/gui/icons.png");  
    private static final String keyDate = "gui.createrailwaysnavigator.route_overview.date";
    private static final String keyNextStop = "gui.createrailwaysnavigator.route_overview.next_stop";
    private static final String keyNextConnections = "gui.createrailwaysnavigator.route_overview.next_connections";
    private static final int MAX_LINES = 4;

    private NextConnectionsDisplayData nextConnections = null;
    private boolean nextStopAnnounced = false;
    private TrainExitSide exitSide = TrainExitSide.UNKNOWN;

    private final BERLabel timeLabel = new BERLabel()
        .setScale(0.25f, 0.25f)
        .setYScale(0.25f)
    ;
    private final BERLabel carriageLabel = new BERLabel()
        .setScale(0.25f, 0.25f)
        .setYScale(0.25f)
    ;
    private final BERLabel trainLineLabel = new BERLabel()
        .setPos(3, 2.5f)
        .setScale(0.25f, 0.15f)
        .setYScale(0.25f)
    ;
    private final BERLabel speedLabel = new BERLabel()
        .setPos(3, 6)
        .setScale(0.25f, 0.2f)
        .setYScale(0.30f)
        .setCentered(true)
    ;
    private final BERLabel dateLabel = new BERLabel()
        .setPos(3, 9)
        .setScale(0.2f, 0.15f)
        .setYScale(0.2f)
        .setCentered(true)
    ;
    private final BERLabel carriageInfoLabel = new BERLabel()
        .setPos(4.5f, 11)
        .setScale(0.2f, 0.15f)
        .setYScale(0.2f)
        .setCentered(true)
    ;
    private final BERLabel nextConnectionsTitleLabel = new BERLabel(ELanguage.translate(keyNextConnections).withStyle(ChatFormatting.BOLD))
        .setPos(3, 5.5f)
        .setScale(0.15f, 0.15f)
        .setYScale(0.15f)
    ;
    private final BERLabel pageIndicatorLabel = new BERLabel()
        .setPos(3, 12.5f)
        .setScale(0.15f, 0.15f)
        .setYScale(0.15f)
        .setCentered(true)
    ;

    private BERLabel[][] scheduleLines;
    private BERLabel[][] nextConnectionsLines = new BERLabel[MAX_LINES - 1][];


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
    public void renderTick(float deltaTime) {
        timeLabel.renderTick();
        dateLabel.renderTick();
        trainLineLabel.renderTick();
        speedLabel.renderTick();
        carriageInfoLabel.renderTick();
        nextConnectionsTitleLabel.renderTick();
        pageIndicatorLabel.renderTick();
        DLUtils.doIfNotNull(scheduleLines, x -> {
            for (int i = 0; i < x.length; i++) {
                DLUtils.doIfNotNull(x[i], y -> {
                    for (int j = 0; j < y.length; j++) {
                        DLUtils.doIfNotNull(y[j], z -> z.renderTick());
                    }
                });
            }
        });
        DLUtils.doIfNotNull(nextConnectionsLines, x -> {
            for (int i = 0; i < x.length; i++) {
                DLUtils.doIfNotNull(x[i], y -> {
                    for (int j = 0; j < y.length; j++) {
                        DLUtils.doIfNotNull(y[j], z -> z.renderTick());
                    }
                });
            }
        });
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent) {
        timeLabel
            .setText(blockEntity.getXSizeScaled() > 1 && !nextStopAnnounced ? TextUtils.text(ModUtils.formatTime(DragonLib.getCurrentWorldTime(), blockEntity.getTimeDisplay() == ETimeDisplay.ETA)).withStyle(ChatFormatting.BOLD) : TextUtils.empty())
            .setPos(blockEntity.getXSizeScaled() * 16 - 3 - timeLabel.getTextWidth() - (this.exitSide != TrainExitSide.UNKNOWN ? 4 : 0), 2.5f)            
            .setColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
        ;
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
            timeLabel.render(graphics, light);
            BERUtils.renderTexture(
                ICONS,
                graphics,
                false,
                timeLabel.getX() - 2.5f,
                2.5f,
                0.0f,
                2,
                2,
                uv255 * 227,
                uv255 * 19,
                uv255 * (227 + 10),
                uv255 * (19 + 10),
                graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING),
                (0xFF << 24) | (graphics.blockEntity().getColor() & 0x00FFFFFF),
                light
            );
        }

        if (graphics.blockEntity().getTrainData() == null || graphics.blockEntity().getTrainData().isEmpty()) {
            graphics.poseStack().popPose();
            return;
        }

        trainLineLabel.render(graphics, light);

        // Carriage label
        if (graphics.blockEntity().getXSizeScaled() > 2 && !nextStopAnnounced) {            
            carriageLabel.render(graphics, light);
            BERUtils.renderTexture(
                CARRIAGE_ICON,
                graphics,
                false,
                carriageLabel.getX() - 3.5f,
                2.5f,
                0.0f,
                3,
                2,
                uv255 * 22,
                uv255 * 231,
                uv255 * (22 + 13),
                uv255 * (231 + 5),
                graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING),
                (0xFF << 24) | (graphics.blockEntity().getColor() & 0x00FFFFFF),
                light
            );
        }
        graphics.poseStack().popPose();

        if (nextStopAnnounced || graphics.blockEntity().getTrainData().isWaitingAtStation()) {            
            switch (side) {
                case RIGHT:
                    BERUtils.renderTexture(
                        ModGuiIcons.ICON_LOCATION,
                        graphics,
                        false,
                        graphics.blockEntity().getXSizeScaled() * 16 - 3 - 3,
                        2.05f,
                        0,
                        3,
                        3,
                        uv255 * ModGuiIcons.ARROW_RIGHT.getU(),
                        uv255 * ModGuiIcons.ARROW_RIGHT.getV(),
                        uv255 * (ModGuiIcons.ARROW_RIGHT.getU() + ModGuiIcons.ICON_SIZE),
                        uv255 * (ModGuiIcons.ARROW_RIGHT.getV() + ModGuiIcons.ICON_SIZE),
                        graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING),
                        (0xFF << 24) | (graphics.blockEntity().getColor()),
                        light
                    );
                    break;
                case LEFT:
                    BERUtils.renderTexture(
                        ModGuiIcons.ICON_LOCATION,
                        graphics,
                        false,
                        3,
                        2.05f,
                        0,
                        3,
                        3,
                        uv255 * ModGuiIcons.ARROW_LEFT.getU(),
                        uv255 * ModGuiIcons.ARROW_LEFT.getV(),
                        uv255 * (ModGuiIcons.ARROW_LEFT.getU() + ModGuiIcons.ICON_SIZE),
                        uv255 * (ModGuiIcons.ARROW_LEFT.getV() + ModGuiIcons.ICON_SIZE),
                        graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING),
                        (0xFF << 24) | (graphics.blockEntity().getColor()),
                        light
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
        BERUtils.fillColor(graphics, 2.5f, 5.0f, 0.01f, graphics.blockEntity().getXSizeScaled() * 16 - 5, 0.25f, (0xFF << 24) | (graphics.blockEntity().getColor() & 0x00FFFFFF), graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING), light);

        if (graphics.blockEntity().getTrainData() == null || graphics.blockEntity().getTrainData().isEmpty()) {
            return;
        }

        if (shouldRenderNextConnections()) {
            DLUtils.doIfNotNull(nextConnectionsLines, x -> {
                for (int i = 0; i < x.length; i++) {
                    DLUtils.doIfNotNull(x[i], a -> {
                        for (int j = 0; j < a.length; j++) {
                            DLUtils.doIfNotNull(a[j], b -> b.render(graphics, light));
                        }
                    });
                }
            });
            nextConnectionsTitleLabel.render(graphics, light);
            pageIndicatorLabel.render(graphics, light);
        } else if (DragonLib.getCurrentWorldTime() % 500 < 200 && !graphics.blockEntity().getTrainData().isWaitingAtStation()) {
            // render stats
            speedLabel.render(graphics, light);
            dateLabel.render(graphics, light);
            carriageInfoLabel.render(graphics, light);
            BERUtils.renderTexture(
                CARRIAGE_ICON,
                graphics,
                false,
                graphics.blockEntity().getXSizeScaled() * 16 / 2f - carriageInfoLabel.getTextWidth() / 2f - 1.5f,
                carriageInfoLabel.getY(),
                0.0f,
                2.25f,
                1.5f,
                uv255 * 22,
                uv255 * 231,
                uv255 * (22 + 13),
                uv255 * (231 + 5),
                graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING),
                (0xFF << 24) | (graphics.blockEntity().getColor() & 0x00FFFFFF),
                light
            );
        } else {
            // Render schedule
            DLUtils.doIfNotNull(scheduleLines, x -> {
                for (int i = 0; i < x.length; i++) {
                    final int idx = i;
                    DLUtils.doIfNotNull(x[i], a -> {
                        for (int j = 0; j < a.length; j++) {
                            DLUtils.doIfNotNull(a[j], b -> b.render(graphics, light));
                        }

                        final float uv32 = 1f / CRNGui.GUI_WIDTH;
                        if (idx == 0 && scheduleLines.length > 1) {
                            BERUtils.renderTexture(
                                CRNGui.GUI,
                                graphics,
                                false,
                                (a[LineComponent.REAL_TIME.i()] == null ? a[LineComponent.SCHEDULED_TIME.i()].getX() + a[LineComponent.SCHEDULED_TIME.i()].getMaxWidth() : a[LineComponent.REAL_TIME.i()].getX() + a[LineComponent.REAL_TIME.i()].getMaxWidth()) - 1,
                                a[LineComponent.SCHEDULED_TIME.i()].getY() - 1,
                                0.0f,
                                1,
                                2,
                                uv32 * 21,
                                uv32 * 30,
                                uv32 * (21 + 7),
                                uv32 * (30 + 14),
                                graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING),
                                (0xFF << 24) | (graphics.blockEntity().getColor() & 0x00FFFFFF),
                                light
                            );
                        } else if (idx >= MAX_LINES - 1) {
                            BERUtils.renderTexture(
                                CRNGui.GUI,
                                graphics,
                                false,
                                (a[LineComponent.REAL_TIME.i()] == null ? a[LineComponent.SCHEDULED_TIME.i()].getX() + a[LineComponent.SCHEDULED_TIME.i()].getMaxWidth() : a[LineComponent.REAL_TIME.i()].getX() + a[LineComponent.REAL_TIME.i()].getMaxWidth()) - 1,
                                a[LineComponent.SCHEDULED_TIME.i()].getY() - 1,
                                0.0f,
                                1,
                                2,
                                uv32 * 35,
                                uv32 * 30,
                                uv32 * (35 + 7),
                                uv32 * (30 + 14),
                                graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING),
                                (0xFF << 24) | (graphics.blockEntity().getColor() & 0x00FFFFFF),
                                light
                            );
                        } else {
                            BERUtils.renderTexture(
                                CRNGui.GUI,
                                graphics,
                                false,
                                (a[LineComponent.REAL_TIME.i()] == null ? a[LineComponent.SCHEDULED_TIME.i()].getX() + a[LineComponent.SCHEDULED_TIME.i()].getMaxWidth() : a[LineComponent.REAL_TIME.i()].getX() + a[LineComponent.REAL_TIME.i()].getMaxWidth()) - 1,
                                a[LineComponent.SCHEDULED_TIME.i()].getY() - 1,
                                0.0f,
                                1,
                                2,
                                uv32 * 28,
                                uv32 * 30,
                                uv32 * (28 + 7),
                                uv32 * (30 + 14),
                                graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING),
                                (0xFF << 24) | (graphics.blockEntity().getColor() & 0x00FFFFFF),
                                light
                            );
                        }
                    });
                }
            });
        }
        
    }

    @Override
    public void update(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent, EUpdateReason reason) {
        if (blockEntity.getTrainData() == null || blockEntity.getTrainData().isEmpty()) {
            return;
        }
        TrainDisplayData data = blockEntity.getTrainData();
        boolean wasNextStopAnnounced = nextStopAnnounced;
        nextStopAnnounced = !data.isWaitingAtStation() && data.getNextStop().isPresent() && data.getNextStop().get().getRealTimeArrivalTime() - DragonLib.getCurrentWorldTime() < ModClientConfig.NEXT_STOP_ANNOUNCEMENT.get();
        this.exitSide = !nextStopAnnounced && !data.isWaitingAtStation() ? TrainExitSide.UNKNOWN : (data.isWaitingAtStation() ? exitSide : blockEntity.relativeExitDirection.get());

        if (blockEntity.getXSizeScaled() > 1 && nextStopAnnounced && !wasNextStopAnnounced && data.getNextStop().isPresent()) {
            DataAccessor.getFromServer(new NextConnectionsRequestData(data.getNextStop().get().getName(), data.getTrainData().getId()), ModAccessorTypes.GET_NEXT_CONNECTIONS_DISPLAY_DATA, (res) -> {
                nextConnections = res;
                updateLayout(blockEntity, data);
                updateContent(blockEntity, data);
            });
        }

        if (reason == EUpdateReason.LAYOUT_CHANGED || !nextStopAnnounced) {
            updateLayout(blockEntity, data);
            nextConnections = null;
        }
        updateContent(blockEntity, data);
    }

    private void updateContent(AdvancedDisplayBlockEntity blockEntity, TrainDisplayData data) {        
        timeLabel
            .setText(blockEntity.getXSizeScaled() > 1 && !nextStopAnnounced ? TextUtils.text(ModUtils.formatTime(DragonLib.getCurrentWorldTime(), blockEntity.getTimeDisplay() == ETimeDisplay.ETA)).withStyle(ChatFormatting.BOLD) : TextUtils.empty())
            .setPos(blockEntity.getXSizeScaled() * 16 - 3 - timeLabel.getTextWidth() - (this.exitSide != TrainExitSide.UNKNOWN ? 4 : 0), 2.5f)
            .setColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
        ;
        carriageLabel
            .setText(blockEntity.getXSizeScaled() > 1 && !nextStopAnnounced ? TextUtils.text(String.format("%02d", blockEntity.getCarriageData().index() + 1)).withStyle(ChatFormatting.BOLD) : TextUtils.empty())
            .setPos(timeLabel.getX() - 4 - carriageLabel.getTextWidth(), 2.5f)
            .setColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
        ;
        trainLineLabel
            .setText(nextStopAnnounced ? ELanguage.translate(keyNextStop, data.getNextStop().get().getName()) : TextUtils.text(data.getTrainData().getName()).withStyle(ChatFormatting.BOLD))
            .setMaxWidth(blockEntity.getXSizeScaled() * 16 - 6 - (blockEntity.getXSizeScaled() > 1 && !nextStopAnnounced ? timeLabel.getTextWidth() - 4 : 0) - (blockEntity.getXSizeScaled() > 1 && !nextStopAnnounced ? carriageLabel.getTextWidth() - 5 : 0) - (this.exitSide != TrainExitSide.UNKNOWN ? 4 : 0), BoundsHitReaction.SCALE_SCROLL)
            .setColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
        ;
        speedLabel
            .setText(ModUtils.calcSpeedString(data.getSpeed(), ModClientConfig.SPEED_UNIT.get()).withStyle(ChatFormatting.BOLD))
            .setMaxWidth(blockEntity.getXSizeScaled() * 16 - 6, BoundsHitReaction.CUT_OFF)
            .setColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
        ;
        dateLabel
            .setText(ELanguage.translate(keyDate, blockEntity.getLevel().getDayTime() / Level.TICKS_PER_DAY, ModUtils.formatTime(DragonLib.getCurrentWorldTime(), blockEntity.getTimeDisplay() == ETimeDisplay.ETA)))
            .setMaxWidth(blockEntity.getXSizeScaled() * 16 - 6, BoundsHitReaction.CUT_OFF)
            .setColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
        ;
        carriageInfoLabel
            .setText(TextUtils.text(String.format("%02d", blockEntity.getCarriageData().index() + 1)))
            .setMaxWidth(blockEntity.getXSizeScaled() * 16 - 6, BoundsHitReaction.CUT_OFF)
            .setColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
        ;

        if (shouldRenderNextConnections() && !nextConnections.getConnections().isEmpty()) {
            final int pages = (int)Math.ceil((float)nextConnections.getConnections().size() / (MAX_LINES - 1));
            final int page = (int)((DragonLib.getCurrentWorldTime() % (100 * pages)) / 100);
            pageIndicatorLabel
                .setText(TextUtils.text(generatePageIndexString(page, pages)))
                .setMaxWidth(blockEntity.getXSizeScaled() * 16 - 6, BoundsHitReaction.CUT_OFF)
                .setColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
            ;
            nextConnectionsTitleLabel
                .setMaxWidth(blockEntity.getXSizeScaled() * 16 - 6, BoundsHitReaction.CUT_OFF)
                .setColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
            ;
            DLUtils.doIfNotNull(nextConnectionsLines, x -> {
                for (int i = 0; i < MAX_LINES - 1; i++) {
                    final int k = i;
                    final int connectionIdx = i + (page * (MAX_LINES - 1));
                    DLUtils.doIfNotNull(nextConnectionsLines[i], a -> {
                        if (connectionIdx >= nextConnections.getConnections().size()) {
                            a[LineComponent.SCHEDULED_TIME.i()].setText(TextUtils.empty());
                            if (a[LineComponent.REAL_TIME.i()] != null) {
                                a[LineComponent.REAL_TIME.i()].setText(TextUtils.empty());
                            }
                            a[LineComponent.TRAIN_NAME.i()].setText(TextUtils.empty());
                            a[LineComponent.DESTINATION.i()].setText(TextUtils.empty());
                            a[LineComponent.PLATFORM.i()].setText(TextUtils.empty());
                            return;
                        }

                        TrainStopDisplayData stop = nextConnections.getConnections().get(connectionIdx);
                        a[LineComponent.PLATFORM.i()]
                            .setText(TextUtils.text(stop.getStationInfo().platform()))
                            .setPos(blockEntity.getXSizeScaled() * 16 - 3 - a[LineComponent.PLATFORM.i()].getTextWidth(), 7.5f + k * 1.7f)
                            .setColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
                        ;
                        if (a[LineComponent.REAL_TIME.i()] != null) {                              
                            a[LineComponent.SCHEDULED_TIME.i()]
                                .setPos(3, 7.5f + k * 1.7f)
                                .setText(TextUtils.text(ModUtils.formatTime(stop.getScheduledDepartureTime(), blockEntity.getTimeDisplay() == ETimeDisplay.ETA)))
                                .setColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
                            ;                        
                            a[LineComponent.REAL_TIME.i()]
                                .setPos(a[LineComponent.SCHEDULED_TIME.i()].getX() + a[LineComponent.SCHEDULED_TIME.i()].getTextWidth() + 1, 7.5f + k * 1.7f)
                                .setText(TextUtils.text(ModUtils.formatTime(stop.getRealTimeDepartureTime(), blockEntity.getTimeDisplay() == ETimeDisplay.ETA)))
                                .setColor(stop.isDepartureDelayed() ? Constants.COLOR_DELAYED : Constants.COLOR_ON_TIME)
                            ;
                        } else {                            
                            a[LineComponent.SCHEDULED_TIME.i()]
                                .setPos(3, 7.5f + k * 1.7f)
                                .setText(TextUtils.text(ModUtils.formatTime(stop.getRealTimeDepartureTime(), blockEntity.getTimeDisplay() == ETimeDisplay.ETA)))
                                .setColor(stop.isDepartureDelayed() ? Constants.COLOR_DELAYED : Constants.COLOR_ON_TIME)
                            ;
                        }
                        float pX = a[LineComponent.SCHEDULED_TIME.i()].getX() + a[LineComponent.SCHEDULED_TIME.i()].getTextWidth() + 1 + (a[LineComponent.REAL_TIME.i()] == null ? 0 : a[LineComponent.REAL_TIME.i()].getTextWidth() + 1);
                        a[LineComponent.TRAIN_NAME.i()]
                            .setPos(pX, 7.5f + k * 1.7f)
                            .setText(TextUtils.text(stop.getTrainName()))
                            .setMaxWidth(6, BoundsHitReaction.CUT_OFF)
                            .setColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
                        ;
                        a[LineComponent.DESTINATION.i()]
                            .setPos(pX + 7, 7.5f + k * 1.7f)
                            .setText(TextUtils.text(stop.getDestination()))
                            .setMaxWidth(a[LineComponent.PLATFORM.i()].getX() - 1 - pX - 7, BoundsHitReaction.SCALE_SCROLL)
                            .setColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
                        ;
                    });
                }
            });
        } else {            
            DLUtils.doIfNotNull(scheduleLines, x -> {
                int totalStationsCount = data.getStopsFromCurrentStation().size();
                int linesCount = Math.min(scheduleLines.length, totalStationsCount);
                for (int i = 0; i < linesCount; i++) {
                    final int j = i;
                    int k = i >= linesCount - 1 ? totalStationsCount - 1 : i;
                    DLUtils.doIfNotNull(scheduleLines[i], a -> {
                        TrainStopDisplayData stop = data.getStopsFromCurrentStation().get(k);
                        if (a[LineComponent.REAL_TIME.i()] != null) {                            
                            a[LineComponent.SCHEDULED_TIME.i()]
                                .setText(TextUtils.text(ModUtils.formatTime(stop.getScheduledArrivalTime(), blockEntity.getTimeDisplay() == ETimeDisplay.ETA)))
                                .setColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
                            ;
                            a[LineComponent.REAL_TIME.i()]
                                .setPos(a[LineComponent.SCHEDULED_TIME.i()].getX() + a[LineComponent.SCHEDULED_TIME.i()].getTextWidth() + 1, 6 + j * 2)
                                .setText(TextUtils.text(ModUtils.formatTime(stop.getRealTimeArrivalTime(), blockEntity.getTimeDisplay() == ETimeDisplay.ETA)))
                                .setColor(stop.isArrivalDelayed() ? Constants.COLOR_DELAYED : Constants.COLOR_ON_TIME)
                            ;
                        } else {
                            a[LineComponent.SCHEDULED_TIME.i()]
                                .setText(TextUtils.text(ModUtils.formatTime(stop.getRealTimeArrivalTime(), blockEntity.getTimeDisplay() == ETimeDisplay.ETA)))
                                .setColor(stop.isArrivalDelayed() ? Constants.COLOR_DELAYED : Constants.COLOR_ON_TIME)
                            ;
                        }
                        float pX = a[LineComponent.SCHEDULED_TIME.i()].getX() + a[LineComponent.SCHEDULED_TIME.i()].getTextWidth() + 3 + (a[LineComponent.REAL_TIME.i()] == null ? 0 : a[LineComponent.REAL_TIME.i()].getTextWidth() + 1);
                        a[LineComponent.DESTINATION.i()]
                            .setPos(pX, 6 + j * 2)
                            .setText(TextUtils.text(stop.getName()).withStyle(j >= linesCount - 1 ? ChatFormatting.BOLD : ChatFormatting.RESET))
                            .setMaxWidth(blockEntity.getXSizeScaled() * 16 - 3 - pX, BoundsHitReaction.SCALE_SCROLL)
                            .setColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
                        ;
                    });
                }
            });
        }
    }
    
    private BERLabel[] createStationLine(AdvancedDisplayBlockEntity blockEntity, int index) {
        BERLabel timeLabel = new BERLabel()
            .setPos(3, 6 + index * 2)
            .setScale(0.15f, 0.1f)
            .setYScale(0.15f)
            .setMaxWidth(6, BoundsHitReaction.CUT_OFF)
        ;
        BERLabel realTimeLabel = null;
        if (blockEntity.getXSizeScaled() > 1) {
            realTimeLabel = new BERLabel()
                .setScale(0.15f, 0.1f)
                .setYScale(0.15f)
                .setMaxWidth(6, BoundsHitReaction.CUT_OFF)
            ;
        }
        BERLabel destinationLabel = new BERLabel()
            .setScale(0.15f, 0.08f)
            .setYScale(0.15f)
        ;

        return new BERLabel[] { timeLabel, realTimeLabel, null, destinationLabel };
    }

    private BERLabel[] createNextConnectionsLine(AdvancedDisplayBlockEntity blockEntity, int index) {
        BERLabel timeLabel = new BERLabel()
            .setPos(3, 7 + index * 2)
            .setScale(0.15f, 0.1f)
            .setYScale(0.15f)
            .setMaxWidth(6, BoundsHitReaction.CUT_OFF)
        ;
        BERLabel realTimeLabel = null;
        if (blockEntity.getXSizeScaled() > 2) {
            realTimeLabel = new BERLabel()
                .setScale(0.15f, 0.1f)
                .setYScale(0.15f)
                .setMaxWidth(6, BoundsHitReaction.CUT_OFF)
            ;
        }
        BERLabel trainNameLabel = new BERLabel()
            .setScale(0.15f, 0.08f)
            .setYScale(0.15f)
        ;
        BERLabel destinationLabel = new BERLabel()
            .setScale(0.15f, 0.08f)
            .setYScale(0.15f)
        ;
        BERLabel platformLabel = new BERLabel()
            .setScale(0.15f, 0.08f)
            .setYScale(0.15f)
        ;

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
