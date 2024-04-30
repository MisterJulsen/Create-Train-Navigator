package de.mrjulsen.crn.network.packets.cts;

import java.util.function.Supplier;

import de.mrjulsen.crn.ExampleMod;
import de.mrjulsen.crn.data.NearestTrackStationResult;
import de.mrjulsen.crn.network.packets.stc.NearestStationResponsePacket;
import de.mrjulsen.crn.network.packets.stc.ServerErrorPacket;
import de.mrjulsen.crn.util.TrainUtils;
import de.mrjulsen.mcdragonlib.net.IPacketBase;
import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class NearestStationRequestPacket implements IPacketBase<NearestStationRequestPacket> {

    public long id;
    public BlockPos pos;

    public NearestStationRequestPacket() { }
    
    public NearestStationRequestPacket(long id, Vec3 pos) {
        this(id, new BlockPos(pos));
    }

    public NearestStationRequestPacket(long id, BlockPos pos) {
        this.id = id;
        this.pos = pos;
    }

    @Override
    public void encode(NearestStationRequestPacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.id);
        buffer.writeBlockPos(packet.pos);
    }

    @Override
    public NearestStationRequestPacket decode(FriendlyByteBuf buffer) {
        long id = buffer.readLong();
        BlockPos pos = buffer.readBlockPos();
        return new NearestStationRequestPacket(id, pos);
    }
    
    @Override
    public void handle(NearestStationRequestPacket packet, Supplier<PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {
            Thread navigationThread = new Thread(() -> {   
                NearestTrackStationResult result = NearestTrackStationResult.empty();
                try {
                    result = TrainUtils.getNearestTrackStation(contextSupplier.get().getPlayer().getLevel(), packet.pos);                    
                } catch (Exception e) {
                    ExampleMod.LOGGER.error("Error while trying to find nearest track station ", e);
                    ExampleMod.net().CHANNEL.sendToPlayer((ServerPlayer)contextSupplier.get().getPlayer(), new ServerErrorPacket(e.getMessage()));
                } finally {                    
                    ExampleMod.net().CHANNEL.sendToPlayer((ServerPlayer)contextSupplier.get().getPlayer(), new NearestStationResponsePacket(packet.id, result));
                }                
            });
            navigationThread.setPriority(Thread.MIN_PRIORITY);
            navigationThread.setName("Station Location Calculator");
            navigationThread.start();
        });
    }   
}
