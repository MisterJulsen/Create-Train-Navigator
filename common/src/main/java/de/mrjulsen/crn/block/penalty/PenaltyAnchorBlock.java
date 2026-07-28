package de.mrjulsen.crn.block.penalty;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import de.mrjulsen.crn.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class PenaltyAnchorBlock extends Block implements IBE<PenaltyAnchorBlockEntity>, IWrenchable {

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty ENABLED = BooleanProperty.create("enabled");
        public static final BooleanProperty HAS_INPUT = BooleanProperty.create("has_input");

    public PenaltyAnchorBlock(Properties properties) {
        super(properties.isRedstoneConductor((state, level, pos) -> false));
        registerDefaultState(defaultBlockState()
                .setValue(POWERED, false)
                .setValue(ENABLED, false)
                .setValue(HAS_INPUT, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder.add(POWERED, ENABLED, HAS_INPUT));
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
        return blockState.getValue(POWERED) && side.getAxis().isHorizontal() ? 15 : 0;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return getBlockEntityOptional(level, pos).map(PenaltyAnchorBlockEntity::getComparatorOutput).orElse(0);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean hasInputSignal = Math.max(context.getLevel().getSignal(context.getClickedPos().above(), Direction.UP), context.getLevel().getSignal(context.getClickedPos().below(), Direction.DOWN)) > 0;
        return this.defaultBlockState().setValue(HAS_INPUT, hasInputSignal);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide) {
            boolean hasInput = state.getValue(HAS_INPUT);
            boolean hasInputSignal = Math.max(level.getSignal(pos.above(), Direction.UP), level.getSignal(pos.below(), Direction.DOWN)) > 0;
            if (hasInput != hasInputSignal) {
                if (hasInput) {
                    level.scheduleTick(pos, this, 4);
                } else {
                    level.setBlock(pos, state.cycle(HAS_INPUT), 2);
                }
            }

        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean hasInputSignal = Math.max(level.getSignal(pos.above(), Direction.UP), level.getSignal(pos.below(), Direction.DOWN)) > 0;
        if (state.getValue(HAS_INPUT) && !hasInputSignal) {
            level.setBlock(pos, state.cycle(HAS_INPUT), 2);
        }

    }

    @Override
    public Class<PenaltyAnchorBlockEntity> getBlockEntityClass() {
        return PenaltyAnchorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PenaltyAnchorBlockEntity> getBlockEntityType() {
        return ModBlockEntities.PENALTY_ANCHOR_BLOCK_ENTITY.get();
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        IBE.onRemove(state, level, pos, newState);
    }

}
