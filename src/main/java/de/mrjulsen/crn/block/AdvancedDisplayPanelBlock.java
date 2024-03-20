package de.mrjulsen.crn.block;

import java.util.Map;
import java.util.function.Predicate;

import com.simibubi.create.foundation.placement.PlacementHelpers;
import com.simibubi.create.foundation.placement.PoleHelper;
import de.mrjulsen.crn.registry.ModBlocks;
import de.mrjulsen.crn.util.Pair;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AdvancedDisplayPanelBlock extends AbstractAdvancedDisplayBlock {
    
    private static final Map<Direction, VoxelShape> SHAPES = Map.of(
        Direction.NORTH, Block.box(0, 0, 13, 16, 16, 16),
        Direction.EAST, Block.box(0, 0, 0, 3, 16, 16),
        Direction.SOUTH, Block.box(0, 0, 0, 16, 16, 3),
        Direction.WEST, Block.box(13, 0, 0, 16, 16, 16)
    );

    public AdvancedDisplayPanelBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPES.get(pState.getValue(FACING));
    }

    @Override
    public Pair<Float, Float> getRenderAspectRatio(Level level, BlockState blockState, BlockPos pos) {
        return Pair.of(1.0F, 1.0F);
    }

    @Override
    public Pair<Float, Float> getRenderOffset(Level level, BlockState blockState, BlockPos pos) {
        return Pair.of(0.0f, 0.0f);
    }

    @Override
    public Pair<Float, Float> getRenderZOffset(Level level, BlockState blockState, BlockPos pos) {
        return Pair.of(3.05f, 16.05f);
    }

    private static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());

    @Override
    protected int getPlacementHelperID() {
        return placementHelperId;
    }

	@MethodsReturnNonnullByDefault
	private static class PlacementHelper extends PoleHelper<Direction> {

		public PlacementHelper() {
			super(ModBlocks.ADVANCED_DISPLAY_PANEL::has, state -> state.getValue(HorizontalDirectionalBlock.FACING)
				.getClockWise()
				.getAxis(), HorizontalDirectionalBlock.FACING);
		}

		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return ModBlocks.ADVANCED_DISPLAY_PANEL::isIn;
		}

	}    
}
