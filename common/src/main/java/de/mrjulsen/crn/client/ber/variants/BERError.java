package de.mrjulsen.crn.client.ber.variants;

import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity.EUpdateReason;
import de.mrjulsen.crn.client.ber.AdvancedDisplayRenderInstance;
import de.mrjulsen.crn.client.ber.IBERRenderSubtype;
import de.mrjulsen.mcdragonlib.client.ber.BERGraphics;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel.BoundsHitReaction;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BERError implements IBERRenderSubtype<AdvancedDisplayBlockEntity, AdvancedDisplayRenderInstance, Boolean> {

    private final BERLabel label = new BERLabel()
        .setPos(3, 3)
        .setScale(0.5f, 0.5f)
        .setYScale(0.5f)
    ;

    @Override
    public void renderTick(float deltaTime) {
        label.renderTick();
    }

    @Override
    public void render(BERGraphics<AdvancedDisplayBlockEntity> graphics, float partialTick, AdvancedDisplayRenderInstance parent, int light, boolean backSide) {
        label.render(graphics);
    }

    @Override
    public void update(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent, EUpdateReason reason) {        
        label
            .setText(TextUtils.text("Error! Unrecognized display type!"))
            .setColor(0xFFFF0000)
            .setMaxWidth(blockEntity.getXSizeScaled() * 16 - 6, BoundsHitReaction.SCALE_SCROLL)
        ;
    }
}
