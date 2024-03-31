package de.mrjulsen.crn.network.packets.cts;

import java.util.function.Supplier;

import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.block.AbstractAdvancedDisplayBlock;
import de.mrjulsen.crn.block.be.AdvancedDisplayBlockEntity;
import de.mrjulsen.crn.data.ESide;
import de.mrjulsen.mcdragonlib.network.IPacketBase;
import de.mrjulsen.mcdragonlib.network.NetworkManagerBase;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

public class AdvancedDisplayUpdatePacket implements IPacketBase<AdvancedDisplayUpdatePacket> {
    private BlockPos pos;
    private CompoundTag nbt;
    private ESide side;

    public AdvancedDisplayUpdatePacket() {}

    public AdvancedDisplayUpdatePacket(BlockPos pos, AdvancedDisplayBlockEntity blockEntity, ESide side) {
        this.pos = pos;
        this.nbt = blockEntity.serialize();
        this.side = side;
    }

    public AdvancedDisplayUpdatePacket(BlockPos pos, CompoundTag nbt, ESide side) {
        this.pos = pos;
        this.nbt = nbt;
        this.side = side;
    }

    @Override
    public void encode(AdvancedDisplayUpdatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeNbt(packet.nbt);
        buffer.writeEnum(packet.side);
    }

    @Override
    public AdvancedDisplayUpdatePacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        CompoundTag nbt = buffer.readNbt();
        ESide side = buffer.readEnum(ESide.class);

        return new AdvancedDisplayUpdatePacket(pos, nbt, side);
    }

    @Override
    public void handle(AdvancedDisplayUpdatePacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkManagerBase.handlePacket(packet, context, () -> {
            ServerPlayer player = context.get().getSender();
            if (player != null) {
                Level level = player.getLevel();
                if (level.isLoaded(packet.pos)) {
                    if (level.getBlockEntity(packet.pos) instanceof AdvancedDisplayBlockEntity blockEntity) {
                        blockEntity.applyToAll(be -> {
                            be.deserialize(packet.nbt);
                            if (level.getBlockState(be.getBlockPos()).getBlock() instanceof AbstractAdvancedDisplayBlock) {
                                level.setBlockAndUpdate(be.getBlockPos(), level.getBlockState(be.getBlockPos()).setValue(AbstractAdvancedDisplayBlock.SIDE, packet.side));
                            }
                            be.notifyUpdate();
                        });
                    }

                    
                }
            }
        });
    }

    @Override
    public NetworkDirection getDirection() {
        return NetworkDirection.PLAY_TO_SERVER;
    }
}
