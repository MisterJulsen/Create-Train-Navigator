package de.mrjulsen.crn.network.packets.stc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import de.mrjulsen.crn.data.ClientTrainStationSnapshot;
import de.mrjulsen.crn.network.InstanceManager;
import de.mrjulsen.crn.network.NetworkManager;
import de.mrjulsen.crn.network.packets.IPacketBase;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class TrackStationResponsePacket implements IPacketBase<TrackStationResponsePacket> {
    public long id;
    public Collection<String> stationNames;
    public Collection<String> trainNames;
    
    public TrackStationResponsePacket() { }

    public TrackStationResponsePacket(long id, Collection<String> stationNames, Collection<String> trainNames) {
        this.id = id;
        this.stationNames = stationNames;
        this.trainNames = trainNames;
    }

    @Override
    public void encode(TrackStationResponsePacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.id);
        buffer.writeInt(packet.stationNames.size());
        for (String s : packet.stationNames) {
            buffer.writeUtf(s);
        }

        buffer.writeInt(packet.trainNames.size());
        for (String s : packet.trainNames) {
            buffer.writeUtf(s);
        }
    }

    @Override
    public TrackStationResponsePacket decode(FriendlyByteBuf buffer) {
        long id = buffer.readLong();
        int count = buffer.readInt();
        List<String> stationNames = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            stationNames.add(buffer.readUtf());
        }

        count = buffer.readInt();
        List<String> trainNames = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            trainNames.add(buffer.readUtf());
        }
        return new TrackStationResponsePacket(id, stationNames, trainNames);
    }

    @Override
    public void handle(TrackStationResponsePacket packet, Supplier<NetworkEvent.Context> context) {        
        context.get().enqueueWork(() ->
        {
            NetworkManager.executeOnClient(() -> {
                ClientTrainStationSnapshot.makeNew(
                    packet.stationNames == null || packet.stationNames.isEmpty() ? new ArrayList<>() : new ArrayList<>(packet.stationNames),
                    packet.trainNames == null || packet.trainNames.isEmpty() ? new ArrayList<>() : new ArrayList<>(packet.trainNames)
                );
                InstanceManager.runClientResponseReceievedAction(packet.id);
            });
        });
        
        context.get().setPacketHandled(true);      
    }    
}

