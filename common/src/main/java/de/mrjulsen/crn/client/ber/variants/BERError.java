package de.mrjulsen.crn.client.ber.variants;

import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity.EUpdateReason;
import de.mrjulsen.crn.block.display.properties.BasicDisplaySettings;
import de.mrjulsen.crn.client.ber.AdvancedDisplayRenderInstance;
import de.mrjulsen.mcdragonlib.client.ber.BERGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BERError implements AbstractAdvancedDisplayRenderer<BasicDisplaySettings> {

    @Override
    public void renderTick(float deltaTime) {
    }

    @Override
    public void render(BERGraphics<AdvancedDisplayBlockEntity> graphics, float partialTick, AdvancedDisplayRenderInstance parent, int light, boolean backSide) {
    }

    @Override
    public void update(Level level, BlockPos pos, BlockState state, AdvancedDisplayBlockEntity blockEntity, AdvancedDisplayRenderInstance parent, EUpdateReason reason) {  
    }
}
