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
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AdvancedDisplayPanelBlock extends AbstractAdvancedDisplayBlock {
        
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
            .setValue(Z_ALIGN, EBlockAlignment.CENTER)
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
	public BlockState getStateForPlacement(BlockPlaceContext pContext) {
		BlockState stateForPlacement = super.getStateForPlacement(pContext);
		Direction direction = pContext.getClickedFace();
        Direction looking = pContext.getHorizontalDirection();
        Axis axis = looking.getAxis();
        AxisDirection axisDirection = looking.getAxisDirection();

        double xzPos = 0.5f;
        if (axis == Axis.X) {
            xzPos = pContext.getClickLocation().x - pContext.getClickedPos().getX();
        } else if (axis == Axis.Z) {            
            xzPos = pContext.getClickLocation().z - pContext.getClickedPos().getZ();
        }

        EBlockAlignment zAlign = EBlockAlignment.CENTER;

        if (direction == pContext.getPlayer().getDirection().getOpposite() || (axisDirection == AxisDirection.POSITIVE ? xzPos > 0.66666666D : xzPos < 0.33333333D)) {
			zAlign = EBlockAlignment.POSITIVE;
        } else if (direction == pContext.getPlayer().getDirection() || (axisDirection == AxisDirection.POSITIVE ? xzPos < 0.33333333D : xzPos > 0.66666666D)) {
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
    public boolean canConnectWithBlock(BlockAndTintGetter level, BlockPos selfPos, BlockPos otherPos) {
		return super.canConnectWithBlock(level, selfPos, otherPos) &&
            level.getBlockState(selfPos).getValue(Z_ALIGN) == level.getBlockState(otherPos).getValue(Z_ALIGN)
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
