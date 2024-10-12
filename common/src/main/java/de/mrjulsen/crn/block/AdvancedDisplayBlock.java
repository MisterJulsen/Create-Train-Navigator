package de.mrjulsen.crn.block;

import de.mrjulsen.mcdragonlib.data.Pair;
import de.mrjulsen.mcdragonlib.data.Tripple;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class AdvancedDisplayBlock extends AbstractAdvancedSidedDisplayBlock {

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

    @Override
    public Tripple<Float, Float, Float> getRenderRotation(Level level, BlockState blockState, BlockPos pos) {
        return Tripple.of(0.0F, 0.0F, 0.0F);
    }


    @Override
    public boolean isSingleLined() {
        return false;
    }
}
