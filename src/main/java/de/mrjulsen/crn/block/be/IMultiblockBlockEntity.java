package de.mrjulsen.crn.block.be;

import java.util.function.Consumer;

import de.mrjulsen.crn.ModMain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.BlockGetter;
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

    default T getBlockEntityCasted(Level level, BlockPos otherpos) {
        if (!getBlockEntityType().isInstance(level.getBlockEntity(otherpos))) {
            return null;
        }
        return getBlockEntityType().cast(level.getBlockEntity(otherpos));
    }

    boolean connectable(BlockGetter getter, BlockPos a, BlockPos b);

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
