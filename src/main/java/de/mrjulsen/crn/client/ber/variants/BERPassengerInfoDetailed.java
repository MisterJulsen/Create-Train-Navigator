package de.mrjulsen.crn.client.ber.variants;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import de.mrjulsen.crn.block.be.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.client.ber.AdvancedDisplayRenderInstance;
import de.mrjulsen.crn.client.ber.base.BERText;
import de.mrjulsen.crn.client.ber.base.BERText.TextTransformation;
import de.mrjulsen.crn.client.ber.base.IBlockEntityRendererInstance.BlockEntityRendererContext;
import de.mrjulsen.crn.client.ber.base.IBlockEntityRendererInstance.EUpdateReason;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.GlobalSettingsManager;
import de.mrjulsen.crn.data.DeparturePrediction.TrainExitSide;
import de.mrjulsen.crn.event.listeners.JourneyListener.State;
import de.mrjulsen.mcdragonlib.utils.TimeUtils;
import de.mrjulsen.mcdragonlib.utils.Utils;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class BERPassengerInfoDetailed implements IBERRenderSubtype<AdvancedDisplayBlockEntity, AdvancedDisplayRenderInstance, Boolean> {

    private State state = State.WHILE_TRAVELING;

    private static final String keyNextStop = "gui.createrailwaysnavigator.route_overview.next_stop";
    private static final String keyDate = "gui.createrailwaysnavigator.route_overview.date";

    @Override
    public boolean isSingleLined() {
        return true;
    }

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
                dirty = true;
            } else if (this.state != State.WHILE_TRAVELING && pBlockEntity.getTrainData().getNextStop().get().departureTicks() > ModClientConfig.NEXT_STOP_ANNOUNCEMENT.get()) {
                this.state = State.WHILE_TRAVELING;
                dirty = true;
            }
        }

        if (dirty) {
            update(level, pos, state, pBlockEntity, parent, EUpdateReason.DATA_CHANGED);
        }
    }

    @Override
    public void update(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent, EUpdateReason reason) {
        if (blockEntity.getTrainData() == null) {
            return;
        }

        parent.labels.clear();

        switch (this.state) {
            case BEFORE_NEXT_STOP:
                updateAnnounceNextStop(level, pos, state, blockEntity, parent);
                break;
            case WHILE_NEXT_STOP:
                updateWhileNextStop(level, pos, state, blockEntity, parent);
                break;
            default:
                updateDefault(level, pos, state, blockEntity, parent);
                break;
        }
    }

    @Override
    public void renderAdditional(BlockEntityRendererContext context, AdvancedDisplayBlockEntity pBlockEntity, AdvancedDisplayRenderInstance parent, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pOverlay, Boolean backSide) {
        if (state == State.WHILE_NEXT_STOP || state == State.BEFORE_NEXT_STOP) {
            context.renderUtils().initRenderEngine();
            TrainExitSide side = pBlockEntity.relativeExitDirection.get();
            float uv = 1.0f / 256.0f;

            if (backSide) {
                side = side.getOpposite();
            }

            switch (side) {
                case RIGHT:
                    context.renderUtils().renderTexture(
                        ModGuiIcons.ICON_LOCATION,
                        pBufferSource,
                        pBlockEntity,
                        pPoseStack,
                        pBlockEntity.getXSizeScaled() * 16 - 3 - 8,
                        4,
                        0,
                        8,
                        8,
                        uv * ModGuiIcons.ARROW_RIGHT.getU(),
                        uv * ModGuiIcons.ARROW_RIGHT.getV(),
                        uv * (ModGuiIcons.ARROW_RIGHT.getU() + ModGuiIcons.ICON_SIZE),
                        uv * (ModGuiIcons.ARROW_RIGHT.getV() + ModGuiIcons.ICON_SIZE),
                        pBlockEntity.getBlockState().getValue(HorizontalDirectionalBlock.FACING),
                        (0xFF << 24) | (pBlockEntity.getColor()),
                        LightTexture.FULL_BRIGHT
                    );
                    break;
                case LEFT:
                    context.renderUtils().renderTexture(
                        ModGuiIcons.ICON_LOCATION,
                        pBufferSource,
                        pBlockEntity,
                        pPoseStack,
                        3f,
                        4,
                        0,
                        8,
                        8,
                        uv * ModGuiIcons.ARROW_LEFT.getU(),
                        uv * ModGuiIcons.ARROW_LEFT.getV(),
                        uv * (ModGuiIcons.ARROW_LEFT.getU() + ModGuiIcons.ICON_SIZE),
                        uv * (ModGuiIcons.ARROW_LEFT.getV() + ModGuiIcons.ICON_SIZE),
                        pBlockEntity.getBlockState().getValue(HorizontalDirectionalBlock.FACING),
                        (0xFF << 24) | (pBlockEntity.getColor()),
                        LightTexture.FULL_BRIGHT
                    );
                    break;
                default:
                    break;
            }
        }
    }

    private void updateDefault(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent) {
        int displayWidth = blockEntity.getXSizeScaled();
        boolean isSingleBlock = blockEntity.getXSizeScaled() <= 1;

        float maxWidth = displayWidth * 16 - 6;        
        parent.labels.add(new BERText(parent.getFontUtils(), () -> List.of(
            Utils.text(blockEntity.getTrainData().trainName()).append(" ").append(Utils.text(blockEntity.getTrainData().getNextStop().get().scheduleTitle())),
            Utils.text("" + (int)Math.abs(Math.round(blockEntity.getTrainData().speed() * 20 * 3.6F))).append(" km/h"),
            isSingleBlock ?
                    Utils.text(TimeUtils.parseTime((int)(blockEntity.getLevel().dayTime() % 24000), ModClientConfig.TIME_FORMAT.get())) :
                    Utils.translate(keyDate, blockEntity.getLevel().getDayTime() / 24000, TimeUtils.parseTime((int)(blockEntity.getLevel().dayTime() % 24000), ModClientConfig.TIME_FORMAT.get()))
        ), 0)
            .withIsCentered(true)
            .withMaxWidth(maxWidth, true)
            .withStretchScale(0.75f, 0.75f)
            .withStencil(0, maxWidth)
            .withCanScroll(true, 1)
            .withColor((0xFF << 24) | (blockEntity.getColor()))
            .withPredefinedTextTransformation(new TextTransformation(3, 5.5f, 0.0f, 1, 0.75f))
            .build()
        );
    }
    
    private void updateAnnounceNextStop(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent) {
        int displayWidth = blockEntity.getXSizeScaled();
        TrainExitSide side = blockEntity.relativeExitDirection.get();

        MutableComponent line = Utils.translate(keyNextStop, GlobalSettingsManager.getInstance().getSettingsData().getAliasFor(blockEntity.getTrainData().getNextStop().get().stationName()).getAliasName().get());
        float maxWidth = displayWidth * 16 - 6 - (side != TrainExitSide.UNKNOWN ? 10 : 0);        
        parent.labels.add(new BERText(parent.getFontUtils(), line, 0)
            .withIsCentered(true)
            .withMaxWidth(maxWidth, true)
            .withStretchScale(0.75f, 0.75f)
            .withStencil(0, maxWidth)
            .withCanScroll(true, 1)
            .withColor((0xFF << 24) | (blockEntity.getColor()))
            .withPredefinedTextTransformation(new TextTransformation(3 + (side == TrainExitSide.LEFT ? 10 : 0), 5.5f, 0.0f, 1, 0.75f))
            .build()
        );
    }

    private void updateWhileNextStop(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent) {
        int displayWidth = blockEntity.getXSizeScaled();

        TrainExitSide side = blockEntity.relativeExitDirection.get();
        MutableComponent line = Utils.text(GlobalSettingsManager.getInstance().getSettingsData().getAliasFor(blockEntity.getTrainData().getNextStop().get().stationName()).getAliasName().get());

        float maxWidth = displayWidth * 16 - 6 - (side != TrainExitSide.UNKNOWN ? 10 : 0);        
        parent.labels.add(new BERText(parent.getFontUtils(), line, 0)
            .withIsCentered(true)
            .withMaxWidth(maxWidth, true)
            .withStretchScale(0.75f, 0.75f)
            .withStencil(0, maxWidth)
            .withCanScroll(true, 1)
            .withColor((0xFF << 24) | (blockEntity.getColor()))
            .withPredefinedTextTransformation(new TextTransformation(3 + (side == TrainExitSide.LEFT ? 10 : 0), 5.5f, 0.0f, 1, 0.75f))
            .build()
        );
    }
}
