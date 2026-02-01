package de.mrjulsen.crn.client.ber.variants;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity.EUpdateReason;
import de.mrjulsen.crn.block.display.properties.TrainDestinationExtendedSettings;
import de.mrjulsen.crn.client.ber.AdvancedDisplayRenderInstance;
import de.mrjulsen.crn.client.lang.CustomLanguage;
import de.mrjulsen.crn.data.train.ETrainStopState;
import de.mrjulsen.mcdragonlib.client.ber.BERGraphics;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel.EScrollMode;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.Pair;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Point;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BERTrainDestinationDetailed implements AbstractAdvancedDisplayRenderer<TrainDestinationExtendedSettings> {

    private final Component TEXT_OUT_OF_SERVICE = CustomLanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.not_in_service");
    private final Component TEXT_DO_NOT_BOARD = CustomLanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.do_not_board");

    private final BERLabel outOfServiceLabel = new BERLabel();
    private final BERLabel trainLineLabel = new BERLabel();
    private final BERLabel destinationLabel = new BERLabel();
    private final BERLabel viaLabel = new BERLabel();
    private final BERLabel stopoversLabel = new BERLabel();


    public BERTrainDestinationDetailed() {
        outOfServiceLabel.text.set(TEXT_OUT_OF_SERVICE);
        outOfServiceLabel.position.set(Point.of(3, 6));
        outOfServiceLabel.horizontalScale.set(Pair.of(0.25f, 0.5f));
        outOfServiceLabel.verticalScale.set(Pair.of(0.5f, 0.5f));
        outOfServiceLabel.horizontalAlign.set(ETextAlignment.CENTER);
        outOfServiceLabel.horizontalScrollingSpeed.set(SCROLLING_SPEED);
        outOfServiceLabel.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);

        trainLineLabel.horizontalScale.set(Pair.of(0.3f, 0.5f));
        trainLineLabel.verticalScale.set(Pair.of(0.5f, 0.5f));
        trainLineLabel.preferredWidth.set(12f);
        trainLineLabel.horizontalScrollMode.set(EScrollMode.FLEX_FIT);

        destinationLabel.horizontalScale.set(Pair.of(0.25f, 0.5f));
        destinationLabel.verticalScale.set(Pair.of(0.5f, 0.5f));
        destinationLabel.horizontalAlign.set(ETextAlignment.CENTER);
        destinationLabel.horizontalScrollingSpeed.set(SCROLLING_SPEED);
        trainLineLabel.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);

        viaLabel.text.set(CustomLanguage.translate("gui." + CreateRailwaysNavigator.MOD_ID + ".via").withStyle(ChatFormatting.ITALIC));
        viaLabel.horizontalScale.set(Pair.of(0.35f, 0.35f));
        viaLabel.verticalScale.set(Pair.of(0.35f, 0.35f));
        
        stopoversLabel.horizontalScale.set(Pair.of(0.35f, 0.35f));
        stopoversLabel.verticalScale.set(Pair.of(0.35f, 0.35f));
        stopoversLabel.horizontalScrollingSpeed.set(SCROLLING_SPEED);
        stopoversLabel.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
    }
    

    @Override
    public void render(BERGraphics<AdvancedDisplayBlockEntity> graphics, float partialTick, AdvancedDisplayRenderInstance parent, int light, boolean backSide) {
        if (graphics.blockEntity().getTrainData() == null || graphics.blockEntity().getTrainData().getState().isIrregular(getDisplaySettings(graphics.blockEntity()).showDoNotBoardText())) {
            outOfServiceLabel.render(graphics);
            return;
        }
        trainLineLabel.render(graphics);
        destinationLabel.render(graphics);
        viaLabel.render(graphics);
        stopoversLabel.render(graphics);
    }

    @Override
    public void update(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent, EUpdateReason reason) {
        if (blockEntity.getTrainData() == null || blockEntity.getTrainData().getState().isIrregular(getDisplaySettings(blockEntity).showDoNotBoardText())) {
            outOfServiceLabel.clippingArea.set(Rectangle.withSize(3, 3, blockEntity.getXSizeScaled() * 16 - 6, blockEntity.getYSizeScaled() * 16 - 6));
            outOfServiceLabel.preferredWidth.set((float)outOfServiceLabel.clippingArea.get().width());
            outOfServiceLabel.color.set(getDisplaySettings(blockEntity).getFontColor());
            outOfServiceLabel.text.set((blockEntity.getTrainData() != null && blockEntity.getTrainData().getState().shouldNotBoard(getDisplaySettings(blockEntity).showDoNotBoardText())) ? TEXT_DO_NOT_BOARD : TEXT_OUT_OF_SERVICE);
            return;
        }
        trainLineLabel.clippingArea.set(Rectangle.withSize(2, 2, blockEntity.getXSizeScaled() * 16 - 4, blockEntity.getYSizeScaled() * 16 - 4));
        destinationLabel.clippingArea.set(Rectangle.withSize(2, 2, blockEntity.getXSizeScaled() * 16 - 4, blockEntity.getYSizeScaled() * 16 - 4));
        viaLabel.clippingArea.set(Rectangle.withSize(2, 2, blockEntity.getXSizeScaled() * 16 - 4, blockEntity.getYSizeScaled() * 16 - 4));
        stopoversLabel.clippingArea.set(Rectangle.withSize(2, 2, blockEntity.getXSizeScaled() * 16 - 4, blockEntity.getYSizeScaled() * 16 - 4));
        updateContent(blockEntity);
    }

    private void updateContent(AdvancedDisplayBlockEntity blockEntity) {
        TrainDestinationExtendedSettings settings = getDisplaySettings(blockEntity);
        ETrainStopState stopState = ETrainStopState.beforeArrival(!blockEntity.getTrainData().isWaitingAtStation());
        int width = settings.getTrainNameWidth();

        trainLineLabel.position.set(Point.of(3, 4));
        trainLineLabel.text.set(width == 0 ? TextUtils.empty() : TextUtils.text(blockEntity.getTrainData().getTrainData().getName(stopState)).withStyle(ChatFormatting.BOLD));
        trainLineLabel.preferredWidth.set((float)(
            settings.isFullTrainNameWidth() ?
                blockEntity.getXSizeScaled() * 16 - 6 :
                (settings.isAutoTrainNameWidth() ?
                    12 :
                    Math.min(
                        getDisplaySettings(blockEntity).getTrainNameWidth(),
                        blockEntity.getXSizeScaled() * 16 - 6)                
            )
        ));
        trainLineLabel.horizontalScrollMode.set(settings.isAutoTrainNameWidth() ? EScrollMode.FLEX_FIT : EScrollMode.WHEN_NEEDED);
        trainLineLabel.horizontalAlign.set(settings.isFullTrainNameWidth() ? ETextAlignment.CENTER : ETextAlignment.LEFT);
        
        if (settings.showLineColor() && blockEntity.getTrainData().getTrainData().hasColor(stopState)) {
            trainLineLabel.backgroundColor.set(blockEntity.getTrainData().getTrainData().getColor(stopState));
            trainLineLabel.color.set(DLColor.pickBasedOnBrightness(blockEntity.getTrainData().getTrainData().getColor(stopState), LIGHT_FONT_COLOR, DARK_FONT_COLOR, 0.5f));
        } else {
            trainLineLabel.backgroundColor.set(DLColor.TRANSPARENT);
            trainLineLabel.color.set(settings.getFontColor());
        }

        destinationLabel.position.set(Point.of((settings.isAutoTrainNameWidth() ? trainLineLabel.getRenderedWidth() : width) + 5, 4));
        destinationLabel.preferredWidth.set(blockEntity.getXSizeScaled() * 16 - destinationLabel.x.get() - 3);
        destinationLabel.text.set(settings.isFullTrainNameWidth() ? TextUtils.empty() : TextUtils.text(blockEntity.getTrainData().getCurrentStop().isPresent() ? blockEntity.getTrainData().getCurrentStop().get().getDestination() : ""));
        destinationLabel.color.set(getDisplaySettings(blockEntity).getFontColor());
        destinationLabel.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);

        viaLabel.position.set(Point.of(3, 10));
        viaLabel.color.set(getDisplaySettings(blockEntity).getFontColor());
        
        stopoversLabel.position.set(Point.of(viaLabel.getRenderedWidth() + 5, 10));
        stopoversLabel.preferredWidth.set(blockEntity.getXSizeScaled() * 16 - stopoversLabel.x.get() - 3);
        stopoversLabel.text.set(TextUtils.concat(TextUtils.text(" \u25CF "), blockEntity.getTrainData().getStopovers().stream().map(x -> (Component)TextUtils.text(x.getRealTimeStation().tagName())).toList()));
        stopoversLabel.color.set(getDisplaySettings(blockEntity).getFontColor());
    }
}
