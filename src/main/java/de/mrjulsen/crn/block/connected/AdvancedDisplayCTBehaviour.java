package de.mrjulsen.crn.block.connected;

import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;

import de.mrjulsen.crn.block.be.AdvancedDisplayBlockEntity;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

public class AdvancedDisplayCTBehaviour extends ConnectedTextureBehaviour.Base {

	protected CTSpriteShiftEntry shift;

	public AdvancedDisplayCTBehaviour(CTSpriteShiftEntry shift) {
		this.shift = shift;
	}

	@Override
	public CTSpriteShiftEntry getShift(BlockState state, Direction direction, TextureAtlasSprite sprite) {
		return shift;
	}

    @Override
	public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter reader, BlockPos pos, BlockPos otherPos, Direction face, Direction primaryOffset, Direction secondaryOffset) {		
		return reader.getBlockEntity(pos) instanceof AdvancedDisplayBlockEntity blockEntity && blockEntity.connectable(reader, pos, otherPos);
	}

}

