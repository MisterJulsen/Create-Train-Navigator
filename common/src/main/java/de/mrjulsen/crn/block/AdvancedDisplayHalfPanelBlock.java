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

public class AdvancedDisplayHalfPanelBlock extends AbstractAdvancedSidedDisplayBlock {

    public static final EnumProperty<EBlockAlignment> Y_ALIGN = EnumProperty.create("y_alignment", EBlockAlignment.class);
    public static final EnumProperty<EBlockAlignment> Z_ALIGN = EnumProperty.create("z_alignment", EBlockAlignment.class);

    private static final Map<ShapeKey, VoxelShape> SHAPES = Map.ofEntries(
        Map.entry(new ShapeKey(Direction.SOUTH, EBlockAlignment.NEGATIVE, EBlockAlignment.NEGATIVE), Block.box(0 , 0 , 13, 16, 8 , 16)),
        Map.entry(new ShapeKey(Direction.NORTH, EBlockAlignment.NEGATIVE, EBlockAlignment.NEGATIVE), Block.box(0 , 0 , 0 , 16, 8 , 3 )),
        Map.entry(new ShapeKey(Direction.EAST,  EBlockAlignment.NEGATIVE, EBlockAlignment.NEGATIVE), Block.box(13, 0 , 0 , 16, 8 , 16)),
        Map.entry(new ShapeKey(Direction.WEST,  EBlockAlignment.NEGATIVE, EBlockAlignment.NEGATIVE), Block.box(0 , 0 , 0 , 3 , 8 , 16)),

        Map.entry(new ShapeKey(Direction.SOUTH, EBlockAlignment.CENTER,   EBlockAlignment.NEGATIVE), Block.box(0 , 4 , 13, 16, 12, 16)),
        Map.entry(new ShapeKey(Direction.NORTH, EBlockAlignment.CENTER,   EBlockAlignment.NEGATIVE), Block.box(0 , 4 , 0 , 16, 12, 3 )),
        Map.entry(new ShapeKey(Direction.EAST,  EBlockAlignment.CENTER,   EBlockAlignment.NEGATIVE), Block.box(13, 4 , 0 , 16, 12, 16)),
        Map.entry(new ShapeKey(Direction.WEST,  EBlockAlignment.CENTER,   EBlockAlignment.NEGATIVE), Block.box(0 , 4 , 0 , 3 , 12, 16)),

        Map.entry(new ShapeKey(Direction.SOUTH, EBlockAlignment.POSITIVE, EBlockAlignment.NEGATIVE), Block.box(0 , 8 , 13, 16, 16, 16)),
        Map.entry(new ShapeKey(Direction.NORTH, EBlockAlignment.POSITIVE, EBlockAlignment.NEGATIVE), Block.box(0 , 8 , 0 , 16, 16, 3 )),
        Map.entry(new ShapeKey(Direction.EAST,  EBlockAlignment.POSITIVE, EBlockAlignment.NEGATIVE), Block.box(13, 8 , 0 , 16, 16, 16)),
        Map.entry(new ShapeKey(Direction.WEST,  EBlockAlignment.POSITIVE, EBlockAlignment.NEGATIVE), Block.box(0 , 8 , 0 , 3 , 16, 16)),



        Map.entry(new ShapeKey(Direction.SOUTH, EBlockAlignment.NEGATIVE, EBlockAlignment.CENTER),   Block.box(0 , 0 , 6.5, 16, 8, 9.5)),
        Map.entry(new ShapeKey(Direction.NORTH, EBlockAlignment.NEGATIVE, EBlockAlignment.CENTER),   Block.box(0 , 0 , 6.5, 16, 8, 9.5)),
        Map.entry(new ShapeKey(Direction.EAST,  EBlockAlignment.NEGATIVE, EBlockAlignment.CENTER),   Block.box(6.5, 0 , 0 , 9.5, 8 , 16)),
        Map.entry(new ShapeKey(Direction.WEST,  EBlockAlignment.NEGATIVE, EBlockAlignment.CENTER),   Block.box(6.5, 0 , 0 , 9.5, 8 , 16)),

        Map.entry(new ShapeKey(Direction.SOUTH, EBlockAlignment.CENTER,   EBlockAlignment.CENTER),   Block.box(0 , 4 , 6.5, 16, 12, 9.5)),
        Map.entry(new ShapeKey(Direction.NORTH, EBlockAlignment.CENTER,   EBlockAlignment.CENTER),   Block.box(0 , 4 , 6.5, 16, 12, 9.5)),
        Map.entry(new ShapeKey(Direction.EAST,  EBlockAlignment.CENTER,   EBlockAlignment.CENTER),   Block.box(6.5, 4 , 0 , 9.5, 12 , 16)),
        Map.entry(new ShapeKey(Direction.WEST,  EBlockAlignment.CENTER,   EBlockAlignment.CENTER),   Block.box(6.5, 4 , 0 , 9.5, 12 , 16)),

        Map.entry(new ShapeKey(Direction.SOUTH, EBlockAlignment.POSITIVE, EBlockAlignment.CENTER),   Block.box(0 , 8 , 6.5, 16, 16, 9.5)),
        Map.entry(new ShapeKey(Direction.NORTH, EBlockAlignment.POSITIVE, EBlockAlignment.CENTER),   Block.box(0 , 8 , 6.5, 16, 16, 9.5)),
        Map.entry(new ShapeKey(Direction.EAST,  EBlockAlignment.POSITIVE, EBlockAlignment.CENTER),   Block.box(6.5, 8 , 0 , 9.5, 16 , 16)),
        Map.entry(new ShapeKey(Direction.WEST,  EBlockAlignment.POSITIVE, EBlockAlignment.CENTER),   Block.box(6.5, 8 , 0 , 9.5, 16 , 16)),



        Map.entry(new ShapeKey(Direction.SOUTH, EBlockAlignment.NEGATIVE, EBlockAlignment.POSITIVE), Block.box(0 , 0 , 0 , 16, 8 , 3 )),
        Map.entry(new ShapeKey(Direction.NORTH, EBlockAlignment.NEGATIVE, EBlockAlignment.POSITIVE), Block.box(0 , 0 , 13, 16, 8 , 16)),
        Map.entry(new ShapeKey(Direction.EAST,  EBlockAlignment.NEGATIVE, EBlockAlignment.POSITIVE), Block.box(0 , 0 , 0 , 3 , 8 , 16)),
        Map.entry(new ShapeKey(Direction.WEST,  EBlockAlignment.NEGATIVE, EBlockAlignment.POSITIVE), Block.box(13, 0 , 0 , 16, 8 , 16)),

        Map.entry(new ShapeKey(Direction.SOUTH, EBlockAlignment.CENTER,   EBlockAlignment.POSITIVE), Block.box(0 , 4 , 0 , 16, 12 , 3 )),
        Map.entry(new ShapeKey(Direction.NORTH, EBlockAlignment.CENTER,   EBlockAlignment.POSITIVE), Block.box(0 , 4 , 13, 16, 12 , 16)),
        Map.entry(new ShapeKey(Direction.EAST,  EBlockAlignment.CENTER,   EBlockAlignment.POSITIVE), Block.box(0 , 4 , 0 , 3 , 12 , 16)),
        Map.entry(new ShapeKey(Direction.WEST,  EBlockAlignment.CENTER,   EBlockAlignment.POSITIVE), Block.box(13, 4 , 0 , 16, 12 , 16)),

        Map.entry(new ShapeKey(Direction.SOUTH, EBlockAlignment.POSITIVE, EBlockAlignment.POSITIVE), Block.box(0 , 8 , 0 , 16, 16 , 3 )),
        Map.entry(new ShapeKey(Direction.NORTH, EBlockAlignment.POSITIVE, EBlockAlignment.POSITIVE), Block.box(0 , 8 , 13, 16, 16 , 16)),
        Map.entry(new ShapeKey(Direction.EAST,  EBlockAlignment.POSITIVE, EBlockAlignment.POSITIVE), Block.box(0 , 8 , 0 , 3 , 16 , 16)),
        Map.entry(new ShapeKey(Direction.WEST,  EBlockAlignment.POSITIVE, EBlockAlignment.POSITIVE), Block.box(13, 8 , 0 , 16, 16 , 16))
    );

    public AdvancedDisplayHalfPanelBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
            .setValue(Y_ALIGN, EBlockAlignment.CENTER)
            .setValue(Z_ALIGN, EBlockAlignment.CENTER)
        );
    }

    @Override
    public Collection<Property<?>> getExcludedProperties() {
        return List.of(Y_ALIGN, Z_ALIGN);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPES.get(new ShapeKey(pState.getValue(FACING), pState.getValue(Y_ALIGN), pState.getValue(Z_ALIGN)));
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

        EBlockAlignment yAlign = EBlockAlignment.CENTER;
        EBlockAlignment zAlign = EBlockAlignment.CENTER;

		if (direction == Direction.UP || (context.getClickLocation().y - context.getClickedPos().getY() < 0.33333333D)) {
			yAlign = EBlockAlignment.NEGATIVE;
        } else if (direction == Direction.DOWN || (context.getClickLocation().y - context.getClickedPos().getY() > 0.66666666D)) {
            yAlign = EBlockAlignment.POSITIVE;
        }

        if (direction == context.getPlayer().getDirection().getOpposite() || (axisDirection == AxisDirection.POSITIVE ? xzPos > 0.66666666D : xzPos < 0.33333333D)) {
			zAlign = EBlockAlignment.POSITIVE;
        } else if (direction == context.getPlayer().getDirection() || (axisDirection == AxisDirection.POSITIVE ? xzPos < 0.33333333D : xzPos > 0.66666666D)) {
            zAlign = EBlockAlignment.NEGATIVE;
        }

		return stateForPlacement
            .setValue(Y_ALIGN, yAlign)
            .setValue(Z_ALIGN, zAlign)
        ;
    }

    @Override
    public BlockState appendOnPlace(BlockPlaceContext context, BlockState state, BlockState other) {
        return super.appendOnPlace(context, state, other)
            .setValue(Y_ALIGN, other.getValue(Y_ALIGN))
            .setValue(Z_ALIGN, other.getValue(Z_ALIGN))
        ;
    }

    @Override
    public boolean canConnectWithBlock(BlockGetter level, BlockState selfState, BlockState otherState) {
		return super.canConnectWithBlock(level, selfState, otherState) &&
            selfState.getValue(Y_ALIGN) == otherState.getValue(Y_ALIGN) && 
            selfState.getValue(Z_ALIGN) == otherState.getValue(Z_ALIGN)
		;
	}

    @Override
    protected boolean canConnect(LevelAccessor level, BlockPos pos, BlockState state, BlockState other) {
        return super.canConnect(level, pos, state, other) &&
            state.getValue(Y_ALIGN) == other.getValue(Y_ALIGN) && 
            state.getValue(Z_ALIGN) == other.getValue(Z_ALIGN)
        ;
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder.add(Y_ALIGN, Z_ALIGN));
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
        return true;
    }

    private static final class ShapeKey {
        private final Direction facing;
        private final EBlockAlignment yAlign;
        private final EBlockAlignment zAlign;

        public ShapeKey(Direction facing, EBlockAlignment yAlign, EBlockAlignment zAlign) {
            this.facing = facing;
            this.yAlign = yAlign;
            this.zAlign = zAlign;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof ShapeKey other) {
                return facing == other.facing && yAlign == other.yAlign && zAlign == other.zAlign;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(facing, yAlign, zAlign);
        }
    }
}