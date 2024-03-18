package de.mrjulsen.crn.network.packets.cts;

import java.util.function.Supplier;

import de.mrjulsen.crn.data.IBlockEntitySerializable;
import de.mrjulsen.mcdragonlib.network.IPacketBase;
import de.mrjulsen.mcdragonlib.network.NetworkManagerBase;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

public class UpdateBlockEntityPacket implements IPacketBase<UpdateBlockEntityPacket> {
    private BlockPos pos;
    private CompoundTag nbt;

    public UpdateBlockEntityPacket() {}

    public UpdateBlockEntityPacket(BlockPos pos, IBlockEntitySerializable blockEntity) {
        this.pos = pos;
        this.nbt = blockEntity.serialize();
    }

    public UpdateBlockEntityPacket(BlockPos pos, CompoundTag nbt) {
        this.pos = pos;
        this.nbt = nbt;
    }

    @Override
    public void encode(UpdateBlockEntityPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeNbt(packet.nbt);
    }

    @Override
    public UpdateBlockEntityPacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        CompoundTag nbt = buffer.readNbt();

        return new UpdateBlockEntityPacket(pos, nbt);
    }

    @Override
    public void handle(UpdateBlockEntityPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkManagerBase.handlePacket(packet, context, () -> {
            ServerPlayer player = context.get().getSender();
            if (player != null) {
                Level level = player.getLevel();
                if (level.isLoaded(packet.pos)) {
                    if (level.getBlockEntity(packet.pos) instanceof IBlockEntitySerializable blockEntity) {
                        blockEntity.deserialize(packet.nbt);
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
