package de.mrjulsen.crn.block;

import de.mrjulsen.crn.data.ESide;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public abstract class AbstractAdvancedSidedDisplayBlock extends AbstractAdvancedDisplayBlock {

    public static final EnumProperty<ESide> SIDE = EnumProperty.create("side", ESide.class);

    public AbstractAdvancedSidedDisplayBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(this.stateDefinition.any()
            .setValue(SIDE, ESide.FRONT)
        );
    }
    
    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(SIDE);
    }

    @Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction face = context.getClickedFace();
		BlockPos clickedPos = context.getClickedPos();
		BlockPos placedOnPos = clickedPos.relative(face.getOpposite());
		Level level = context.getLevel();
		BlockState blockState = level.getBlockState(placedOnPos);
		BlockState stateForPlacement = this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());

		if ((blockState.getBlock() != this) || (context.getPlayer() != null && context.getPlayer().isShiftKeyDown())) {
			stateForPlacement = super.getStateForPlacement(context).setValue(FACING, context.getHorizontalDirection().getOpposite());
			stateForPlacement = getPropertyFromNeighbours(stateForPlacement, level, clickedPos, SIDE);
		} else { // Clicked on existing block
			Direction otherFacing = blockState.getValue(FACING);
			stateForPlacement = stateForPlacement.setValue(FACING, otherFacing).setValue(SIDE, blockState.getValue(SIDE));
		}

		return updateColumn(level, clickedPos, stateForPlacement, true);
	}
}
