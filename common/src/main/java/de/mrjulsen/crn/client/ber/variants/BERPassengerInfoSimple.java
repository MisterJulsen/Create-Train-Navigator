package de.mrjulsen.crn.client.ber.variants;

import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity.EUpdateReason;
import de.mrjulsen.crn.block.display.AdvancedDisplaySource.ETimeDisplay;
import de.mrjulsen.crn.client.ber.AdvancedDisplayRenderInstance;
import de.mrjulsen.crn.client.ber.IBERRenderSubtype;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.lang.ELanguage;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.TrainExitSide;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.ber.BERGraphics;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel.BoundsHitReaction;
import de.mrjulsen.mcdragonlib.client.util.BERUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class BERPassengerInfoSimple implements IBERRenderSubtype<AdvancedDisplayBlockEntity, AdvancedDisplayRenderInstance, Boolean> {

    private static final String keyNextStop = "gui.createrailwaysnavigator.route_overview.next_stop";
    private static final String keyDate = "gui.createrailwaysnavigator.route_overview.date";

    private static final int TICKS_PER_SLIDE = 100;

    private TrainExitSide exitSide = TrainExitSide.UNKNOWN;
    private final BERLabel label = new BERLabel()
        .setPos(3, 5.5f)
        .setYScale(0.75f)
        .setScale(0.75f, 0.75f)
        .setCentered(true)
        .setScrollingSpeed(2)
    ;

    @Override
    public void renderTick(float deltaTime) {
        label.renderTick();
    }


    @Override
    public void render(BERGraphics<AdvancedDisplayBlockEntity> graphics, float partialTick, AdvancedDisplayRenderInstance parent, int light, boolean backSide) {
        if (graphics.blockEntity().getTrainData() == null || graphics.blockEntity().getTrainData().isEmpty()) {
            return;
        }

        float uv = 1.0f / 256.0f;
        TrainExitSide side = exitSide;
        if (backSide) {
            side = side.getOpposite();
        }
        switch (side) {
            case RIGHT:
                BERUtils.renderTexture(
                    ModGuiIcons.ICON_LOCATION,
                    graphics,
                    false,
                    graphics.blockEntity().getXSizeScaled() * 16 - 3 - 8,
                    4,
                    0,
                    8,
                    8,
                    uv * ModGuiIcons.ARROW_RIGHT.getU(),
                    uv * ModGuiIcons.ARROW_RIGHT.getV(),
                    uv * (ModGuiIcons.ARROW_RIGHT.getU() + ModGuiIcons.ICON_SIZE),
                    uv * (ModGuiIcons.ARROW_RIGHT.getV() + ModGuiIcons.ICON_SIZE),
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
                    4,
                    0,
                    8,
                    8,
                    uv * ModGuiIcons.ARROW_LEFT.getU(),
                    uv * ModGuiIcons.ARROW_LEFT.getV(),
                    uv * (ModGuiIcons.ARROW_LEFT.getU() + ModGuiIcons.ICON_SIZE),
                    uv * (ModGuiIcons.ARROW_LEFT.getV() + ModGuiIcons.ICON_SIZE),
                    graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING),
                    (0xFF << 24) | (graphics.blockEntity().getColor()),
                    light
                );
                break;
            default:
                break;
        }

        graphics.poseStack().pushPose();        
        switch (side) {
            case LEFT:
                graphics.poseStack().translate(10, 0, 0);
                break;
            default:
                break;
        }
        label.render(graphics, light);
        graphics.poseStack().popPose();
    }
    
    @Override
    public void update(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent, EUpdateReason data) {
        if (blockEntity.getTrainData() == null ||blockEntity.getTrainData().isEmpty()) {
            return;
        }

        this.exitSide = blockEntity.getTrainData().isWaitingAtStation() ? exitSide : blockEntity.relativeExitDirection.get();
        if (!blockEntity.getTrainData().getNextStop().isPresent()) {
            label.setText(TextUtils.text(blockEntity.getTrainData().getTrainData().getName()));
        } else if (blockEntity.getTrainData().isWaitingAtStation()) {
            label.setText(TextUtils.text(blockEntity.getTrainData().getNextStop().get().getName()));
        } else if (blockEntity.getTrainData().getNextStop().get().getRealTimeArrivalTime() - DragonLib.getCurrentWorldTime() < ModClientConfig.NEXT_STOP_ANNOUNCEMENT.get()) {
            label.setText(ELanguage.translate(keyNextStop, blockEntity.getTrainData().getNextStop().get().getName()));
        } else {
            final int slides = 3;
            int slide = (int)(DragonLib.getCurrentWorldTime() % (TICKS_PER_SLIDE * slides)) / TICKS_PER_SLIDE;
            switch (slide) {                
                case 0 -> label.setText(TextUtils.text(blockEntity.getTrainData().getTrainData().getName() + " " + blockEntity.getTrainData().getNextStop().get().getDestination()));
                case 1 -> label.setText(ELanguage.translate(keyDate, blockEntity.getLevel().getDayTime() / Level.TICKS_PER_DAY, ModUtils.formatTime(DragonLib.getCurrentWorldTime(), blockEntity.getTimeDisplay() == ETimeDisplay.ETA)));
                case 2 -> label.setText(ModUtils.calcSpeedString(blockEntity.getTrainData().getSpeed(), ModClientConfig.SPEED_UNIT.get()));
            }            
            this.exitSide = TrainExitSide.UNKNOWN;
        }

        label
            .setMaxWidth(blockEntity.getXSizeScaled() * 16 - 6 - (exitSide == TrainExitSide.UNKNOWN ? 0 : 10), BoundsHitReaction.SCROLL)
            .setColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
        ;
    }
}
