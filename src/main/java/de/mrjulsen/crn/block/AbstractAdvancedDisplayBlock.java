package de.mrjulsen.crn.block;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.placement.IPlacementHelper;
import com.simibubi.create.foundation.placement.PlacementHelpers;
import de.mrjulsen.crn.block.be.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.crn.data.ESide;
import de.mrjulsen.crn.registry.ModBlockEntities;
import de.mrjulsen.crn.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

public abstract class AbstractAdvancedDisplayBlock extends Block implements IWrenchable, IBE<AdvancedDisplayBlockEntity> {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<ESide> SIDE = EnumProperty.create("side", ESide.class);

    public AbstractAdvancedDisplayBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(SIDE, ESide.FRONT)
        );
    }

    @Override
    public BlockState rotate(BlockState pState, Rotation pRotation) {
        return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)));
    }    

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
    }
    
    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(FACING, SIDE);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        
        ItemStack heldItem = pPlayer.getItemInHand(pHand);
        AdvancedDisplayBlockEntity blockEntity = ((AdvancedDisplayBlockEntity)pLevel.getBlockEntity(pPos)).getController();

        IPlacementHelper placementHelper = PlacementHelpers.get(getPlacementHelperID());
        if (placementHelper.matchesItem(heldItem))
            return placementHelper.getOffset(pPlayer, pLevel, pState, pPos, pHit).placeInWorld(pLevel, (BlockItem)heldItem.getItem(), pPlayer, pHand, pHit);
            
		DyeColor dye = DyeColor.getColor(heldItem);        
		if (dye != null) {
			pLevel.playSound(null, pPos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
			blockEntity.applyToAll(be -> {
                be.setColor(dye == DyeColor.ORANGE ? 0xFF9900 : dye.getMaterialColor().col);
            });
            return InteractionResult.SUCCESS;
		}

        /*
        if (heldItem.is(Items.GLOW_INK_SAC)) {
            pLevel.playSound(null, pPos, SoundEvents.GLOW_INK_SAC_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
			blockEntity.applyToAll(be -> {
                be.setGlowing(true);
            });
            return InteractionResult.SUCCESS;
        }
        */

		return InteractionResult.FAIL;
    }

    @Override
    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {        
        Direction leftDirection = pState.getValue(HorizontalDirectionalBlock.FACING).getClockWise();
        BlockPos leftPos = pPos.relative(leftDirection);
        updateNeighbour(pState, pLevel, pPos, pOldState, pIsMoving, leftDirection, leftPos);

        Direction rightDirection = pState.getValue(HorizontalDirectionalBlock.FACING).getCounterClockWise();
        BlockPos rightPos = pPos.relative(rightDirection);        
        updateNeighbour(pState, pLevel, pPos, pOldState, pIsMoving, rightDirection, rightPos);
    }

    private void updateNeighbour(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving, Direction direction, BlockPos neighbourPos) {
        if (pLevel.getBlockState(neighbourPos).is(this) && pLevel.getBlockEntity(neighbourPos) instanceof AdvancedDisplayBlockEntity otherBe && pLevel.getBlockEntity(pPos) instanceof AdvancedDisplayBlockEntity be && be.connectedTo(otherBe)) {
            be.setColor(otherBe.getColor());
            be.setDisplayType(otherBe.getDisplayType());
            be.setInfoType(otherBe.getInfoType());
            if (pLevel.isClientSide) {
                be.getRenderer().update(pLevel, neighbourPos, pOldState, otherBe);
            }
            return;
        }
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

    protected abstract int getPlacementHelperID();
    public abstract Pair<Float, Float> getRenderOffset(Level level, BlockState blockState, BlockPos pos);
    /** First value: Front side, Second value: Back side */ public abstract Pair<Float, Float> getRenderZOffset(Level level, BlockState blockState, BlockPos pos);
    public abstract Pair<Float, Float> getRenderAspectRatio(Level level, BlockState blockState, BlockPos pos);
}
