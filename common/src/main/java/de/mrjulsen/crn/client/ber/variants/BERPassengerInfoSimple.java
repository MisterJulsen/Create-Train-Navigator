package de.mrjulsen.crn.client.ber.variants;

import de.mrjulsen.crn.data.train.ETrainStopState;
import org.joml.Vector3f;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity.EUpdateReason;
import de.mrjulsen.crn.block.display.properties.PassengerInformationScrollingTextSettings;
import de.mrjulsen.crn.client.ber.AdvancedDisplayRenderInstance;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.lang.CustomLanguage;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.data.TrainExitSide;
import de.mrjulsen.crn.data.train.portable.TrainDisplayData.State;
import de.mrjulsen.crn.util.ModUtils;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.ber.BERGraphics;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel.EScrollMode;
import de.mrjulsen.mcdragonlib.client.util.RenderUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.Pair;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Point;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.mcdragonlib.util.time.ConfiguredTimeSystem;
import de.mrjulsen.mcdragonlib.util.time.DLTime;
import de.mrjulsen.mcdragonlib.util.time.TimeContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class BERPassengerInfoSimple implements AbstractAdvancedDisplayRenderer<PassengerInformationScrollingTextSettings> {

    private static final ResourceLocation ICONS = new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "textures/gui/icons.png");  
    private final MutableComponent textTrainTerminatesHere = CustomLanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.this_train_terminates_there")
        .append(" ")
        .append(CustomLanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.passengers_leave_train"));
    private final MutableComponent textTrainTerminated = CustomLanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.train_terminates");
    private static final String keyNextStop = "gui.createrailwaysnavigator.route_overview.next_stop";
    private static final String keyDate = "gui.createrailwaysnavigator.route_overview.date";

    private static final int TICKS_PER_SLIDE = 100;

    private TrainExitSide exitSide = TrainExitSide.UNKNOWN;
    private final BERLabel label = new BERLabel();

    public BERPassengerInfoSimple() {
        label.position.set(Point.of(3, 5.5f));
        label.horizontalScale.set(Pair.of(0.75f, 0.75f));
        label.verticalScale.set(Pair.of(0.75f, 0.75f));
        label.horizontalAlign.set(ETextAlignment.CENTER);
        label.horizontalScrollingSpeed.set(SCROLLING_SPEED);
    }


    @Override
    public void render(BERGraphics<AdvancedDisplayBlockEntity> graphics, float partialTick, AdvancedDisplayRenderInstance parent, int light, boolean backSide) {
        if (graphics.blockEntity().getTrainData() == null || graphics.blockEntity().getTrainData().getState().isOutOfService()) {
            return;
        }

        float uv = 1.0f / 256.0f;
        TrainExitSide side = exitSide;
        if (backSide) {
            side = side.getOpposite();
        }
        switch (side) {
            case RIGHT:
                RenderUtils.renderTexture(
                    ICONS,
                    graphics,
                    new Vector3f(graphics.blockEntity().getXSizeScaled() * 16 - 3 - 8, 4, 0),
                    8, 8,
                    uv * ModGuiIcons.ARROW_RIGHT.getU(), uv * ModGuiIcons.ARROW_RIGHT.getV(),
                    uv * ModGuiIcons.ICON_SIZE, uv * ModGuiIcons.ICON_SIZE,
                    graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING).getOpposite(),
                    getDisplaySettings(graphics.blockEntity()).getFontColor(),
                    false
                );
                break;
            case LEFT:
                RenderUtils.renderTexture(
                    ICONS,
                    graphics,
                    new Vector3f(3, 4, 0),
                    8, 8,
                    uv * ModGuiIcons.ARROW_LEFT.getU(), uv * ModGuiIcons.ARROW_LEFT.getV(),
                    uv * ModGuiIcons.ICON_SIZE, uv * ModGuiIcons.ICON_SIZE,
                    graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING).getOpposite(),
                    getDisplaySettings(graphics.blockEntity()).getFontColor(),
                    false
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
        if (blockEntity.getTrainData() == null || blockEntity.getTrainData().getState().isOutOfService()) {
            return;
        }
        
        label.clippingArea.set(Rectangle.withSize(3, 3, blockEntity.getXSizeScaled() * 16 - 6, blockEntity.getYSizeScaled() * 16 - 6));        


        PassengerInformationScrollingTextSettings settings = getDisplaySettings(blockEntity);
        this.exitSide = settings.showExit() ? (blockEntity.getTrainData().isWaitingAtStation() ? exitSide : blockEntity.relativeExitDirection.get()) : TrainExitSide.UNKNOWN;
        ETrainStopState stopState = ETrainStopState.beforeArrival(!blockEntity.getTrainData().isWaitingAtStation());

        
        if (blockEntity.getTrainData().getState() == State.AT_TERMINUS) {
            label.text.set(textTrainTerminated);
        } else if (!blockEntity.getTrainData().getNextStop().isPresent()) {
            label.text.set(settings.getTrainTextComponents().showTrainName() ? TextUtils.text(blockEntity.getTrainData().getTrainData().getName(stopState)) : TextUtils.empty());
        } else if (blockEntity.getTrainData().isWaitingAtStation()) {
            label.text.set(TextUtils.text(blockEntity.getTrainData().getNextStop().get().getRealTimeStation().tagName()));
        } else if (blockEntity.getTrainData().getNextStop().get().getRealTimeArrivalTime() - DragonLib.getCurrentWorldTime() < ModClientConfig.NEXT_STOP_ANNOUNCEMENT.get()) {
            MutableComponent txt = CustomLanguage.translate(keyNextStop, blockEntity.getTrainData().getNextStop().get().getRealTimeStation().tagName());
            if (blockEntity.getTrainData().getState().isTerminating(getDisplaySettings(blockEntity).showDoNotBoardText())) {
                txt = TextUtils.concatSimple(txt, textTrainTerminatesHere);
            }
            label.text.set(txt);
        } else {
            final int slides = 3;
            int slide = (int)(DragonLib.getCurrentWorldTime() % (TICKS_PER_SLIDE * slides)) / TICKS_PER_SLIDE;
            
            if ((slide == 1 && !settings.showTimeAndDate())) {
                slide++;
            }
            if ((slide == 2 && !settings.showStats())) {
                slide++;
            }
            
            slide %= slides;
            switch (slide) {
                case 0 -> label.text.set(TextUtils.text((settings.getTrainTextComponents().showTrainName()
                        ? blockEntity.getTrainData().getTrainData().getName(stopState) + " "
                        : "")
                        + ((settings.getTrainTextComponents().showDestination() && blockEntity.getTrainData().getCurrentStop().isPresent())
                            ? (blockEntity.getTrainData().getCurrentStop().get().getDestination())//blockEntity.getTrainData().isWaitingAtStation() ? blockEntity.getTrainData().getNextStop().get().getDestination() : blockEntity.getTrainData().getFinalStop().get().getDestination())
                            : "")));
                case 1 -> label.text.set(
                            CustomLanguage.translate(keyDate, blockEntity.getLevel().getDayTime() / Level.TICKS_PER_DAY,
                                DLTime.fromLevelTime(level, new ConfiguredTimeSystem()).format(ModClientConfig.TIME_FORMAT.get().getFormat(), TimeContext.INGAME)));
                case 2 -> label.text.set(ModUtils.calcSpeedString(blockEntity.getTrainData().getSpeed(),
                        ModClientConfig.SPEED_UNIT.get()));
            }            
            this.exitSide = TrainExitSide.UNKNOWN;
        }

        label.preferredWidth.set((float)blockEntity.getXSizeScaled() * 16 - 6 - (exitSide == TrainExitSide.UNKNOWN ? 0 : 10));
        label.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
        label.color.set(getDisplaySettings(blockEntity).getFontColor());
    }
}
