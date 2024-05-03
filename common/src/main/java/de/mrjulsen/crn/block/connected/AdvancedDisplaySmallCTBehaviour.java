package de.mrjulsen.crn.block.connected;

import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;

import de.mrjulsen.crn.block.AbstractAdvancedSidedDisplayBlock;
import de.mrjulsen.crn.block.be.AdvancedDisplayBlockEntity;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class AdvancedDisplaySmallCTBehaviour extends ConnectedTextureBehaviour.Base {
    protected CTSpriteShiftEntry topShift;
	protected CTSpriteShiftEntry layerShift;

	public AdvancedDisplaySmallCTBehaviour(CTSpriteShiftEntry layerShift) {
		this(layerShift, null);
	}

	public AdvancedDisplaySmallCTBehaviour(CTSpriteShiftEntry layerShift, CTSpriteShiftEntry topShift) {
		this.layerShift = layerShift;
		this.topShift = topShift;
	}

	@Override
	public CTSpriteShiftEntry getShift(BlockState state, Direction direction, TextureAtlasSprite sprite) {
		boolean b = false;

		if (!(state.getBlock() instanceof AbstractAdvancedSidedDisplayBlock)) {
			return layerShift;
		}

		switch (state.getValue(AbstractAdvancedSidedDisplayBlock.SIDE)) {
			case BOTH:
				b = state.getValue(AbstractAdvancedSidedDisplayBlock.FACING).getAxis() == direction.getAxis();
				break;
			default:
				b = state.getValue(AbstractAdvancedSidedDisplayBlock.FACING) == direction;
				break;
		}
		return b ? layerShift : null;
	}
    
    @Override
	public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter reader, BlockPos pos, BlockPos otherPos, Direction face, Direction primaryOffset, Direction secondaryOffset) {
		return reader.getBlockEntity(pos) instanceof AdvancedDisplayBlockEntity blockEntity && blockEntity.connectable(reader, pos, otherPos);
	}

	@Override
	protected boolean isBeingBlocked(BlockState state, BlockAndTintGetter reader, BlockPos pos, BlockPos otherPos, Direction face) {
		return (state.getValue(HorizontalDirectionalBlock.FACING) == face && super.isBeingBlocked(state, reader, pos, otherPos, face));
	}

	@Override
	protected boolean reverseUVs(BlockState state, Direction face) {
		Axis axis = state.getValue(HorizontalDirectionalBlock.FACING).getAxis();
		if (axis == Axis.X)
			return face.getAxisDirection() == AxisDirection.NEGATIVE && face.getAxis() != Axis.X;
		if (axis == Axis.Z)
			return face != Direction.NORTH && face.getAxisDirection() != AxisDirection.POSITIVE;
		return super.reverseUVs(state, face);
	}

	@Override
	protected boolean reverseUVsHorizontally(BlockState state, Direction face) {
		return super.reverseUVsHorizontally(state, face);
	}

	@Override
	protected boolean reverseUVsVertically(BlockState state, Direction face) {
		Axis axis = state.getValue(HorizontalDirectionalBlock.FACING).getAxis();
		if (axis == Axis.X && face == Direction.NORTH)
			return false;
		if (axis == Axis.Z && face == Direction.WEST)
			return false;
		return super.reverseUVsVertically(state, face);
	}

	@Override
	protected Direction getUpDirection(BlockAndTintGetter reader, BlockPos pos, BlockState state, Direction face) {
		Axis axis = state.getValue(HorizontalDirectionalBlock.FACING).getAxis();
		if (axis == Axis.Y)
			return super.getUpDirection(reader, pos, state, face);
		boolean alongX = axis == Axis.X;
		if (face.getAxis().isVertical() && alongX)
			return super.getUpDirection(reader, pos, state, face).getClockWise();
		if (face.getAxis() == axis || face.getAxis().isVertical())
			return super.getUpDirection(reader, pos, state, face);
		return Direction.fromAxisAndDirection(axis, alongX ? AxisDirection.POSITIVE : AxisDirection.NEGATIVE);
	}

	@Override
	protected Direction getRightDirection(BlockAndTintGetter reader, BlockPos pos, BlockState state, Direction face) {
		Axis axis = state.getValue(HorizontalDirectionalBlock.FACING).getAxis();
		if (axis == Axis.Y)
			return super.getRightDirection(reader, pos, state, face);
		if (face.getAxis().isVertical() && axis == Axis.X)
			return super.getRightDirection(reader, pos, state, face).getClockWise();
		if (face.getAxis() == axis || face.getAxis().isVertical())
			return super.getRightDirection(reader, pos, state, face);
		return Direction.fromAxisAndDirection(Axis.Y, face.getAxisDirection());
	}
}
