package de.mrjulsen.crn.block;

import de.mrjulsen.mcdragonlib.data.Pair;
import de.mrjulsen.mcdragonlib.data.Tripple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AdvancedDisplaySlopedBlock extends AbstractAdvancedDisplayBlock {

    private static final VoxelShape SHAPE_SN = Block.box(0, 0, 3, 16, 16, 13);
    private static final VoxelShape SHAPE_EW = Block.box(3, 0, 0, 13, 16, 16);

    public AdvancedDisplaySlopedBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return pState.getValue(FACING) == Direction.NORTH || pState.getValue(FACING) == Direction.SOUTH ? SHAPE_SN : SHAPE_EW;
    }

    @Override
    public boolean canConnectWithBlock(Level level, BlockPos selfPos, BlockPos otherPos) {
        return level.getBlockState(otherPos).getBlock() instanceof AdvancedDisplaySlopedBlock;
    }

    @Override
    protected boolean canConnect(LevelAccessor level, BlockPos pos, BlockState state, BlockState other) {
        return super.canConnect(level, pos, state, other);
    }

    @Override
    public Pair<Float, Float> getRenderAspectRatio(Level level, BlockState blockState, BlockPos pos) {
        return Pair.of(1.0F, 0.5F);
    }

    @Override
    public Pair<Float, Float> getRenderOffset(Level level, BlockState blockState, BlockPos pos) {
        return Pair.of(0.0f, -3.0F);
    }

    @Override
    public Pair<Float, Float> getRenderZOffset(Level level, BlockState blockState, BlockPos pos) {
        return Pair.of(13.15f, 14.05f);
    }

    @Override
    public Tripple<Float, Float, Float> getRenderRotation(Level level, BlockState blockState, BlockPos pos) {
        return Tripple.of(-22.5F, 0.0F, 0.0F);
    }
}
