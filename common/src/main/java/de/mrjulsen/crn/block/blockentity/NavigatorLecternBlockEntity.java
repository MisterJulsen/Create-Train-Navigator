package de.mrjulsen.crn.block.blockentity;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import de.mrjulsen.crn.block.NavigatorLecternBlock;
import de.mrjulsen.crn.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class NavigatorLecternBlockEntity extends SmartBlockEntity {

    private static final String NBT_NAVIGATOR = "Navigator";
    private CompoundTag navigatorNbt = new CompoundTag();

    public NavigatorLecternBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) { }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        if (navigatorNbt != null) compound.put(NBT_NAVIGATOR, navigatorNbt);
    }

    @Override
    public void writeSafe(CompoundTag compound) {
        super.writeSafe(compound);
        if (navigatorNbt != null) compound.put(NBT_NAVIGATOR, navigatorNbt);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        navigatorNbt = compound.contains(NBT_NAVIGATOR) ? compound.getCompound(NBT_NAVIGATOR) : new CompoundTag();
    }

    public void setNavigator(ItemStack newNavigator) {
        if (newNavigator != null) {
            this.navigatorNbt = newNavigator.getOrCreateTag();
            level.playSound(null, getBlockPos(), SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.8F, 1.0F);
        }
    }

    public void swapControllers(ItemStack stack, Player player, InteractionHand hand, BlockState state) {
        ItemStack newController = stack.copy();
        stack.setCount(0);
        if (player.getItemInHand(hand).isEmpty()) {
            player.setItemInHand(hand, createNavigator());
        } else {
            dropController(state);
        }
        setNavigator(newController);
    }

    public void dropController(BlockState state) {
        Direction dir = state.getValue(NavigatorLecternBlock.FACING);
        double x = worldPosition.getX() + 0.5 + 0.25 * dir.getStepX();
        double y = worldPosition.getY() + 1;
        double z = worldPosition.getZ() + 0.5 + 0.25 * dir.getStepZ();
        ItemEntity itementity = new ItemEntity(level, x, y, z, createNavigator());
        itementity.setDefaultPickUpDelay();
        level.addFreshEntity(itementity);
        navigatorNbt = new CompoundTag();
        level.playSound(null, getBlockPos(), SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8F, 1.0F);
    }

    public static boolean playerInRange(Player player, Level world, BlockPos pos) {
        //double modifier = world.isRemote ? 0 : 1.0;
        double reach = 5;// + modifier;
        return player.distanceToSqr(Vec3.atCenterOf(pos)) < reach * reach;
    }

    private ItemStack createNavigator() {
        ItemStack stack = ModItems.NAVIGATOR.asStack();
        stack.setTag(navigatorNbt == null ? new CompoundTag() : navigatorNbt);
        return stack;
    }

}
