package de.mrjulsen.crn.block.be;

import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings("unchecked")
public interface IMultiblockBlockEntity<T extends BlockEntity & IMultiblockBlockEntity<T, B>, B extends Block> {
    
    byte getIndex();
    byte getMaxWidth();
    boolean isController();
    Class<B> getBlockType();
    Class<T> getBlockEntityType();

    default T getController() {
        T be = (T)this;
		if (isController())
			return be;

		BlockState blockState = be.getBlockState();
		if (!getBlockType().isInstance(blockState.getBlock())) {
            return null;
        }

		MutableBlockPos pos = be.getBlockPos().mutable();
		Direction side = blockState.getValue(HorizontalDirectionalBlock.FACING).getClockWise();

		for (int i = 0; i < getMaxWidth(); i++) {

            T otherBlockEntity = getBlockEntityCasted(be.getLevel(), pos.relative(side)) ;
            if (otherBlockEntity == null || !connectedTo(otherBlockEntity)) {
                return be;
            }
            pos.move(side);

			if (otherBlockEntity.isController()) {
                return otherBlockEntity;
            }
		}
		return be;
	}

    default T getBlockEntityCasted(Level level, BlockPos otherpos) {
        if (!getBlockEntityType().isInstance(level.getBlockEntity(otherpos))) {
            return null;
        }
        return getBlockEntityType().cast(level.getBlockEntity(otherpos));
    }

    default boolean connectedTo(T otherBlockEntity) {
        T be = (T)this;
        return otherBlockEntity.getBlockState().is(be.getBlockState().getBlock()) &&
                otherBlockEntity.getBlockState().getValue(HorizontalDirectionalBlock.FACING) == be.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
    }

    default void applyToAll(Consumer<T> apply) {
        T be = (T)this;

		BlockState blockState = be.getBlockState();
		if (!getBlockType().isInstance(blockState.getBlock())) {
            return;
        }

		MutableBlockPos pos = be.getBlockPos().mutable();
		Direction side = blockState.getValue(HorizontalDirectionalBlock.FACING).getCounterClockWise();

		for (int i = 0; i < getMaxWidth(); i++) {
            BlockPos newPos = pos.relative(side, -getIndex());
            pos.move(side);
            T otherBlockEntity = getBlockEntityCasted(be.getLevel(), newPos);
            if (otherBlockEntity == null || !connectedTo(otherBlockEntity)) {
                continue;
            }
            apply.accept(otherBlockEntity);
		}
	}
}
