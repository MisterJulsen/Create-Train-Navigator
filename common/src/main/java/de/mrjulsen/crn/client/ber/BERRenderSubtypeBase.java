package de.mrjulsen.crn.client.ber;

import de.mrjulsen.crn.block.blockentity.AdvancedDisplayBlockEntity.EUpdateReason;
import de.mrjulsen.mcdragonlib.client.ber.AbstractBlockEntityRenderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class BERRenderSubtypeBase<T extends BlockEntity, S extends AbstractBlockEntityRenderInstance<T>, U> implements IBERRenderSubtype<T, S, U>{

    @Override
    public void update(Level level, BlockPos pos, BlockState state, T blockEntity, S parent, EUpdateReason reason) {}    
}
