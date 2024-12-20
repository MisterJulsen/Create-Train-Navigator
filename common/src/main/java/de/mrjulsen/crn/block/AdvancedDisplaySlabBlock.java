package de.mrjulsen.crn.block;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.mrjulsen.crn.block.properties.EBlockAlignment;
import de.mrjulsen.mcdragonlib.data.Pair;
import de.mrjulsen.mcdragonlib.data.Tripple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AdvancedDisplaySlabBlock extends AbstractAdvancedSidedDisplayBlock {
    
	public static final EnumProperty<EBlockAlignment> Y_ALIGN = EnumProperty.create("y_alignment", EBlockAlignment.class);

    private static final Map<ShapeKey, VoxelShape> SHAPES = Map.ofEntries(
        Map.entry(new ShapeKey(Direction.SOUTH, EBlockAlignment.NEGATIVE), Block.box(0 , 0 , 0 , 16, 8 , 16)),
        Map.entry(new ShapeKey(Direction.NORTH, EBlockAlignment.NEGATIVE), Block.box(0 , 0 , 0 , 16, 8 , 16)),
        Map.entry(new ShapeKey(Direction.EAST,  EBlockAlignment.NEGATIVE), Block.box(0 , 0 , 0 , 16, 8 , 16)),
        Map.entry(new ShapeKey(Direction.WEST,  EBlockAlignment.NEGATIVE), Block.box(0 , 0 , 0 , 16, 8 , 16)),
        
        Map.entry(new ShapeKey(Direction.SOUTH, EBlockAlignment.CENTER),   Block.box(0 , 4 , 0 , 16, 12, 16)),
        Map.entry(new ShapeKey(Direction.NORTH, EBlockAlignment.CENTER),   Block.box(0 , 4 , 0 , 16, 12, 16)),
        Map.entry(new ShapeKey(Direction.EAST,  EBlockAlignment.CENTER),   Block.box(0 , 4 , 0 , 16, 12, 16)),
        Map.entry(new ShapeKey(Direction.WEST,  EBlockAlignment.CENTER),   Block.box(0 , 4 , 0 , 16, 12, 16)),
        
        Map.entry(new ShapeKey(Direction.SOUTH, EBlockAlignment.POSITIVE), Block.box(0 , 8 , 0 , 16, 16, 16)),
        Map.entry(new ShapeKey(Direction.NORTH, EBlockAlignment.POSITIVE), Block.box(0 , 8 , 0 , 16, 16, 16)),
        Map.entry(new ShapeKey(Direction.EAST,  EBlockAlignment.POSITIVE), Block.box(0 , 8 , 0 , 16, 16, 16)),
        Map.entry(new ShapeKey(Direction.WEST,  EBlockAlignment.POSITIVE), Block.box(0 , 8 , 0 , 16, 16, 16))
    );

    public AdvancedDisplaySlabBlock(Properties properties) {
        super(properties);        
		registerDefaultState(defaultBlockState()
            .setValue(Y_ALIGN, EBlockAlignment.CENTER)
        );
    }
    
    @Override
    public Collection<Property<?>> getExcludedProperties() {
        return List.of(Y_ALIGN);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPES.get(new ShapeKey(pState.getValue(FACING), pState.getValue(Y_ALIGN)));
    }

    @Override
    public BlockState getDefaultPlacementState(BlockPlaceContext context, BlockState state, BlockState other) {
        BlockState stateForPlacement = super.getDefaultPlacementState(context, state, other);
		Direction direction = context.getClickedFace();

        EBlockAlignment yAlign = EBlockAlignment.CENTER;

		if (direction == Direction.UP || (context.getClickLocation().y - context.getClickedPos().getY() < 0.33333333D)) {
			yAlign = EBlockAlignment.NEGATIVE;
        } else if (direction == Direction.DOWN || (context.getClickLocation().y - context.getClickedPos().getY() > 0.66666666D)) {
            yAlign = EBlockAlignment.POSITIVE;
        }

		return stateForPlacement
            .setValue(Y_ALIGN, yAlign)
        ;
    }

    @Override
    public BlockState appendOnPlace(BlockPlaceContext context, BlockState state, BlockState other) {
        return super.appendOnPlace(context, state, other)
            .setValue(Y_ALIGN, other.getValue(Y_ALIGN))
        ;
    }

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
		super.createBlockStateDefinition(pBuilder.add(Y_ALIGN));
	}
    
    @Override
    public boolean canConnectWithBlock(BlockGetter level, BlockState selfState, BlockState otherState) {
		return super.canConnectWithBlock(level, selfState, otherState) &&
            selfState.getValue(Y_ALIGN) == otherState.getValue(Y_ALIGN)
		;
	}

    @Override
    protected boolean canConnect(LevelAccessor level, BlockPos pos, BlockState state, BlockState other) {
        return super.canConnect(level, pos, state, other) &&
            state.getValue(Y_ALIGN) == other.getValue(Y_ALIGN)
        ;
    }

    @Override
    public Pair<Float, Float> getRenderAspectRatio(Level level, BlockState blockState, BlockPos pos) {
        return Pair.of(1.0F, 0.5F);
    }

    @Override
    public Pair<Float, Float> getRenderOffset(Level level, BlockState blockState, BlockPos pos) {
        float y;
        switch (blockState.getValue(Y_ALIGN)) {
            case NEGATIVE:
                y = 8.0f;
                break;
            case POSITIVE:
                y = 0.0f;
                break;
            default:
                y = 4.0f;
                break;
        }
        return Pair.of(0.0f, y);
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
        return true;
    }
    
    private static final class ShapeKey {
        private final Direction facing;
        private final EBlockAlignment yAlign;
    
        public ShapeKey(Direction facing, EBlockAlignment yAlign) {
            this.facing = facing;
            this.yAlign = yAlign;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof ShapeKey other) {
                return facing == other.facing && yAlign == other.yAlign;
            }
            return false;
        }
    
        @Override
        public int hashCode() {
            return Objects.hash(facing, yAlign);
        }
    }
}
