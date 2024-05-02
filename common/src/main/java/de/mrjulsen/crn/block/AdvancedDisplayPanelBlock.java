package de.mrjulsen.crn.block;

import java.util.Map;

import de.mrjulsen.mcdragonlib.data.Pair;
import de.mrjulsen.mcdragonlib.data.Tripple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AdvancedDisplayPanelBlock extends AbstractAdvancedSidedDisplayBlock {
    
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
    public boolean canConnectWithBlock(Level level, BlockPos selfPos, BlockPos otherPos) {
        return true;
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

    @Override
    public Tripple<Float, Float, Float> getRenderRotation(Level level, BlockState blockState, BlockPos pos) {
        return Tripple.of(0.0F, 0.0F, 0.0F);
    }

    @Override
    public boolean isSingleLined() {
        return false;
    }
}
