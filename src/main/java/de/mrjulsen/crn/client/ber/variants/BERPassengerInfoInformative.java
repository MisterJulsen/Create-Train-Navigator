package de.mrjulsen.crn.client.ber.variants;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.blaze3d.vertex.PoseStack;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.block.be.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.client.ber.AdvancedDisplayRenderInstance;
import de.mrjulsen.crn.client.ber.base.BERText;
import de.mrjulsen.crn.client.ber.base.BERText.TextTransformation;
import de.mrjulsen.crn.client.ber.base.IBlockEntityRendererInstance.BlockEntityRendererContext;
import de.mrjulsen.crn.client.ber.base.IBlockEntityRendererInstance.EUpdateReason;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.SimpleTrainConnection;
import de.mrjulsen.crn.data.DeparturePrediction.TrainExitSide;
import de.mrjulsen.crn.data.DeparturePrediction.SimpleDeparturePrediction;
import de.mrjulsen.crn.event.listeners.JourneyListener.State;
import de.mrjulsen.crn.network.InstanceManager;
import de.mrjulsen.crn.network.NetworkManager;
import de.mrjulsen.crn.network.packets.cts.NextConnectionsRequestPacket;
import de.mrjulsen.mcdragonlib.DragonLibConstants;
import de.mrjulsen.mcdragonlib.utils.TimeUtils;
import de.mrjulsen.mcdragonlib.utils.Utils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class BERPassengerInfoInformative implements IBERRenderSubtype<AdvancedDisplayBlockEntity, AdvancedDisplayRenderInstance, Boolean> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(ModMain.MOD_ID, "textures/gui/overview.png");
    private static final int TEX_ROUTE_PATH_U = 226;
    private static final int TEX_ROUTE_PATH_H = 14;    
    private static final float PANEL_LINE_HEIGHT = 2.0f;
    private static final float PANEL_Y_START = 5.75f;
    private static final int NEXT_CONNECTIONS_MAX_ENTRIES_PER_PAGE = 3;
    private static final int NEXT_CONNECTIONS_PAGE_TIMER = 100;

    private BERText timeLabel;
    private BERText titleLabel;
    private State state = State.WHILE_TRAVELING;

    // data
    private List<SimpleTrainConnection> nextConnections;
    private long nextConnectionsRefreshTime = 0;
    private int nextConnectionsPage = 0;
    private int nextConnectionsMaxPage = 0;
    private int nextConnectionsTimer = 0;

    // Cache
    private TrainExitSide lastKnownExitSide = TrainExitSide.UNKNOWN;

    private static final String keyNextStop = "gui.createrailwaysnavigator.route_overview.next_stop";
    private static final Component textNextConnections = Utils.translate("gui.createrailwaysnavigator.route_overview.next_connections").withStyle(ChatFormatting.BOLD);

    @Override
    public boolean isSingleLined() {
        return false;
    }

    @SuppressWarnings("resource")
    @Override
    public void tick(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity pBlockEntity, AdvancedDisplayRenderInstance parent) {
        if (pBlockEntity.getTrainData() == null) {
            return;
        }

        boolean dirty = false;
        
        if (pBlockEntity.getTrainData().getNextStop().isPresent()) {
            if (this.state != State.WHILE_NEXT_STOP && pBlockEntity.getTrainData().getNextStop().get().departureTicks() <= 0) {
                this.state = State.WHILE_NEXT_STOP;
                dirty = true;
            } else if (this.state != State.BEFORE_NEXT_STOP && pBlockEntity.getTrainData().getNextStop().get().departureTicks() <= ModClientConfig.NEXT_STOP_ANNOUNCEMENT.get() && pBlockEntity.getTrainData().getNextStop().get().departureTicks() > 0) {
                this.state = State.BEFORE_NEXT_STOP;
                this.nextConnections = null;
                long id = InstanceManager.registerClientNextConnectionsResponseAction((data, refreshTime) -> {
                    this.nextConnections = new ArrayList<>(data);
                    nextConnectionsPage = 0;
                    nextConnectionsTimer = 0;
                    nextConnectionsRefreshTime = refreshTime;
                    if (data != null && !data.isEmpty()) {
                        nextConnectionsMaxPage = (int)(nextConnections.size() / NEXT_CONNECTIONS_MAX_ENTRIES_PER_PAGE + (nextConnections.size() % NEXT_CONNECTIONS_MAX_ENTRIES_PER_PAGE == 0 ? 0 : 1));
                        parent.labels.clear();
                        updateNextConnections(level, pos, state, pBlockEntity, parent);
                    }
                });
                NetworkManager.getInstance().sendToServer(Minecraft.getInstance().player.connection.getConnection(), new NextConnectionsRequestPacket(id, pBlockEntity.getTrainData().trainId(), pBlockEntity.getTrainData().getNextStop().get().stationName(), pBlockEntity.getTrainData().getNextStop().get().departureTicks()));
                dirty = true;
            } else if (this.state != State.WHILE_TRAVELING && pBlockEntity.getTrainData().getNextStop().get().departureTicks() > ModClientConfig.NEXT_STOP_ANNOUNCEMENT.get()) {
                this.state = State.WHILE_TRAVELING;
                dirty = true;
            }
        }

        if (this.state == State.BEFORE_NEXT_STOP && this.nextConnections != null && !this.nextConnections.isEmpty()) {
            nextConnectionsTimer++;
            if ((nextConnectionsTimer %= NEXT_CONNECTIONS_PAGE_TIMER) == 0) {
                nextConnectionsPage++;
                nextConnectionsPage %= nextConnectionsMaxPage;
                
                parent.labels.clear();
                updateNextConnections(level, pos, state, pBlockEntity, parent);
            }
        }

        if (this.state != State.WHILE_TRAVELING && lastKnownExitSide != pBlockEntity.relativeExitDirection.get()) {
            dirty = true;
        }
        lastKnownExitSide = pBlockEntity.relativeExitDirection.get();

        if (dirty) {
            update(level, pos, state, pBlockEntity, parent, EUpdateReason.DATA_CHANGED);
        } else {
            generateTimeLabel(level, pos, state, pBlockEntity, parent);
        }

        if (titleLabel != null) {
            titleLabel.tick();
        }
    }

    @Override
    public void update(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent, EUpdateReason reason) {
        if (blockEntity.getTrainData() == null) {
            return;
        }

        generateTitleBar(level, pos, state, blockEntity, parent);

        if (this.state == State.BEFORE_NEXT_STOP && this.nextConnections != null && !this.nextConnections.isEmpty()) {
            return;
        }

        parent.labels.clear();
        updateOverview(level, pos, state, blockEntity, parent);
    }

    @Override
    public void renderAdditional(BlockEntityRendererContext context, AdvancedDisplayBlockEntity pBlockEntity, AdvancedDisplayRenderInstance parent, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pOverlay, Boolean backSide) {

        // render title bar
        timeLabel.render(pPoseStack, pBufferSource, pBlockEntity.isGlowing() ? LightTexture.FULL_BRIGHT : pPackedLight);
        titleLabel.render(pPoseStack, pBufferSource, pBlockEntity.isGlowing() ? LightTexture.FULL_BRIGHT : pPackedLight);

        context.renderUtils().initRenderEngine();
        context.renderUtils().fillColor(pBufferSource, pBlockEntity, (0xFF << 24) | (pBlockEntity.getColor() & 0x00FFFFFF), pPoseStack, 2.5f, 4.75f, 0.0f, pBlockEntity.getXSizeScaled() * 16 - 5, 0.25f, pBlockEntity.getBlockState().getValue(HorizontalDirectionalBlock.FACING), pPackedLight);
        float uv = 1.0f / 256.0f;
        float y = 5f;

        if (notInService(pBlockEntity)) {
            return;
        }

        // Render route path
        if (this.state != State.BEFORE_NEXT_STOP || nextConnections == null || nextConnections.isEmpty()) {
            float tempH = PANEL_LINE_HEIGHT - 0.2857142f;
            context.renderUtils().renderTexture(
                TEXTURE,
                pBufferSource,
                pBlockEntity,
                pPoseStack,
                8,
                y,
                0.0f,
                1,
                tempH,
                uv * TEX_ROUTE_PATH_U,
                uv * 2,
                uv * (TEX_ROUTE_PATH_U + 7),
                uv * (TEX_ROUTE_PATH_H),
                pBlockEntity.getBlockState().getValue(HorizontalDirectionalBlock.FACING),
                0xFFFFFFFF,
                pPackedLight
            );
            y += tempH;
            for (int i = 0; i < 2 && i < pBlockEntity.getTrainData().stopovers().size(); i++) {
                context.renderUtils().renderTexture(
                    TEXTURE,
                    pBufferSource,
                    pBlockEntity,
                    pPoseStack,
                    8,
                    y,
                    0.0f,
                    1,
                    2,
                    uv * TEX_ROUTE_PATH_U,
                    uv * TEX_ROUTE_PATH_H * 1,
                    uv * (TEX_ROUTE_PATH_U + 7),
                    uv * (TEX_ROUTE_PATH_H * 2),
                    pBlockEntity.getBlockState().getValue(HorizontalDirectionalBlock.FACING),
                    0xFFFFFFFF,
                    pPackedLight
                );
                y += PANEL_LINE_HEIGHT;
            }
            
            if (pBlockEntity.getTrainData().predictions().size() > 1) {
                context.renderUtils().renderTexture(
                    TEXTURE,
                    pBufferSource,
                    pBlockEntity,
                    pPoseStack,
                    8,
                    y,
                    0.0f,
                    1,
                    2,
                    uv * TEX_ROUTE_PATH_U,
                    uv * TEX_ROUTE_PATH_H * 2,
                    uv * (TEX_ROUTE_PATH_U + 7),
                    uv * (TEX_ROUTE_PATH_H * 3),
                    pBlockEntity.getBlockState().getValue(HorizontalDirectionalBlock.FACING),
                    0xFFFFFFFF,
                    pPackedLight
                );
            }
            
        }

        // EXIT ARROW
        TrainExitSide side = pBlockEntity.relativeExitDirection.get();
        if (backSide) {
            side = side.getOpposite();
        }
        if (state != State.WHILE_TRAVELING && side != TrainExitSide.UNKNOWN) {
            context.renderUtils().renderTexture(
                ModGuiIcons.ICON_LOCATION,
                pBufferSource,
                pBlockEntity,
                pPoseStack,
                pBlockEntity.getXSizeScaled() * 16 - 3f - 2,
                2.25f,
                0,
                2.5f,
                2.5f,
                uv * (side == TrainExitSide.RIGHT ? ModGuiIcons.ARROW_RIGHT : ModGuiIcons.ARROW_LEFT).getU(),
                uv * (side == TrainExitSide.RIGHT ? ModGuiIcons.ARROW_RIGHT : ModGuiIcons.ARROW_LEFT).getV(),
                uv * ((side == TrainExitSide.RIGHT ? ModGuiIcons.ARROW_RIGHT : ModGuiIcons.ARROW_LEFT).getU() + ModGuiIcons.ICON_SIZE),
                uv * ((side == TrainExitSide.RIGHT ? ModGuiIcons.ARROW_RIGHT : ModGuiIcons.ARROW_LEFT).getV() + ModGuiIcons.ICON_SIZE),
                pBlockEntity.getBlockState().getValue(HorizontalDirectionalBlock.FACING),
                (0xFF << 24) | (pBlockEntity.getColor()),
                LightTexture.FULL_BRIGHT
            );
        }
    }

    private boolean notInService(AdvancedDisplayBlockEntity blockEntity) {
        Optional<SimpleDeparturePrediction> optPred = blockEntity.getTrainData().getNextStop();
        return !optPred.isPresent() || optPred.get().stationName() == null || optPred.get().stationName().isBlank();
    }

    private float generateTimeLabel(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent) {   
        float arrowOffset = (this.state != State.WHILE_TRAVELING && blockEntity.relativeExitDirection.get() != TrainExitSide.UNKNOWN ? 4 : 0);     
        float maxWidth = blockEntity.getXSizeScaled() * 16 - arrowOffset;
        MutableComponent line = Utils.text(TimeUtils.parseTime((int)(blockEntity.getLevel().getDayTime() % DragonLibConstants.TICKS_PER_DAY + Constants.TIME_SHIFT), ModClientConfig.TIME_FORMAT.get())).withStyle(ChatFormatting.BOLD);
        float rawTextWidth = Math.min(parent.getFontUtils().font.width(line) * 0.5f, maxWidth);
        float textWidth = rawTextWidth * 0.5f;
        timeLabel = parent.carriageIndexLabel = new BERText(parent.getFontUtils(), line, 0)
            .withIsCentered(false)
            .withMaxWidth(maxWidth, true)
            .withStretchScale(0.5f, 0.5f)
            .withColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
            .withPredefinedTextTransformation(new TextTransformation(blockEntity.getXSizeScaled() * 16 - 2.5f - textWidth - (this.state != State.WHILE_TRAVELING && blockEntity.relativeExitDirection.get() != TrainExitSide.UNKNOWN ? 4 : 0), 2.5f, 0.0f, 0.5f, 0.25f))
            .build();

        return rawTextWidth + arrowOffset;
    }

    private void generateTitleBar(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent) {
        int displayWidth = blockEntity.getXSizeScaled();

        float maxWidth = displayWidth * 16 - 6 - (this.state != State.WHILE_TRAVELING && blockEntity.relativeExitDirection.get() != TrainExitSide.UNKNOWN ? 4 : 0);
        maxWidth *= 2;
        float timeWidth = generateTimeLabel(level, pos, state, blockEntity, parent);
        MutableComponent line = Utils.text(blockEntity.getTrainData().trainName()).withStyle(ChatFormatting.BOLD);
        if (blockEntity.getTrainData().getNextStop().isPresent()) {
            switch (this.state) {
                case BEFORE_NEXT_STOP:
                    line = Utils.translate(keyNextStop, blockEntity.getTrainData().getNextStop().get().stationName());
                    break;
                case WHILE_NEXT_STOP:
                    line = Utils.translate(blockEntity.getTrainData().trainName() + " " + blockEntity.getTrainData().getNextStop().get().stationName()).withStyle(ChatFormatting.BOLD);
                    break;
                default:
                    break;
            }
        }

        if (titleLabel != null && line.getString().equals(titleLabel.getCurrentText().getString())) {
            return;
        }

        titleLabel = new BERText(parent.getFontUtils(), line, 0)
            .withIsCentered(false)
            .withMaxWidth(maxWidth - timeWidth, true)
            .withStretchScale(0.3f, 0.5f)
            .withStencil(0, maxWidth - timeWidth)
            .withCanScroll(true, 0.5f)
            .withColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
            .withPredefinedTextTransformation(new TextTransformation(3.0f, 2.5f, 0.0f, 0.5f, 0.25f))
            .build();
    }

    private void updateOverview(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent) {
        int displayWidth = blockEntity.getXSizeScaled();

        // ### CONTENT PANEL
        if (notInService(blockEntity)) {            
            return;
        }

        float y = PANEL_Y_START;
        // DESTINATION
        SimpleDeparturePrediction pred = blockEntity.getTrainData().getNextStop().get();
        float maxWidth = displayWidth * 16 - 12.5f;
        int rawTime = (int)(blockEntity.getLastRefreshedTime() % 24000 + pred.departureTicks() + Constants.TIME_SHIFT);
        MutableComponent line = Utils.text(TimeUtils.parseTime(rawTime - rawTime % ModClientConfig.REALTIME_PRECISION_THRESHOLD.get(), ModClientConfig.TIME_FORMAT.get()));
        parent.labels.add(new BERText(parent.getFontUtils(), line, 0)
            .withIsCentered(false)
            .withMaxWidth(4, true)
            .withStretchScale(0.08f, 0.14f)
            .withStencil(0, 7)
            .withColor((0xFF << 24) | (blockEntity.getColor()))
            .withPredefinedTextTransformation(new TextTransformation(3.0f, y + 0.3f, 0.0f, 1, 0.14f))
            .build()
        );


        line = Utils.text(pred.stationName()).withStyle(ChatFormatting.BOLD);
        parent.labels.add(new BERText(parent.getFontUtils(), line, 0)
            .withIsCentered(false)
            .withMaxWidth(maxWidth, true)
            .withStretchScale(0.15f, 0.2f)
            .withStencil(0, maxWidth)
            .withCanScroll(true, 0.5f)
            .withColor((0xFF << 24) | (blockEntity.getColor()))
            .withPredefinedTextTransformation(new TextTransformation(10.0f, y, 0.0f, 1, 0.2f))
            .build()
        );
        y += PANEL_LINE_HEIGHT;

        for (int i = 0; i < 2 && i < blockEntity.getTrainData().stopovers().size(); i++) {
            pred = blockEntity.getTrainData().stopovers().get(i);
            rawTime = (int)(blockEntity.getLastRefreshedTime() % 24000 + pred.departureTicks() + Constants.TIME_SHIFT);
            line = Utils.text(TimeUtils.parseTime(rawTime - rawTime % ModClientConfig.REALTIME_PRECISION_THRESHOLD.get(), ModClientConfig.TIME_FORMAT.get()));
            parent.labels.add(new BERText(parent.getFontUtils(), line, 0)
                .withIsCentered(false)
                .withMaxWidth(4, true)
                .withStretchScale(0.08f, 0.14f)
                .withStencil(0, 7)
                .withColor((0xFF << 24) | (blockEntity.getColor()))
                .withPredefinedTextTransformation(new TextTransformation(3.0f, y + 0.1f, 0.0f, 1, 0.14f))
                .build()
            );
            line = Utils.text(pred.stationName());
            parent.labels.add(new BERText(parent.getFontUtils(), line, 0)
                .withIsCentered(false)
                .withMaxWidth(maxWidth, true)
                .withStretchScale(0.15f, 0.16f)
                .withStencil(0, maxWidth)
                .withCanScroll(true, 0.5f)
                .withColor((0xFF << 24) | (blockEntity.getColor()))
                .withPredefinedTextTransformation(new TextTransformation(10.0f, y + 0.2f, 0.0f, 1, 0.16f))
                .build()
            );
            y += PANEL_LINE_HEIGHT;
        }

        if (blockEntity.getTrainData().predictions().size() <= 1) {
            return;
        }

        // DESTINATION
        pred = blockEntity.getTrainData().getLastStop().get();
        rawTime = (int)(blockEntity.getLastRefreshedTime() % 24000 + pred.departureTicks() + Constants.TIME_SHIFT);
        line = Utils.text(TimeUtils.parseTime(rawTime - rawTime % ModClientConfig.REALTIME_PRECISION_THRESHOLD.get(), ModClientConfig.TIME_FORMAT.get()));
        parent.labels.add(new BERText(parent.getFontUtils(), line, 0)
            .withIsCentered(false)
            .withMaxWidth(4, true)
            .withStretchScale(0.08f, 0.14f)
            .withStencil(0, 7)
            .withColor((0xFF << 24) | (blockEntity.getColor()))
            .withPredefinedTextTransformation(new TextTransformation(3.0f, y + 0.3f, 0.0f, 1, 0.14f))
            .build()
        );
        line = Utils.text(pred.stationName()).withStyle(ChatFormatting.BOLD);
        parent.labels.add(new BERText(parent.getFontUtils(), line, 0)
            .withIsCentered(false)
            .withMaxWidth(maxWidth, true)
            .withStretchScale(0.2f, 0.2f)
            .withStencil(0, maxWidth)
            .withCanScroll(true, 0.5f)
            .withColor((0xFF << 24) | (blockEntity.getColor()))
            .withPredefinedTextTransformation(new TextTransformation(10.0f, y, 0.0f, 1, 0.2f))
            .build()
        );
    }

    private void updateNextConnections(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent) {
        int displayWidth = blockEntity.getXSizeScaled();

        // ### CONTENT PANEL
        if (notInService(blockEntity)) {            
            return;
        }

        float y = PANEL_Y_START;
        float maxWidth = displayWidth * 16 - 3;

        MutableComponent ln = Utils.text(generatePageIndexString());
        float rawTextWidth = Math.min(parent.getFontUtils().font.width(ln) * 0.2f, maxWidth - 16.0f);
        parent.labels.add(new BERText(parent.getFontUtils(), ln, 0)
            .withIsCentered(false)
            .withMaxWidth(maxWidth - 16.0f, true)
            .withStretchScale(0.2f, 0.2f)
            .withStencil(0, maxWidth - 16.0f)
            .withCanScroll(false, 0.5f)
            .withColor((0xFF << 24) | (blockEntity.getColor()))
            .withPredefinedTextTransformation(new TextTransformation(maxWidth - rawTextWidth - 0.25f, y - 0.2f, 0.0f, 1, 0.2f))
            .build()
        );

        parent.labels.add(new BERText(parent.getFontUtils(), textNextConnections, 0)
            .withIsCentered(false)
            .withMaxWidth(maxWidth - 6.0f - rawTextWidth, true)
            .withStretchScale(0.1f, 0.15f)
            .withStencil(0, maxWidth - 6.0f - rawTextWidth)
            .withCanScroll(true, 0.5f)
            .withColor((0xFF << 24) | (blockEntity.getColor()))
            .withPredefinedTextTransformation(new TextTransformation(3.0f, y, 0.0f, 1, 0.15f))
            .build()
        );
        y += PANEL_LINE_HEIGHT;
       
        if (this.nextConnections == null) {
            return;
        }

        for (int i = nextConnectionsPage * NEXT_CONNECTIONS_MAX_ENTRIES_PER_PAGE; i < (nextConnectionsPage + 1) * NEXT_CONNECTIONS_MAX_ENTRIES_PER_PAGE && i < nextConnections.size(); i++) {

            SimpleTrainConnection connection = nextConnections.get(i);
            int rawTime = (int)(nextConnectionsRefreshTime % 24000 + connection.ticks() + Constants.TIME_SHIFT);
            MutableComponent line = Utils.text(TimeUtils.parseTime(rawTime - rawTime % ModClientConfig.REALTIME_PRECISION_THRESHOLD.get(), ModClientConfig.TIME_FORMAT.get()));

            // Time
            parent.labels.add(new BERText(parent.getFontUtils(), line, 0)
                .withIsCentered(false)
                .withMaxWidth(4, true)
                .withStretchScale(0.08f, 0.14f)
                .withStencil(0, 4)
                .withColor((0xFF << 24) | (blockEntity.getColor()))
                .withPredefinedTextTransformation(new TextTransformation(4.0f, y + 0.1f, 0.0f, 1, 0.14f))
                .build()
            );

            // Train Name
            line = Utils.text(connection.trainName());
            parent.labels.add(new BERText(parent.getFontUtils(), line, 0)
                .withIsCentered(false)
                .withMaxWidth(5, true)
                .withStretchScale(0.1f, 0.16f)
                .withStencil(0, 5)
                .withColor((0xFF << 24) | (blockEntity.getColor()))
                .withPredefinedTextTransformation(new TextTransformation(8.5f, y, 0.0f, 1, 0.16f))
                .build()
            );

            // Platform
            line = Utils.text(connection.stationDetails().platform());
            rawTextWidth = Math.min(parent.getFontUtils().font.width(line) * 0.14f, maxWidth - 16.0f);
            parent.labels.add(new BERText(parent.getFontUtils(), line, 0)
                .withIsCentered(false)
                .withMaxWidth(maxWidth - 16.0f, true)
                .withStretchScale(0.14f, 0.16f)
                .withStencil(0, maxWidth - 16.0f)
                .withCanScroll(false, 0.5f)
                .withColor((0xFF << 24) | (blockEntity.getColor()))
                .withPredefinedTextTransformation(new TextTransformation(maxWidth - rawTextWidth, y, 0.0f, 1, 0.16f))
                .build()
            );

            // Destination
            line = Utils.text(connection.scheduleTitle());
            parent.labels.add(new BERText(parent.getFontUtils(), line, 0)
                .withIsCentered(false)
                .withMaxWidth(maxWidth - 17.0f - rawTextWidth, true)
                .withStretchScale(0.1f, 0.16f)
                .withStencil(0, maxWidth - 17.0f - rawTextWidth)
                .withCanScroll(true, 0.5f)
                .withColor((0xFF << 24) | (blockEntity.getColor()))
                .withPredefinedTextTransformation(new TextTransformation(14.0f, y, 0.0f, 1, 0.16f))
                .build()
            );
            y += PANEL_LINE_HEIGHT;
        }
    }

    private String generatePageIndexString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nextConnectionsPage; i++) {
            sb.append(" □");
        }
        sb.append(" ■");

        for (int i = nextConnectionsPage + 1; i < nextConnectionsMaxPage; i++) {
            sb.append(" □");
        }

        return sb.toString();
    }
}
