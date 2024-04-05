package de.mrjulsen.crn.network.packets.cts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.UUID;
import java.util.function.Supplier;

import de.mrjulsen.crn.data.DeparturePrediction.SimpleDeparturePrediction;
import de.mrjulsen.crn.network.NetworkManager;
import de.mrjulsen.mcdragonlib.network.IPacketBase;
import de.mrjulsen.crn.network.packets.stc.RealtimeResponsePacket;
import de.mrjulsen.crn.util.TrainUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkDirection;
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
            final Level level = context.get().getSender().level();
            new Thread(() -> {
                final long updateTime = level.getDayTime();
                Collection<SimpleDeparturePrediction> predictions = new ArrayList<>();
                packet.ids.forEach(x -> {
                    if (!TrainUtils.isTrainIdValid(x)) {
                        return;
                    }
                    
                    predictions.addAll(TrainUtils.getTrainDeparturePredictions(x, context.get().getSender().level()).stream().map(a -> a.simplify()).sorted(Comparator.comparingInt(a -> a.departureTicks())).toList());
                });
                NetworkManager.getInstance().sendToClient(new RealtimeResponsePacket(packet.requestId, predictions, updateTime), context.get().getSender());
            }, "Realtime Provider").run();
        });
        
        context.get().setPacketHandled(true);
    }
    
    @Override
    public NetworkDirection getDirection() {
        return NetworkDirection.PLAY_TO_SERVER;
    }    
    
    public static record StationData(Collection<String> stationName, Collection<Integer> indices, UUID trainId) {}
}
