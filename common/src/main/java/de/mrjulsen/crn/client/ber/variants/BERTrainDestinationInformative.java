package de.mrjulsen.crn.client.ber.variants;

import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity.EUpdateReason;
import de.mrjulsen.crn.client.ber.AdvancedDisplayRenderInstance;
import de.mrjulsen.crn.client.ber.IBERRenderSubtype;
import de.mrjulsen.mcdragonlib.client.ber.BERGraphics;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel.BoundsHitReaction;
import de.mrjulsen.mcdragonlib.client.util.BERUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class BERTrainDestinationInformative implements IBERRenderSubtype<AdvancedDisplayBlockEntity, AdvancedDisplayRenderInstance, Boolean> {

    private static final ResourceLocation CARRIAGE_ICON = new ResourceLocation("create:textures/gui/assemble.png");
    private static final ResourceLocation ICONS = new ResourceLocation(CreateRailwaysNavigator.MOD_ID, "textures/gui/icons.png");  


    private final BERLabel carriageIndexLabel = new BERLabel()
        .setScale(0.25f, 0.25f)
        .setYScale(0.25f)
    ;
    private final BERLabel trainLineLabel = new BERLabel()
        .setScale(0.25f, 0.15f)
        .setYScale(0.25f)
    ;
    private final BERLabel fromLabel = new BERLabel()
        .setScale(0.25f, 0.15f)
        .setYScale(0.25f)
        .setScrollingSpeed(2)
    ;
    private final BERLabel stopoversLabel = new BERLabel()
        .setScale(0.2f, 0.15f)
        .setYScale(0.2f)
        .setScrollingSpeed(2)
    ;
    private final BERLabel destinationLabel = new BERLabel()
        .setScale(0.25f, 0.15f)
        .setYScale(0.25f)
        .setScrollingSpeed(2)
    ;


    @Override
    public void renderTick(float deltaTime) {
        carriageIndexLabel.renderTick();
        trainLineLabel.renderTick();
        fromLabel.renderTick();
        stopoversLabel.renderTick();
        destinationLabel.renderTick();
    }

    @Override
    public void render(BERGraphics<AdvancedDisplayBlockEntity> graphics, float partialTick, AdvancedDisplayRenderInstance parent, int light, boolean backSide) {        
        float uv = 1.0f / 256.0f;
        BERUtils.fillColor(graphics, 2.5f, 5.0f, 0.0f, graphics.blockEntity().getXSizeScaled() * 16 - 5, 0.25f, (0xFF << 24) | (graphics.blockEntity().getColor() & 0x00FFFFFF), graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING), light);
        BERUtils.renderTexture(CARRIAGE_ICON, graphics, false, graphics.blockEntity().getXSizeScaled() * 16 - 7 - carriageIndexLabel.getTextWidth(), 2.5f, 0, 3, 2, uv * 22, uv * 231, uv * 22 + uv * 13, uv * 231 + uv * 5, graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING).getOpposite(), (0xFF << 24) | (graphics.blockEntity().getColor() & 0x00FFFFFF), light);
        carriageIndexLabel.render(graphics, light);

        if (graphics.blockEntity().getTrainData() == null || graphics.blockEntity().getTrainData().isEmpty()) {
            return;
        }

        trainLineLabel.render(graphics, light);
        fromLabel.render(graphics, light);
        stopoversLabel.render(graphics, light);
        destinationLabel.render(graphics, light);

        BERUtils.renderTexture(
            ICONS,
            graphics,
            false,
            3,
            6,
            0.0f,
            2,
            2,
            uv * 195,
            uv * 19,
            uv * (195 + 10),
            uv * (19 + 10),
            graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING),
            (0xFF << 24) | (graphics.blockEntity().getColor() & 0x00FFFFFF),
            light
        );
        
        BERUtils.renderTexture(
            ICONS,
            graphics,
            false,
            3,
            11,
            0.0f,
            2,
            2,
            uv * 211,
            uv * 19,
            uv * (211 + 10),
            uv * (19 + 10),
            graphics.blockEntity().getBlockState().getValue(HorizontalDirectionalBlock.FACING),
            (0xFF << 24) | (graphics.blockEntity().getColor() & 0x00FFFFFF),
            light
        );
    }

    @Override
    public void update(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent, EUpdateReason reason) {
        if (blockEntity.getTrainData() == null || blockEntity.getTrainData().isEmpty()) {
            return;
        }
        updateContent(blockEntity);
    }

    private void updateContent(AdvancedDisplayBlockEntity blockEntity) {
        carriageIndexLabel
            .setText(TextUtils.text(String.format("%02d", blockEntity.getCarriageData().index() + 1)).withStyle(ChatFormatting.BOLD))
            .setPos(blockEntity.getXSizeScaled() * 16 - 3 - carriageIndexLabel.getTextWidth(), 2.5f)
            .setColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
        ;
        trainLineLabel
            .setPos(3, 2.5f)
            .setMaxWidth(blockEntity.getXSizeScaled() * 16 - 6 - carriageIndexLabel.getTextWidth() - 5, BoundsHitReaction.SCALE_SCROLL)
            .setText(TextUtils.text(blockEntity.getTrainData().getTrainData().getName()).withStyle(ChatFormatting.BOLD))
            .setColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
        ;
        fromLabel
            .setPos(6, 6)
            .setMaxWidth(blockEntity.getXSizeScaled() * 16 - 9, BoundsHitReaction.SCALE_SCROLL)
            .setText(TextUtils.text(!blockEntity.getTrainData().getAllStops().isEmpty() ? blockEntity.getTrainData().getAllStops().get(0).getName() : ""))
            .setColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
        ;
        stopoversLabel
            .setPos(6, 8.75f)
            .setMaxWidth(blockEntity.getXSizeScaled() * 16 - 9, BoundsHitReaction.SCALE_SCROLL)
            .setText(TextUtils.concat(TextUtils.text(" \u25CF "), blockEntity.getTrainData().getStopovers().stream().map(x -> (Component)TextUtils.text(x.getName())).toList()))
            .setColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
        ;
        destinationLabel
            .setPos(6, 11)
            .setMaxWidth(blockEntity.getXSizeScaled() * 16 - 9, BoundsHitReaction.SCALE_SCROLL)
            .setText(TextUtils.text(blockEntity.getTrainData().getNextStop().isPresent() ? blockEntity.getTrainData().getNextStop().get().getDestination() : "").withStyle(ChatFormatting.BOLD))
            .setColor((0xFF << 24) | (blockEntity.getColor() & 0x00FFFFFF))
        ;
    }
}
