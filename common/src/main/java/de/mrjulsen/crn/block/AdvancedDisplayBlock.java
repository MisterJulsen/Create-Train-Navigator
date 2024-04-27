package de.mrjulsen.crn.block;

import de.mrjulsen.mcdragonlib.data.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class AdvancedDisplayBlock extends AbstractAdvancedDisplayBlock {

    public AdvancedDisplayBlock(Properties properties) {
        super(properties.noOcclusion());
    }

    @Override
    public boolean canConnectWithBlock(Level level, BlockPos selfPos, BlockPos otherPos) {
        return true;
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
}
