package de.mrjulsen.crn.network.packets.cts;

import java.util.UUID;
import java.util.function.Supplier;

import de.mrjulsen.crn.network.NetworkManager;
import de.mrjulsen.crn.network.packets.IPacketBase;
import de.mrjulsen.crn.network.packets.stc.NextConnectionsResponsePacket;
import de.mrjulsen.crn.util.TrainUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class NextConnectionsRequestPacket implements IPacketBase<NextConnectionsRequestPacket> {

    public long requestId;
    public UUID trainId;
    public long ticksToNextStop;
    public String currentStationName;

    public NextConnectionsRequestPacket() { }
    
    public NextConnectionsRequestPacket(long requestId, UUID trainId, String currentStationName, long ticksToNextStop) {
        this.requestId = requestId;
        this.trainId = trainId;
        this.ticksToNextStop = ticksToNextStop;
        this.currentStationName = currentStationName;
    }

    @Override
    public void encode(NextConnectionsRequestPacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.requestId);
        buffer.writeUUID(packet.trainId);
        buffer.writeLong(packet.ticksToNextStop);
        buffer.writeUtf(packet.currentStationName);
    }

    @Override
    public NextConnectionsRequestPacket decode(FriendlyByteBuf buffer) {
        long requestId = buffer.readLong();
        UUID trainId = buffer.readUUID();
        long ticksToNextStop = buffer.readLong();
        String currentStationName = buffer.readUtf();
        
        return new NextConnectionsRequestPacket(requestId, trainId, currentStationName, ticksToNextStop);
    }

    @Override
    public void handle(NextConnectionsRequestPacket packet, Supplier<NetworkEvent.Context> context) {        
        context.get().enqueueWork(() ->
        {
            new Thread(() -> {
                final long updateTime = context.get().getSender().level.getDayTime();
                NetworkManager.sendToClient(new NextConnectionsResponsePacket(packet.requestId, TrainUtils.getConnectionsAt(packet.currentStationName, packet.trainId, (int)packet.ticksToNextStop), updateTime), context.get().getSender());
            }, "Connections Loader").run();
        });
        
        context.get().setPacketHandled(true);
    }
}
