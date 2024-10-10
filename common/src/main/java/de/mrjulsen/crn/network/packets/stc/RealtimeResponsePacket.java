package de.mrjulsen.crn.network.packets.stc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import de.mrjulsen.crn.data.DeparturePrediction.SimpleDeparturePrediction;
import de.mrjulsen.crn.network.InstanceManager;
import de.mrjulsen.mcdragonlib.net.IPacketBase;
import dev.architectury.networking.NetworkManager.PacketContext;
import dev.architectury.utils.EnvExecutor;
import dev.architectury.utils.Env;
import net.minecraft.network.FriendlyByteBuf;

public class RealtimeResponsePacket implements IPacketBase<RealtimeResponsePacket> {
    public long id;
    public Collection<SimpleDeparturePrediction> departure;
    public long time;
    
    public RealtimeResponsePacket() { }

    public RealtimeResponsePacket(long id, Collection<SimpleDeparturePrediction> departure, long time) {
        this.id = id;
        this.departure = departure;
        this.time = time;
    }

    @Override
    public void encode(RealtimeResponsePacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.id);
        buffer.writeLong(packet.time);
        buffer.writeInt(packet.departure.size());
        for (SimpleDeparturePrediction s : packet.departure) {
            buffer.writeNbt(s.toNbt());
        }
    }

    @Override
    public RealtimeResponsePacket decode(FriendlyByteBuf buffer) {
        long id = buffer.readLong();
        long time = buffer.readLong();
        int count = buffer.readInt();
        List<SimpleDeparturePrediction> departure = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            departure.add(SimpleDeparturePrediction.fromNbt(buffer.readNbt()));
        }
        return new RealtimeResponsePacket(id, departure, time);
    }
    
    @Override
    public void handle(RealtimeResponsePacket packet, Supplier<PacketContext> contextSupplier) {
        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> {
            contextSupplier.get().queue(() -> {
                new Thread(() -> {
                    InstanceManager.runClientRealtimeResponseAction(packet.id, packet.departure, packet.time);
                }, "Realtime Processor").run();
            });
        });
    }
}

