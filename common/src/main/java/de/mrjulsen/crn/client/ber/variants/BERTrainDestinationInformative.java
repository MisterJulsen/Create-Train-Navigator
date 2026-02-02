package de.mrjulsen.crn.client.ber.variants;

import de.mrjulsen.crn.data.train.ETrainStopState;
import org.joml.Vector3f;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity.EUpdateReason;
import de.mrjulsen.crn.block.display.properties.TrainDestinationDetailedSettings;
import de.mrjulsen.crn.client.ber.AdvancedDisplayRenderInstance;
import de.mrjulsen.crn.client.lang.CustomLanguage;
import de.mrjulsen.mcdragonlib.client.ber.BERGraphics;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel.EScrollMode;
import de.mrjulsen.mcdragonlib.client.gui.widgets.richtext.PaddingF;
import de.mrjulsen.mcdragonlib.client.util.RenderUtils;
import de.mrjulsen.mcdragonlib.data.ETextAlignment;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.Pair;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Point;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class BERTrainDestinationInformative implements AbstractAdvancedDisplayRenderer<TrainDestinationDetailedSettings> {

    private final Component TEXT_DO_NOT_BOARD = CustomLanguage.translate("block." + CreateRailwaysNavigator.MOD_ID + ".advanced_display.ber.do_not_board");
    private static final ResourceLocation CARRIAGE_ICON = new ResourceLocation("create:textures/gui/assemble.png");
    private static final ResourceLocation ICONS = new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "textures/gui/icons.png");  


    private final BERLabel carriageIndexLabel = new BERLabel();
    private final BERLabel trainLineLabel = new BERLabel();
    private final BERLabel fromLabel = new BERLabel();
    private final BERLabel stopoversLabel = new BERLabel();
    private final BERLabel destinationLabel = new BERLabel();
    
    public BERTrainDestinationInformative() {
        carriageIndexLabel.horizontalScale.set(Pair.of(0.25f, 0.25f));
        carriageIndexLabel.verticalScale.set(Pair.of(0.25f, 0.25f));
        
        trainLineLabel.horizontalScale.set(Pair.of(0.15f, 0.25f));
        trainLineLabel.verticalScale.set(Pair.of(0.25f, 0.25f));
        trainLineLabel.backgroundPadding.set(new PaddingF(0.5f, 0.5f, 0.25f, 0.5f));
        
        fromLabel.horizontalScale.set(Pair.of(0.15f, 0.25f));
        fromLabel.verticalScale.set(Pair.of(0.25f, 0.25f));
        fromLabel.horizontalScrollingSpeed.set(SCROLLING_SPEED);
        
        stopoversLabel.horizontalScale.set(Pair.of(0.15f, 0.25f));
        stopoversLabel.verticalScale.set(Pair.of(0.25f, 0.25f));
        stopoversLabel.horizontalScrollingSpeed.set(SCROLLING_SPEED);
        
        destinationLabel.horizontalScale.set(Pair.of(0.15f, 0.25f));
        destinationLabel.verticalScale.set(Pair.of(0.25f, 0.25f));
        destinationLabel.horizontalScrollingSpeed.set(SCROLLING_SPEED);
    }

    @Override
    public void render(BERGraphics<AdvancedDisplayBlockEntity> graphics, float partialTick, AdvancedDisplayRenderInstance parent, int light, boolean backSide) {        
        float uv = 1.0f / 256.0f;
        RenderUtils.fillColor(graphics, new Vector3f(2.5f, 5.0f, 0.0f), graphics.blockEntity().getXSizeScaled() * 16 - 5, 0.25f, getDisplaySettings(graphics.blockEntity()).getFontColor(), graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING));
        RenderUtils.renderTexture(
            CARRIAGE_ICON,
            graphics,
            new Vector3f(graphics.blockEntity().getXSizeScaled() * 16 - 6 - carriageIndexLabel.getRenderedWidth(), 2.5f, 0),
            3, 2,
            uv * 22, uv * 231,
            uv * 13, uv * 5,
            graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING).getOpposite(),
            getDisplaySettings(graphics.blockEntity()).getFontColor(),
            false
        );

        carriageIndexLabel.render(graphics);

        if (graphics.blockEntity().getTrainData() == null || graphics.blockEntity().getTrainData().getState().isOutOfService()) {
            return;
        }

        trainLineLabel.render(graphics, light);
        fromLabel.render(graphics, light);        
        if (graphics.blockEntity().getTrainData().getState().shouldNotBoard(getDisplaySettings(graphics.blockEntity()).showDoNotBoardText())) {
            return;
        }

        destinationLabel.render(graphics, light);
        stopoversLabel.render(graphics, light);

        RenderUtils.renderTexture(
            ICONS,
            graphics,
            new Vector3f(3, 6, 0.0f),
            2, 2,
            uv * 195, uv * 19,
            uv * (10), uv * (10),
            graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING),
            getDisplaySettings(graphics.blockEntity()).getFontColor(),
            false
        );
        
        RenderUtils.renderTexture(
            ICONS,
            graphics,
            new Vector3f(3, 11, 0.0f),
            2, 2,
            uv * 211, uv * 19,
            uv * (10), uv * (10),
            graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING),
            getDisplaySettings(graphics.blockEntity()).getFontColor(),
            false
        );
    }

    @Override
    public void update(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent, EUpdateReason reason) {        
        carriageIndexLabel.clippingArea.set(Rectangle.withSize(2, 2, blockEntity.getXSizeScaled() * 16 - 4, blockEntity.getYSizeScaled() * 16 - 4));
        trainLineLabel.clippingArea.set(Rectangle.withSize(2, 2, blockEntity.getXSizeScaled() * 16 - 4, blockEntity.getYSizeScaled() * 16 - 4));
        fromLabel.clippingArea.set(Rectangle.withSize(2, 2, blockEntity.getXSizeScaled() * 16 - 4, blockEntity.getYSizeScaled() * 16 - 4));
        stopoversLabel.clippingArea.set(Rectangle.withSize(2, 2, blockEntity.getXSizeScaled() * 16 - 4, blockEntity.getYSizeScaled() * 16 - 4));
        destinationLabel.clippingArea.set(Rectangle.withSize(2, 2, blockEntity.getXSizeScaled() * 16 - 4, blockEntity.getYSizeScaled() * 16 - 4));
        updateContent(blockEntity);
    }

    private void updateContent(AdvancedDisplayBlockEntity blockEntity) {
        TrainDestinationDetailedSettings settings = getDisplaySettings(blockEntity);
        ETrainStopState stopState = ETrainStopState.beforeArrival(!blockEntity.getTrainData().isWaitingAtStation());

        int index = (settings.shouldOverwriteCarriageIndex() ? 0 : blockEntity.getCarriageData().index() + 1) + settings.getCarriageIndex();

        carriageIndexLabel.text.set(TextUtils.text(String.format("%02d", index)).withStyle(ChatFormatting.BOLD));
        carriageIndexLabel.horizontalAlign.set(ETextAlignment.RIGHT);
        carriageIndexLabel.position.set(Point.of(blockEntity.getXSizeScaled() * 16 - 6, 2.5f));
        float carriageLabelW = carriageIndexLabel.getRenderedWidth();
        carriageIndexLabel.preferredWidth.set(carriageLabelW);
        carriageIndexLabel.color.set(getDisplaySettings(blockEntity).getFontColor());
        
        if (blockEntity.getTrainData() == null || blockEntity.getTrainData().getState().isOutOfService()) {
            return;
        }

        trainLineLabel.position.set(Point.of(3, 2.5f));
        trainLineLabel.preferredWidth.set(carriageIndexLabel.x.get() - 9);
        trainLineLabel.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
        trainLineLabel.text.set(TextUtils.text(blockEntity.getTrainData().getTrainData().getName(stopState)).withStyle(ChatFormatting.BOLD));
        
        if (settings.showLineColor() && blockEntity.getTrainData().getTrainData().hasColor(stopState)) {
            trainLineLabel.backgroundColor.set(blockEntity.getTrainData().getTrainData().getColor(stopState));
            trainLineLabel.color.set(DLColor.pickBasedOnBrightness(blockEntity.getTrainData().getTrainData().getColor(stopState), LIGHT_FONT_COLOR, DARK_FONT_COLOR, 0.5f));
        } else {
            trainLineLabel.backgroundColor.set(DLColor.TRANSPARENT);
            trainLineLabel.color.set(settings.getFontColor());
        }
        
        if (blockEntity.getTrainData().getState().shouldNotBoard(getDisplaySettings(blockEntity).showDoNotBoardText())) {    
            fromLabel.position.set(Point.of(3, 6));
            fromLabel.preferredWidth.set((float)(blockEntity.getXSizeScaled() * 16 - 9));
            fromLabel.text.set(TEXT_DO_NOT_BOARD);
            fromLabel.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
            fromLabel.color.set(getDisplaySettings(blockEntity).getFontColor());
            return;
        }
                
        fromLabel.position.set(Point.of(6, 6));
        fromLabel.preferredWidth.set((float)(blockEntity.getXSizeScaled() * 16 - 9));
        fromLabel.text.set(TextUtils.text(!blockEntity.getTrainData().getAllStops().isEmpty() ? blockEntity.getTrainData().getAllStops().get(0).getRealTimeStation().tagName() : ""));
        fromLabel.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
        fromLabel.color.set(getDisplaySettings(blockEntity).getFontColor());
        
        stopoversLabel.position.set(Point.of(6, 8.75f));
        stopoversLabel.preferredWidth.set((float)(blockEntity.getXSizeScaled() * 16 - 9));
        stopoversLabel.text.set(TextUtils.concat(TextUtils.text(" \u25CF "), blockEntity.getTrainData().getStopovers().stream().map(x -> (Component)TextUtils.text(x.getRealTimeStation().tagName())).toList()));
        stopoversLabel.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
        stopoversLabel.color.set(getDisplaySettings(blockEntity).getFontColor());
        
        destinationLabel.position.set(Point.of(6, 11.25f));
        destinationLabel.preferredWidth.set((float)(blockEntity.getXSizeScaled() * 16 - 9));
        destinationLabel.text.set(TextUtils.text(blockEntity.getTrainData().getCurrentStop().isPresent() ? blockEntity.getTrainData().getCurrentStop().get().getDestination() : "").withStyle(ChatFormatting.BOLD));
        destinationLabel.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
        destinationLabel.color.set(getDisplaySettings(blockEntity).getFontColor());
    }
}
