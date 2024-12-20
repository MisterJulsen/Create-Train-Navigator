package de.mrjulsen.crn.client.ber;

import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity.EUpdateReason;
import de.mrjulsen.mcdragonlib.client.ber.AbstractBlockEntityRenderInstance;
import de.mrjulsen.mcdragonlib.client.ber.BERGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public interface IBERRenderSubtype<T extends BlockEntity, S extends AbstractBlockEntityRenderInstance<T>, U> {
    void update(Level level, BlockPos pos, BlockState state, T blockEntity, S parent, EUpdateReason data);
    default void renderTick(float deltaTime) {}
    default void render(BERGraphics<T> graphics, float partialTick, S parent, int light, boolean backSide) {}
    default void tick(Level level, BlockPos pos, BlockState state, T pBlockEntity, S parent) {}
}
