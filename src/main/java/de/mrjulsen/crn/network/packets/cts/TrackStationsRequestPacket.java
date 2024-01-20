package de.mrjulsen.crn.network.packets.cts;

import java.util.function.Supplier;

import de.mrjulsen.crn.network.NetworkManager;
import de.mrjulsen.crn.network.packets.IPacketBase;
import de.mrjulsen.crn.network.packets.stc.TrackStationResponsePacket;
import de.mrjulsen.crn.util.TrainUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

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
    public void handle(TrackStationsRequestPacket packet, Supplier<NetworkEvent.Context> context) {        
        context.get().enqueueWork(() ->
        {
            NetworkManager.sendToClient(new TrackStationResponsePacket(
                packet.id,
                TrainUtils.getAllStations().stream().map(x -> x.name).toList(),
                TrainUtils.getAllTrains().stream().map(x -> x.name.getString()).toList()
            ), context.get().getSender());
        });
        
        context.get().setPacketHandled(true);      
    }    
}
