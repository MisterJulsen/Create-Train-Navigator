package de.mrjulsen.crn.client.ber.variants;

import de.mrjulsen.crn.client.ber.base.AbstractBlockEntityRenderInstance;
import de.mrjulsen.crn.client.ber.base.IBlockEntityRendererInstance.EUpdateReason;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class BERRenderSubtypeBase<T extends BlockEntity, S extends AbstractBlockEntityRenderInstance<T>, U> implements IBERRenderSubtype<T, S, U>{

    @Override
    public boolean isSingleLined() {
        return false;
    }

    @Override
    public void update(Level level, BlockPos pos, BlockState state, T blockEntity, S parent, EUpdateReason reason) {}    
}
