package de.mrjulsen.crn.client.ber.variants;

import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity.EUpdateReason;
import de.mrjulsen.crn.block.display.properties.BasicDisplaySettings;
import de.mrjulsen.crn.client.ber.AdvancedDisplayRenderInstance;
import de.mrjulsen.mcdragonlib.client.ber.BERGraphics;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel;
import de.mrjulsen.mcdragonlib.client.ber.BERLabel.EScrollMode;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.Point;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BERError implements AbstractAdvancedDisplayRenderer<BasicDisplaySettings> {

    private final BERLabel label = new BERLabel();

    public BERError() {
        label.text.set(TextUtils.text("Error! Unrecognized display type!"));
        label.position.set(Point.of(3, 3));
        label.horizontalMinScale.set(0.5f);
        label.horizontalMaxScale.set(0.5f);
        label.verticalMinScale.set(0.5f);
        label.verticalMaxScale.set(0.5f);
        label.color.set(DLColor.RED);
        label.horizontalScrollMode.set(EScrollMode.WHEN_NEEDED);
    }

    @Override
    public void renderTick(float deltaTime) {
    }

    @Override
    public void render(BERGraphics<AdvancedDisplayBlockEntity> graphics, float partialTick, AdvancedDisplayRenderInstance parent, int light, boolean backSide) {
        label.render(graphics);
    }

    @Override
    public void update(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent, EUpdateReason reason) {  
        label.clippingArea.set(Rectangle.withSize(3, 3, blockEntity.getXSizeScaled() * 16 - 6, blockEntity.getYSizeScaled() * 16 - 6));        
        label.preferredWidth.set((float)blockEntity.getXSizeScaled() * 16 - 6);
        label.glowing.set(blockEntity.isGlowing());
    }
}
