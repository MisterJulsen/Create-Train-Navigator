package de.mrjulsen.crn.block;

import de.mrjulsen.crn.block.be.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.data.ESide;
import de.mrjulsen.mcdragonlib.client.ber.IBlockEntityRendererInstance.EUpdateReason;
import net.minecraft.core.BlockPos;
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
    protected boolean updateNeighbour(BlockState pState, Level pLevel, BlockPos pPos, BlockPos neighbourPos) {
        if (pLevel.getBlockState(neighbourPos).is(this) && pLevel.getBlockEntity(neighbourPos) instanceof AdvancedDisplayBlockEntity otherBe && pLevel.getBlockEntity(pPos) instanceof AdvancedDisplayBlockEntity be) {
            be.setColor(otherBe.getColor());
            be.setGlowing(otherBe.isGlowing());
            be.setDisplayType(otherBe.getDisplayType());
            be.setInfoType(otherBe.getInfoType());
			be.notifyUpdate();
            if (pLevel.isClientSide) {
                be.getController().getRenderer().update(pLevel, neighbourPos, pState, otherBe, EUpdateReason.BLOCK_CHANGED);
            }

			pLevel.setBlockAndUpdate(pPos, pState.setValue(SIDE, pLevel.getBlockState(neighbourPos).getValue(SIDE)));
            return true;
        }
		return false;
    }
}
