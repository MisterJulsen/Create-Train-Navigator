package de.mrjulsen.crn.block;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.mrjulsen.crn.data.EBlockAlignment;
import de.mrjulsen.mcdragonlib.data.Pair;
import de.mrjulsen.mcdragonlib.data.Tripple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
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

public class AdvancedDisplayPanelBlock extends AbstractAdvancedSidedDisplayBlock {
        
	public static final EnumProperty<EBlockAlignment> Z_ALIGN = EnumProperty.create("z_alignment", EBlockAlignment.class);

    private static final Map<ShapeKey, VoxelShape> SHAPES = Map.ofEntries(
        Map.entry(new ShapeKey(Direction.SOUTH, EBlockAlignment.NEGATIVE), Block.box(0 , 0 , 13, 16, 16, 16)),
        Map.entry(new ShapeKey(Direction.NORTH, EBlockAlignment.NEGATIVE), Block.box(0 , 0 , 0 , 16, 16, 3 )),
        Map.entry(new ShapeKey(Direction.EAST,  EBlockAlignment.NEGATIVE), Block.box(13, 0 , 0 , 16, 16, 16)),
        Map.entry(new ShapeKey(Direction.WEST,  EBlockAlignment.NEGATIVE), Block.box(0 , 0 , 0 , 3 , 16, 16)),
        
        Map.entry(new ShapeKey(Direction.SOUTH, EBlockAlignment.CENTER),   Block.box(0   , 0 , 6.5f, 16  , 16, 9.5f)),
        Map.entry(new ShapeKey(Direction.NORTH, EBlockAlignment.CENTER),   Block.box(0   , 0 , 6.5f, 16  , 16, 9.5f)),
        Map.entry(new ShapeKey(Direction.EAST,  EBlockAlignment.CENTER),   Block.box(6.5f, 0 , 0   , 9.5f, 16, 16  )),
        Map.entry(new ShapeKey(Direction.WEST,  EBlockAlignment.CENTER),   Block.box(6.5f, 0 , 0   , 9.5f, 16, 16  )),
        
        Map.entry(new ShapeKey(Direction.SOUTH, EBlockAlignment.POSITIVE), Block.box(0 , 0 , 0 , 16, 16, 3 )),
        Map.entry(new ShapeKey(Direction.NORTH, EBlockAlignment.POSITIVE), Block.box(0 , 0 , 13, 16, 16, 16)),
        Map.entry(new ShapeKey(Direction.EAST,  EBlockAlignment.POSITIVE), Block.box(0 , 0 , 0 , 3 , 16, 16)),
        Map.entry(new ShapeKey(Direction.WEST,  EBlockAlignment.POSITIVE), Block.box(13, 0 , 0 , 16, 16, 16))
    );

    public AdvancedDisplayPanelBlock(Properties properties) {
        super(properties);
    registerDefaultState(defaultBlockState()
            .setValue(Z_ALIGN, EBlockAlignment.POSITIVE)
        );
    }
    
    @Override
    public Collection<Property<?>> getExcludedProperties() {
        return List.of(Z_ALIGN);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPES.get(new ShapeKey(pState.getValue(FACING), pState.getValue(Z_ALIGN)));
    }

    

    @Override
    public BlockState getDefaultPlacementState(BlockPlaceContext context, BlockState state, BlockState other) {
        BlockState stateForPlacement = super.getDefaultPlacementState(context, state, other);
		Direction direction = context.getClickedFace();
        Direction looking = context.getHorizontalDirection();
        Axis axis = looking.getAxis();
        AxisDirection axisDirection = looking.getAxisDirection();

        double xzPos = 0.5f;
        if (axis == Axis.X) {
            xzPos = context.getClickLocation().x - context.getClickedPos().getX();
        } else if (axis == Axis.Z) {            
            xzPos = context.getClickLocation().z - context.getClickedPos().getZ();
        }

        EBlockAlignment zAlign = EBlockAlignment.POSITIVE;

        if (direction == context.getPlayer().getDirection() || (axisDirection == AxisDirection.POSITIVE ? xzPos < 0.5D : xzPos > 0.5D)) {
			zAlign = EBlockAlignment.NEGATIVE;
        }

		return stateForPlacement
            .setValue(Z_ALIGN, zAlign)
        ;
    }

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
		super.createBlockStateDefinition(pBuilder.add(Z_ALIGN));
	}
    
    @Override
    public BlockState appendOnPlace(BlockPlaceContext context, BlockState state, BlockState other) {
        return super.appendOnPlace(context, state, other)
            .setValue(Z_ALIGN, other.getValue(Z_ALIGN))
        ;
    }
    
    @Override
    public boolean canConnectWithBlock(BlockGetter level, BlockState selfState, BlockState otherState) {
		return super.canConnectWithBlock(level, selfState, otherState) &&
            selfState.getValue(Z_ALIGN) == otherState.getValue(Z_ALIGN)
		;
	}

    @Override
    protected boolean canConnect(LevelAccessor level, BlockPos pos, BlockState state, BlockState other) {
        return super.canConnect(level, pos, state, other) &&
            state.getValue(Z_ALIGN) == other.getValue(Z_ALIGN)
        ;
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
        float z1;
        float z2;
        switch (blockState.getValue(Z_ALIGN)) {
            case NEGATIVE:
                z1 = 16.05f;
                z2 = 3.05f;
                break;
            case POSITIVE:
                z1 = 3.05f;
                z2 = 16.05f;
                break;
            default:
                z1 = 9.55f;
                z2 = 9.55f;
                break;
        }
        return Pair.of(z1, z2);
    }

    @Override
    public Tripple<Float, Float, Float> getRenderRotation(Level level, BlockState blockState, BlockPos pos) {
        return Tripple.of(0.0F, 0.0F, 0.0F);
    }

    @Override
    public boolean isSingleLined() {
        return false;
    }

    private static final class ShapeKey {
        private final Direction facing;
        private final EBlockAlignment zAlign;
    
        public ShapeKey(Direction facing, EBlockAlignment zAlign) {
            this.facing = facing;
            this.zAlign = zAlign;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof ShapeKey other) {
                return facing == other.facing && zAlign == other.zAlign;
            }
            return false;
        }
    
        @Override
        public int hashCode() {
            return Objects.hash(facing, zAlign);
        }
    }
}
