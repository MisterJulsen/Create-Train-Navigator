package de.mrjulsen.crn.network.packets.cts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.UUID;
import java.util.function.Supplier;

import de.mrjulsen.crn.ExampleMod;
import de.mrjulsen.crn.data.GlobalSettingsManager;
import de.mrjulsen.crn.data.DeparturePrediction.SimpleDeparturePrediction;
import de.mrjulsen.crn.network.packets.stc.RealtimeResponsePacket;
import de.mrjulsen.crn.util.TrainUtils;
import de.mrjulsen.mcdragonlib.net.IPacketBase;
import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

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
    public void handle(RealtimeRequestPacket packet, Supplier<PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {
            final Level level = contextSupplier.get().getPlayer().getLevel();
            new Thread(() -> {
                final long updateTime = level.getDayTime();
                Collection<SimpleDeparturePrediction> predictions = new ArrayList<>();
                packet.ids.forEach(x -> {
                    if (!TrainUtils.isTrainIdValid(x)) {
                        return;
                    }
                    
                    predictions.addAll(TrainUtils.getTrainDeparturePredictions(x, contextSupplier.get().getPlayer().getLevel()).stream().map(a -> a.simplify()).filter(a -> !GlobalSettingsManager.getInstance().getSettingsData().isBlacklisted(a.stationName())).sorted(Comparator.comparingInt(a -> a.departureTicks())).toList());
                });
                ExampleMod.net().CHANNEL.sendToPlayer((ServerPlayer)contextSupplier.get().getPlayer(), (new RealtimeResponsePacket(packet.requestId, predictions, updateTime)));
            }, "Realtime Provider").run();
        });
    }
    
    public static record StationData(Collection<String> stationName, Collection<Integer> indices, UUID trainId) {}
}
