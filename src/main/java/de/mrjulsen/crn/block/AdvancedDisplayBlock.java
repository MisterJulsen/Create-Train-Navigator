package de.mrjulsen.crn.block;

import java.util.function.Predicate;

import com.simibubi.create.foundation.placement.PlacementHelpers;
import com.simibubi.create.foundation.placement.PoleHelper;
import de.mrjulsen.crn.registry.ModBlocks;
import de.mrjulsen.crn.util.Pair;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class AdvancedDisplayBlock extends AbstractAdvancedDisplayBlock {

    public AdvancedDisplayBlock(Properties properties) {
        super(properties.noOcclusion());
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
        return Pair.of(16.05f, 16.05f);
    }

    private static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());

    @Override
    protected int getPlacementHelperID() {
        return placementHelperId;
    }

	@MethodsReturnNonnullByDefault
	private static class PlacementHelper extends PoleHelper<Direction> {

		public PlacementHelper() {
			super(ModBlocks.ADVANCED_DISPLAY_BLOCK::has, state -> state.getValue(HorizontalDirectionalBlock.FACING)
				.getClockWise()
				.getAxis(), HorizontalDirectionalBlock.FACING);
		}

		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return ModBlocks.ADVANCED_DISPLAY_BLOCK::isIn;
		}

	}
    
}
