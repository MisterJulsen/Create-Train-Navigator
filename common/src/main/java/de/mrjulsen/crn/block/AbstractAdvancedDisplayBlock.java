package de.mrjulsen.crn.block;

import java.util.Collection;
import java.util.List;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.Iterate;

import de.mrjulsen.crn.block.be.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.crn.registry.ModBlockEntities;
import de.mrjulsen.mcdragonlib.client.ber.IBlockEntityRendererInstance.EUpdateReason;
import de.mrjulsen.mcdragonlib.data.Pair;
import de.mrjulsen.mcdragonlib.data.Tripple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.ticks.LevelTickAccess;

public abstract class AbstractAdvancedDisplayBlock extends Block implements IWrenchable, IBE<AdvancedDisplayBlockEntity> {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    
	public static final BooleanProperty UP = BooleanProperty.create("up");
	public static final BooleanProperty DOWN = BooleanProperty.create("down");

    public AbstractAdvancedDisplayBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(this.stateDefinition.any()
            .setValue(UP, false)
            .setValue(DOWN, false)
            .setValue(FACING, Direction.NORTH)
        );
    }

    @Override
    public BlockState rotate(BlockState pState, Rotation pRotation) {
        return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)));
    }    

    @Override
    public BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
    }
    
    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(UP, DOWN, FACING);
    }

    @Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction face = context.getClickedFace();
		BlockPos clickedPos = context.getClickedPos();
		BlockPos placedOnPos = clickedPos.relative(face.getOpposite());
		Level level = context.getLevel();
		BlockState otherState = level.getBlockState(placedOnPos);
		BlockState stateForPlacement = this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());

		if ((otherState.getBlock() != this) || (context.getPlayer() != null && context.getPlayer().isShiftKeyDown())) {
			stateForPlacement = getDefaultPlacementState(context, stateForPlacement, otherState);
		} else { // Clicked on existing block
			stateForPlacement = appendOnPlace(context, stateForPlacement, otherState);
		}

		return updateColumn(level, clickedPos, stateForPlacement, true);
	}

	public BlockState appendOnPlace(BlockPlaceContext context, BlockState state, BlockState other) {
		Direction otherFacing = other.getValue(FACING);
		state = state
			.setValue(FACING, otherFacing)
		;
		return state;
	}

	public BlockState getDefaultPlacementState(BlockPlaceContext context, BlockState state, BlockState other) {
		return super.getStateForPlacement(context).setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

    protected BlockState updateColumn(Level level, BlockPos pos, BlockState state, boolean present) {
		MutableBlockPos currentPos = new MutableBlockPos();
		Axis axis = getConnectionAxis(state);

		for (Direction connection : Iterate.directionsInAxis(Axis.Y)) {
			boolean connect = true;

			Move: for (Direction movement : Iterate.directionsInAxis(axis)) {
				currentPos.set(pos);
				for (int i = 0; i < 1000; i++) {
					if (!level.isLoaded(currentPos))
						break;

					BlockPos otherPos = currentPos.relative(connection);
					BlockState other1 = currentPos.equals(pos) ? state : level.getBlockState(currentPos);
					BlockState other2 = level.getBlockState(otherPos);
					boolean col1 = canConnect(level, pos, state, other1);
					boolean col2 = canConnect(level, pos, state, other2);
					currentPos.move(movement);

					if (!col1 && !col2)
						break;
					if (col1 && col2)
						continue;

					connect = false;
					break Move;
				}
			}
			state = setConnection(state, connection, connect);
		}
		return state;
	}

	@Override
	public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
		if (pOldState.getBlock() == this)
			return;
		LevelTickAccess<Block> blockTicks = pLevel.getBlockTicks();
		if (!blockTicks.hasScheduledTick(pPos, this))
			pLevel.scheduleTick(pPos, this, 1);

		updateNeighbours(pState, pLevel, pPos);

		if (pLevel.isClientSide) {
			withBlockEntityDo(pLevel, pPos, be -> be.getController().getRenderer().update(pLevel, pPos, pState, be, EUpdateReason.BLOCK_CHANGED));			
		}
	}

	public <T extends Comparable<T>> BlockState getPropertyFromNeighbours(BlockState pState, Level pLevel, BlockPos pPos, Property<T> property) {
		Direction leftDirection = pState.getValue(HorizontalDirectionalBlock.FACING).getClockWise();
		BlockState newState = null;
		BlockPos relPos = pPos.relative(leftDirection);
		if ((newState = getPropertyFromNeighbour(pState, pLevel, pPos, relPos, property)) != null) {
			return newState;
		}
		relPos = pPos.relative(Direction.UP);        
		if ((newState = getPropertyFromNeighbour(pState, pLevel, pPos, relPos, property)) != null) {
			return newState;
		}
		relPos = pPos.relative(leftDirection.getOpposite());
		if ((newState = getPropertyFromNeighbour(pState, pLevel, pPos, relPos, property)) != null) {
			return newState;
		}
		relPos = pPos.relative(Direction.DOWN);        
		if ((newState = getPropertyFromNeighbour(pState, pLevel, pPos, relPos, property)) != null) {
			return newState;
		}
		return pState;
	}

	public <T extends Comparable<T>> BlockState getPropertyFromNeighbour(BlockState pState, Level pLevel, BlockPos pPos, BlockPos relPos, Property<T> property) {
		if (canConnectWithBlock(pLevel, pState, pLevel.getBlockState(relPos))) {
			return pState.setValue(property, pLevel.getBlockState(relPos).getValue(property));
		}
		return null;
	}

	public void updateNeighbours(BlockState pState, Level pLevel, BlockPos pPos) {
		Direction leftDirection = pState.getValue(HorizontalDirectionalBlock.FACING).getClockWise();
		BlockPos relPos = pPos.relative(leftDirection);
		if (updateNeighbour(pState, pLevel, pPos, relPos)) {
			return;
		}
		relPos = pPos.relative(Direction.UP);        
		if (updateNeighbour(pState, pLevel, pPos, relPos)) {
			return;
		}
		relPos = pPos.relative(leftDirection.getOpposite());
		if (updateNeighbour(pState, pLevel, pPos, relPos)) {
			return;
		}
		relPos = pPos.relative(Direction.DOWN);        
		if (updateNeighbour(pState, pLevel, pPos, relPos)) {
			return;
		}
	}

    @Override
	public void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
		if (pState.getBlock() != this)
			return;
		BlockPos belowPos = pPos.relative(Direction.fromAxisAndDirection(getConnectionAxis(pState), AxisDirection.NEGATIVE));
		BlockState belowState = pLevel.getBlockState(belowPos);
		if (!canConnect(pLevel, pPos, pState, belowState))
			KineticBlockEntity.switchToBlockState(pLevel, pPos, updateColumn(pLevel, pPos, pState, true));
		withBlockEntityDo(pLevel, pPos, AdvancedDisplayBlockEntity::updateControllerStatus);
	}

    @Override
	public BlockState updateShape(BlockState state, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pNeighborPos) {
		return updatedShapeInner(state, pDirection, pNeighborState, pLevel, pCurrentPos, pNeighborPos);
	}

	private BlockState updatedShapeInner(BlockState state, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pNeighborPos) {
		if (!canConnect(pLevel, pCurrentPos, state, pNeighborState))
			return setConnection(state, pDirection, false);
		if (pDirection.getAxis() == getConnectionAxis(state))
			return applyPropertiesOf(state, pNeighborState);
		return setConnection(state, pDirection, getConnection(pNeighborState, pDirection.getOpposite()));
	}

	public BlockState applyPropertiesOf(BlockState current, BlockState state) {
        BlockState blockState = this.defaultBlockState();
        for (Property<?> property : state.getBlock().getStateDefinition().getProperties()) {
            if (!blockState.hasProperty(property))
				continue;

			if (getExcludedProperties().contains(property)) {
				blockState = copyPropertyOf(current, blockState, property);
				continue;
			}
			
            blockState = copyPropertyOf(state, blockState, property);
        }
        return blockState;
    }

	private static <T extends Comparable<T>> BlockState copyPropertyOf(BlockState sourceState, BlockState targetState, Property<T> property) {
        return (BlockState)targetState.setValue(property, sourceState.getValue(property));
    }

    protected boolean canConnect(LevelAccessor level, BlockPos pos, BlockState state, BlockState other) {
		return other.getBlock() == this && state.getValue(FACING) == other.getValue(FACING);
	}

	public boolean canConnectWithBlock(BlockGetter level, BlockState selfState, BlockState otherState) {
		return selfState.getBlock() instanceof AbstractAdvancedDisplayBlock && otherState.getBlock() instanceof AbstractAdvancedDisplayBlock &&
			selfState.getBlock() == otherState.getBlock() &&
			selfState.getValue(FACING) == otherState.getValue(FACING)
		;
	}

	protected Axis getConnectionAxis(BlockState state) {
		return state.getValue(FACING).getClockWise().getAxis();
	}

	public static boolean getConnection(BlockState state, Direction side) {
		BooleanProperty property = side == Direction.DOWN ? DOWN : side == Direction.UP ? UP : null;
		return property != null && state.getValue(property);
	}

	public static BlockState setConnection(BlockState state, Direction side, boolean connect) {
		BooleanProperty property = side == Direction.DOWN ? DOWN : side == Direction.UP ? UP : null;
		if (property != null)
			state = state.setValue(property, connect);
			
		return state;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
		super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
		if (pIsMoving || pNewState.getBlock() == this)
			return;
		for (Direction d : Iterate.directionsInAxis(getConnectionAxis(pState))) {
			BlockPos relative = pPos.relative(d);
			BlockState adjacent = pLevel.getBlockState(relative);
			if (canConnect(pLevel, pPos, pState, adjacent))
				KineticBlockEntity.switchToBlockState(pLevel, relative, updateColumn(pLevel, relative, adjacent, false));
		}
	}

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        
        ItemStack heldItem = pPlayer.getItemInHand(pHand);
        AdvancedDisplayBlockEntity blockEntity = ((AdvancedDisplayBlockEntity)pLevel.getBlockEntity(pPos)).getController();

		if (heldItem.getItem() instanceof DyeItem dyeItem) {
			DyeColor dye = dyeItem.getDyeColor();        
			if (dye != null) {
				pLevel.playSound(null, pPos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
				blockEntity.applyToAll(be -> {
					be.setColor(dye == DyeColor.ORANGE ? 0xFF9900 : dye.getMapColor().col);				
					be.notifyUpdate();
				});

				if (pLevel.isClientSide) {
					blockEntity.getRenderer().update(pLevel, pPos, pState, blockEntity, EUpdateReason.BLOCK_CHANGED);
				}

				return InteractionResult.SUCCESS;
			}
		}
       
		if (heldItem.is(Items.GLOW_INK_SAC)) {
			pLevel.playSound(null, pPos, SoundEvents.GLOW_INK_SAC_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
			blockEntity.applyToAll(be -> {
                be.setGlowing(true);
				be.notifyUpdate();
            });
			
			if (pLevel.isClientSide) {
				blockEntity.getRenderer().update(pLevel, pPos, pState, blockEntity, EUpdateReason.BLOCK_CHANGED);
			}

            return InteractionResult.SUCCESS;
		}

		return InteractionResult.FAIL;
    }
	
    protected boolean updateNeighbour(BlockState pState, Level pLevel, BlockPos pPos, BlockPos neighbourPos) {
        if (pLevel.getBlockState(neighbourPos).is(this) && pLevel.getBlockEntity(neighbourPos) instanceof AdvancedDisplayBlockEntity otherBe && pLevel.getBlockEntity(pPos) instanceof AdvancedDisplayBlockEntity be) {
	    	be.copyFrom(otherBe);
            return true;
        }
		return false;
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide && level.getBlockEntity(context.getClickedPos()) instanceof AdvancedDisplayBlockEntity be) {
            AdvancedDisplayBlockEntity controller = be.getController();
            if (controller != null) {
                ClientWrapper.showAdvancedDisplaySettingsScreen(controller);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.FAIL;
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public Class<AdvancedDisplayBlockEntity> getBlockEntityClass() {
        return AdvancedDisplayBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AdvancedDisplayBlockEntity> getBlockEntityType() {
        return ModBlockEntities.ADVANCED_DISPLAY_BLOCK_ENTITY.get();
    }

	public abstract boolean isSingleLined();

    public abstract Tripple<Float, Float, Float> getRenderRotation(Level level, BlockState blockState, BlockPos pos);
    public abstract Pair<Float, Float> getRenderOffset(Level level, BlockState blockState, BlockPos pos);
    /** First value: Front side, Second value: Back side */ public abstract Pair<Float, Float> getRenderZOffset(Level level, BlockState blockState, BlockPos pos);
    public abstract Pair<Float, Float> getRenderAspectRatio(Level level, BlockState blockState, BlockPos pos);

	public Collection<Property<?>> getExcludedProperties() {
		return List.of();
	}
}
