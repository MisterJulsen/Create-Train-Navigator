package de.mrjulsen.crn.network.packets.cts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.UUID;
import java.util.function.Supplier;

import de.mrjulsen.crn.data.DeparturePrediction.SimpleDeparturePrediction;
import de.mrjulsen.crn.network.NetworkManager;
import de.mrjulsen.crn.network.packets.IPacketBase;
import de.mrjulsen.crn.network.packets.stc.RealtimeResponsePacket;
import de.mrjulsen.crn.util.TrainUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class RealtimeRequestPacket implements IPacketBase<RealtimeRequestPacket> {

    public long requestId;
    public Collection<UUID> ids;

    public RealtimeRequestPacket() { }
    
    public RealtimeRequestPacket(long requestId, Collection<UUID> ids) {
        this.requestId = requestId;
        this.ids = ids;
    }

    @Override
    public void encode(RealtimeRequestPacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.requestId);
        buffer.writeInt(packet.ids.size());
        for (UUID u : packet.ids) {
            buffer.writeUUID(u);
        }
    }

    @Override
    public RealtimeRequestPacket decode(FriendlyByteBuf buffer) {
        long requestId = buffer.readLong();
        int count = buffer.readInt();
        Collection<UUID> uuids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            uuids.add(buffer.readUUID());
        }
        return new RealtimeRequestPacket(requestId, uuids);
    }

    @Override
    public void handle(RealtimeRequestPacket packet, Supplier<NetworkEvent.Context> context) {        
        context.get().enqueueWork(() ->
        {
            new Thread(() -> {
                final long updateTime = context.get().getSender().level.getDayTime();
                Collection<SimpleDeparturePrediction> predictions = new ArrayList<>();
                packet.ids.forEach(x -> predictions.addAll(TrainUtils.getTrainDeparturePredictions(x).stream().map(a -> a.simplify()).sorted(Comparator.comparingInt(a -> a.ticks())).toList()));
                NetworkManager.sendToClient(new RealtimeResponsePacket(packet.requestId, predictions, updateTime), context.get().getSender());
            }, "Realtime Reader").run();
        });
        
        context.get().setPacketHandled(true);
    }
    
    public static record StationData(Collection<String> stationName, Collection<Integer> indices, UUID trainId) {}
}
