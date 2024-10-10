package de.mrjulsen.crn.network.packets.stc;

import java.util.List;
import java.util.ArrayList;
import java.util.function.Supplier;

import de.mrjulsen.crn.data.SimpleRoute;
import de.mrjulsen.crn.network.InstanceManager;
import de.mrjulsen.mcdragonlib.net.IPacketBase;
import dev.architectury.networking.NetworkManager.PacketContext;
import dev.architectury.utils.EnvExecutor;
import dev.architectury.utils.Env;
import net.minecraft.network.FriendlyByteBuf;

public class NavigationResponsePacket implements IPacketBase<NavigationResponsePacket> {
    public long id;
    public List<SimpleRoute> routes; 
    public long duration;
    public long lastUpdated;
    
    public NavigationResponsePacket() { }

    public NavigationResponsePacket(long id, List<SimpleRoute> routes, long duration, long lastUpdated) {
        this.id = id;
        this.routes = routes;
        this.duration = duration;
        this.lastUpdated = lastUpdated;
    }

    @Override
    public void encode(NavigationResponsePacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.id);
        buffer.writeLong(packet.duration);
        buffer.writeLong(packet.lastUpdated);
        buffer.writeInt(packet.routes.size());
        for (SimpleRoute route : packet.routes) {
            buffer.writeNbt(route.toNbt());
        }
    }

    @Override
    public NavigationResponsePacket decode(FriendlyByteBuf buffer) {
        long id = buffer.readLong();
        long duration = buffer.readLong();
        long lastUpdated = buffer.readLong();
        int routesCount = buffer.readInt();        
        List<SimpleRoute> routes = new ArrayList<>(routesCount);
        for (int i = 0; i < routesCount; i++) {
            routes.add(SimpleRoute.fromNbt(buffer.readNbt()));
        }
        return new NavigationResponsePacket(id, routes, duration, lastUpdated);
    }

    @Override
    public void handle(NavigationResponsePacket packet, Supplier<PacketContext> contextSupplier) {
        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> {
            contextSupplier.get().queue(() -> {
                InstanceManager.runClientNavigationResponseAction(packet.id, packet.routes, new NavigationResponseData(packet.lastUpdated, packet.duration));
            });
        });
    }

    public static record NavigationResponseData(long lastUpdated, long calculationTime) {}
}

