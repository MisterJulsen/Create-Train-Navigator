package de.mrjulsen.crn.block;

import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.block.IBE;
import de.mrjulsen.crn.api.client.Screens;
import de.mrjulsen.crn.block.blockentity.NavigatorLecternBlockEntity;
import de.mrjulsen.crn.registry.ModBlockEntities;
import de.mrjulsen.crn.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;

public class NavigatorLecternBlock extends LecternBlock implements IBE<NavigatorLecternBlockEntity>, SpecialBlockItemRequirement {

    public NavigatorLecternBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(HAS_BOOK, true));
    }

    @Override
    public Class<NavigatorLecternBlockEntity> getBlockEntityClass() {
        return NavigatorLecternBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends NavigatorLecternBlockEntity> getBlockEntityType() {
        return ModBlockEntities.NAVIGATOR_LECTERN_BLOCK_ENTITY.get();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return IBE.super.newBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.isShiftKeyDown() && NavigatorLecternBlockEntity.playerInRange(player, world, pos)) {
            if (world.isClientSide) {
                Screens.showNavigatorScreen(null, true);
            }
            return InteractionResult.SUCCESS;
        }

        if (player.isShiftKeyDown()) {
            if (!world.isClientSide) {
                replaceWithLectern(state, world, pos);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!world.isClientSide)
                withBlockEntityDo(world, pos, be -> be.dropController(state));

            super.onRemove(state, world, pos, newState, isMoving);
        }
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        return 15;
    }

    public void replaceLectern(BlockState lecternState, Level world, BlockPos pos, ItemStack navigator) {
        world.setBlockAndUpdate(pos, defaultBlockState()
                .setValue(FACING, lecternState.getValue(FACING))
                .setValue(POWERED, lecternState.getValue(POWERED))
        );
        withBlockEntityDo(world, pos, be -> be.setNavigator(navigator));
    }

    public void replaceWithLectern(BlockState state, Level world, BlockPos pos) {
        world.setBlockAndUpdate(pos, Blocks.LECTERN.defaultBlockState()
                .setValue(FACING, state.getValue(FACING))
                .setValue(POWERED, state.getValue(POWERED)));
    }

    @SuppressWarnings("deprecation")
    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return Blocks.LECTERN.getCloneItemStack(level, pos, state);
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState state, BlockEntity be) {
        ArrayList<ItemStack> requiredItems = new ArrayList<>();
        requiredItems.add(new ItemStack(Blocks.LECTERN));
        requiredItems.add(new ItemStack(ModItems.NAVIGATOR.get()));
        return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, requiredItems);
    }
}
