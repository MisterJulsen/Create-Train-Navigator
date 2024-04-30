package de.mrjulsen.crn.network.packets.cts;

import java.util.function.Supplier;

import de.mrjulsen.crn.ExampleMod;
import de.mrjulsen.crn.event.listeners.TrainListener;
import de.mrjulsen.crn.network.packets.stc.TrackStationResponsePacket;
import de.mrjulsen.crn.util.TrainUtils;
import de.mrjulsen.mcdragonlib.net.IPacketBase;
import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class TrackStationsRequestPacket implements IPacketBase<TrackStationsRequestPacket> {

    public long id;

    public TrackStationsRequestPacket() { }
    
    public TrackStationsRequestPacket(long id) {
        this.id = id;
    }

    @Override
    public void encode(TrackStationsRequestPacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.id);
    }

    @Override
    public TrackStationsRequestPacket decode(FriendlyByteBuf buffer) {
        long id = buffer.readLong();
        return new TrackStationsRequestPacket(id);
    }
    
    @Override
    public void handle(TrackStationsRequestPacket packet, Supplier<PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {
            ExampleMod.net().CHANNEL.sendToPlayer((ServerPlayer)contextSupplier.get().getPlayer(), new TrackStationResponsePacket(
                packet.id,
                TrainUtils.getAllStations().stream().map(x -> x.name).toList(),
                TrainUtils.getAllTrains().stream().map(x -> x.name.getString()).toList(),
                TrainListener.getInstance().getListeningTrainCount(),
                TrainListener.getInstance().getTotalTrainCount()
            ));
        });
    }
}
