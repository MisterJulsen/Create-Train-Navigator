package de.mrjulsen.crn.block;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;

import de.mrjulsen.crn.block.be.TrainStationClockBlockEntity;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.registry.ModBlockEntities;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.ber.IBlockEntityRendererInstance.EUpdateReason;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.TimeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TrainStationClockBlock extends Block implements IWrenchable, IBE<TrainStationClockBlockEntity> {

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	
    private static final VoxelShape SHAPE_SN = Block.box(0, 0, 4, 16, 16, 12);
    private static final VoxelShape SHAPE_EW = Block.box(4, 0, 0, 12, 16, 16);

    public TrainStationClockBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
        );
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        
        ItemStack heldItem = pPlayer.getItemInHand(pHand);
        TrainStationClockBlockEntity blockEntity = ((TrainStationClockBlockEntity)pLevel.getBlockEntity(pPos));

		if (heldItem.getItem() instanceof DyeItem dyeItem) {
			DyeColor dye = dyeItem.getDyeColor();        
			if (dye != null) {
				pLevel.playSound(null, pPos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
				blockEntity.setColor(dye == DyeColor.ORANGE ? 0xFF9900 : dye.getMapColor().col);

				if (pLevel.isClientSide) {
					blockEntity.getRenderer().update(pLevel, pPos, pState, blockEntity, EUpdateReason.BLOCK_CHANGED);
				}

				return InteractionResult.SUCCESS;
			}
		}
       
		if (heldItem.is(Items.GLOW_INK_SAC)) {
			pLevel.playSound(null, pPos, SoundEvents.GLOW_INK_SAC_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
			blockEntity.setGlowing(true);
			
			if (pLevel.isClientSide) {
				blockEntity.getRenderer().update(pLevel, pPos, pState, blockEntity, EUpdateReason.BLOCK_CHANGED);
			}

            return InteractionResult.SUCCESS;
		}

		if (pLevel.isClientSide) {
            pPlayer.displayClientMessage(TextUtils.translate("gui.createrailwaysnavigator.time", TimeUtils.parseTime((int)(pLevel.getDayTime() % DragonLib.TICKS_PER_DAY + DragonLib.DAYTIME_SHIFT), ModClientConfig.TIME_FORMAT.get())), true);
        }
        return InteractionResult.SUCCESS;
    }

	@Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return pState.getValue(FACING) == Direction.NORTH || pState.getValue(FACING) == Direction.SOUTH ? SHAPE_SN : SHAPE_EW;
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
        pBuilder.add(FACING);
    }

    @Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public Class<TrainStationClockBlockEntity> getBlockEntityClass() {
        return TrainStationClockBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TrainStationClockBlockEntity> getBlockEntityType() {
       return ModBlockEntities.TRAIN_STATION_CLOCK_BLOCK_ENTITY.get();
    }
}
