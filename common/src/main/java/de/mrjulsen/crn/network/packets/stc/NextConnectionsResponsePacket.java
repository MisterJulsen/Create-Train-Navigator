package de.mrjulsen.crn.network.packets.stc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import de.mrjulsen.crn.data.SimpleTrainConnection;
import de.mrjulsen.crn.network.InstanceManager;
import de.mrjulsen.mcdragonlib.net.IPacketBase;
import dev.architectury.networking.NetworkManager.PacketContext;
import dev.architectury.utils.EnvExecutor;
import dev.architectury.utils.Env;
import net.minecraft.network.FriendlyByteBuf;

public class NextConnectionsResponsePacket implements IPacketBase<NextConnectionsResponsePacket> {
    public long id;
    public Collection<SimpleTrainConnection> connections;
    public long time;
    
    public NextConnectionsResponsePacket() { }

    public NextConnectionsResponsePacket(long id, Collection<SimpleTrainConnection> connections, long time) {
        this.id = id;
        this.connections = connections;
        this.time = time;
    }

    @Override
    public void encode(NextConnectionsResponsePacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.id);
        buffer.writeLong(packet.time);
        buffer.writeInt(packet.connections.size());
        for (SimpleTrainConnection s : packet.connections) {
            buffer.writeNbt(s.toNbt());
        }
    }

    @Override
    public NextConnectionsResponsePacket decode(FriendlyByteBuf buffer) {
        long id = buffer.readLong();
        long time = buffer.readLong();
        int count = buffer.readInt();
        List<SimpleTrainConnection> connections = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            connections.add(SimpleTrainConnection.fromNbt(buffer.readNbt()));
        }
        return new NextConnectionsResponsePacket(id, connections, time);
    }
    
    @Override
    public void handle(NextConnectionsResponsePacket packet, Supplier<PacketContext> contextSupplier) {
        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> {
            contextSupplier.get().queue(() -> {
                InstanceManager.runClientNextConnectionsResponseAction(packet.id, packet.connections, packet.time);
            });
        });
    }
}

