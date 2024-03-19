package de.mrjulsen.crn.block;

import java.util.function.Predicate;

import com.simibubi.create.foundation.placement.PlacementHelpers;
import com.simibubi.create.foundation.placement.PoleHelper;
import de.mrjulsen.crn.registry.ModBlocks;
import de.mrjulsen.crn.util.Pair;
import de.mrjulsen.crn.util.Tripple;
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

public class AdvancedDisplayBlock extends AbstractAdvancedDisplayBlock {
    
    private static final VoxelShape SHAPE_SN = Block.box(0, 0, 3, 16, 16, 13);
    private static final VoxelShape SHAPE_EW = Block.box(3, 0, 0, 13, 16, 16);

    public AdvancedDisplayBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return pState.getValue(FACING) == Direction.NORTH || pState.getValue(FACING) == Direction.SOUTH ? SHAPE_SN : SHAPE_EW;
    }

    @Override
    public Pair<Float, Float> getRenderAspectRatio(Level level, BlockState blockState, BlockPos pos) {
        return Pair.of(1.0F, 1.0F);
    }

    @Override
    public Tripple<Float, Float, Float> getRenderOffset(Level level, BlockState blockState, BlockPos pos) {
        return Tripple.of(0.0f, 0.0f, 13.05f);
    }

    private static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());

    @Override
    protected int getPlacementHelperID() {
        return placementHelperId;
    }

	@MethodsReturnNonnullByDefault
	private static class PlacementHelper extends PoleHelper<Direction> {

		public PlacementHelper() {
			super(ModBlocks.ADVANCED_DISPLAY::has, state -> state.getValue(HorizontalDirectionalBlock.FACING)
				.getClockWise()
				.getAxis(), HorizontalDirectionalBlock.FACING);
		}

		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return ModBlocks.ADVANCED_DISPLAY::isIn;
		}

	}
    
}
