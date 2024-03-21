package de.mrjulsen.crn.block.connected;

import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;

import de.mrjulsen.crn.block.AbstractAdvancedDisplayBlock;
import de.mrjulsen.crn.block.be.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.block.be.IMultiblockBlockEntity;
import de.mrjulsen.crn.client.ber.AdvancedDisplayRenderInstance;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class DefaultCTBehaviour extends ConnectedTextureBehaviour.Base {

	protected CTSpriteShiftEntry shift;

	public DefaultCTBehaviour(CTSpriteShiftEntry shift) {
		this.shift = shift;
	}

	@Override
	public CTSpriteShiftEntry getShift(BlockState state, Direction direction, TextureAtlasSprite sprite) {
		return shift;
	}

    @Override
	public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter reader, BlockPos pos, BlockPos otherPos, Direction face, Direction primaryOffset, Direction secondaryOffset) {
		if (other.getBlock() != state.getBlock()) {
			return false;
		}
		if (other.getValue(HorizontalDirectionalBlock.FACING) != state.getValue(HorizontalDirectionalBlock.FACING)) {
			return false;
		}
		if (other.getValue(AbstractAdvancedDisplayBlock.SIDE) != state.getValue(AbstractAdvancedDisplayBlock.SIDE)) {
			return false;
		}
		if (reader.getBlockEntity(pos) instanceof IMultiblockBlockEntity be1 && reader.getBlockEntity(otherPos) instanceof IMultiblockBlockEntity be2 && Math.abs(be1.getXIndex() - be2.getXIndex()) > 1) {
			return false;
		}
        if (pos.below().equals(otherPos) && !state.getValue(AbstractAdvancedDisplayBlock.DOWN)) {
			return false;
		}
            
		return true;
	}

}

