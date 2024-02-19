package de.mrjulsen.crn.network.packets.stc;

import java.util.function.Supplier;

import de.mrjulsen.crn.data.NearestTrackStationResult;
import de.mrjulsen.crn.network.InstanceManager;
import de.mrjulsen.crn.network.NetworkManager;
import de.mrjulsen.mcdragonlib.network.IPacketBase;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

public class NearestStationResponsePacket implements IPacketBase<NearestStationResponsePacket> {
    public long id;
    public NearestTrackStationResult result;
    
    public NearestStationResponsePacket() { }

    public NearestStationResponsePacket(long id, NearestTrackStationResult result) {
        this.id = id;
        this.result = result;
    }

    @Override
    public void encode(NearestStationResponsePacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.id);
        packet.result.serialize(buffer);
    }

    @Override
    public NearestStationResponsePacket decode(FriendlyByteBuf buffer) {
        long id = buffer.readLong();
        NearestTrackStationResult result = NearestTrackStationResult.deserialize(buffer);
        return new NearestStationResponsePacket(id, result);
    }

    @Override
    public void handle(NearestStationResponsePacket packet, Supplier<NetworkEvent.Context> context) {        
        context.get().enqueueWork(() ->
        {
            NetworkManager.executeOnClient(() -> {
                InstanceManager.runClientNearestStationResponseAction(packet.id, packet.result);
            });
        });
        
        context.get().setPacketHandled(true);      
    } 
    
    @Override
    public NetworkDirection getDirection() {
        return NetworkDirection.PLAY_TO_CLIENT;
    }   
}

