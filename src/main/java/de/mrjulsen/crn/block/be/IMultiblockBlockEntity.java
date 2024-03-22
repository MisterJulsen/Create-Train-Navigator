package de.mrjulsen.crn.block.be;

import java.util.function.Consumer;

import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.block.AbstractAdvancedDisplayBlock;
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
       
    byte getWidth();
    byte getHeight();
    byte getMaxWidth();
    byte getMaxHeight();
    boolean isController();
    Class<B> getBlockType();
    Class<T> getBlockEntityType();

    default T getController() {
        T be = (T)this;
        
        /*
		if (isController())
			return be;

		BlockState blockState = be.getBlockState();
		if (!getBlockType().isInstance(blockState.getBlock())) {
            return null;
        }

		MutableBlockPos pos = be.getBlockPos().mutable();
		Direction side = blockState.getValue(HorizontalDirectionalBlock.FACING).getClockWise();

        for (int i = 0; i < getMaxHeight(); i++) {
			BlockState other = be.getLevel().getBlockState(pos);
            if (other.getBlock() instanceof AbstractAdvancedDisplayBlock) {
				break;
			}
            pos.move(Direction.UP);
        }

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
        */

        if (isController())
			return be;

		BlockState blockState = be.getBlockState();
		if (!(blockState.getBlock() instanceof AbstractAdvancedDisplayBlock))
			return null;

		MutableBlockPos pos = be.getBlockPos().mutable();
		Direction side = blockState.getValue(AbstractAdvancedDisplayBlock.FACING).getClockWise();

		for (int i = 0; i < getMaxWidth(); i++) {
			if (be.getLevel().getBlockEntity(pos.relative(side)) instanceof AdvancedDisplayBlockEntity otherBe) {
                if (otherBe.isController()) {
                    return (T)otherBe;
                }

				pos.move(side);
				continue;
			}
        }

        for (int i = 0; i < getMaxHeight(); i++) {
            if (be.getLevel().getBlockState(pos.above()).getBlock() instanceof AbstractAdvancedDisplayBlock) {
				pos.move(Direction.UP);
				continue;
			}			

			BlockEntity found = be.getLevel().getBlockEntity(pos);
			if (found instanceof AdvancedDisplayBlockEntity flap && flap.isController())
				return (T)flap;

			break;
		}

		return null;
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

		for (int i = 0; i < getWidth() && i < getMaxWidth(); i++) {
            BlockPos newPos = pos.relative(side, i);
            for (int j = 0; j < getHeight() && j < getMaxHeight(); j++) {
                BlockPos newPos2 = newPos.relative(Direction.DOWN, j);
                T otherBlockEntity = getBlockEntityCasted(be.getLevel(), newPos2);
                if (otherBlockEntity != null) {
                    apply.accept(otherBlockEntity);
                } else {
                    ModMain.LOGGER.error(String.format("BlockEntity at %s does not exist!", newPos2));
                }
            }
		}
        
	}
}
