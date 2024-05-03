package de.mrjulsen.crn.block;

import de.mrjulsen.mcdragonlib.data.Pair;
import de.mrjulsen.mcdragonlib.data.Tripple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AdvancedDisplaySlopedBlock extends AbstractAdvancedDisplayBlock {

    private static final VoxelShape SHAPE_SN = Shapes.or(
        Block.box(0, 13.85, 2, 16, 15.999999999999998, 14),
        Block.box(0, 11.725000000000001, 2.66, 16, 13.850000000000001, 13.34),
        Block.box(0, 9.6, 3.54, 16, 11.725, 12.465000000000002),
        Block.box(0, 7.475000000000001, 4.42, 16, 9.600000000000001, 11.58)
    );
    private static final VoxelShape SHAPE_EW = Shapes.or(
        Block.box(2, 13.85, 0, 14, 15.999999999999998, 16),
        Block.box(2.66, 11.725000000000001, 0, 13.34, 13.850000000000001, 16),
        Block.box(3.5349999999999984, 9.6, 0, 12.46, 11.725, 16),
        Block.box(4.42, 7.475000000000001, 0, 11.58, 9.600000000000001, 16)
    );

    public AdvancedDisplaySlopedBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return pState.getValue(FACING) == Direction.NORTH || pState.getValue(FACING) == Direction.SOUTH ? SHAPE_SN : SHAPE_EW;
    }

    @Override
    public Pair<Float, Float> getRenderAspectRatio(Level level, BlockState blockState, BlockPos pos) {
        return Pair.of(1.0F, 0.5F);
    }

    @Override
    public Pair<Float, Float> getRenderOffset(Level level, BlockState blockState, BlockPos pos) {
        return Pair.of(0.0f, 0.75F);
    }

    @Override
    public Pair<Float, Float> getRenderZOffset(Level level, BlockState blockState, BlockPos pos) {
        return Pair.of(14.0f, 14.0f);
    }

    @Override
    public Tripple<Float, Float, Float> getRenderRotation(Level level, BlockState blockState, BlockPos pos) {
        return Tripple.of(-22.5F, 0.0F, 0.0F);
    }

    @Override
    public boolean isSingleLined() {
        return true;
    }
}
