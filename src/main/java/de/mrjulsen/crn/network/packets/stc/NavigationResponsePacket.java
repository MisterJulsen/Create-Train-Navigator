package de.mrjulsen.crn.network.packets.stc;

import java.util.List;
import java.util.ArrayList;
import java.util.function.Supplier;

import de.mrjulsen.crn.data.SimpleRoute;
import de.mrjulsen.crn.network.InstanceManager;
import de.mrjulsen.crn.network.NetworkManager;
import de.mrjulsen.crn.network.packets.IPacketBase;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class NavigationResponsePacket implements IPacketBase<NavigationResponsePacket> {
    public long id;
    public List<SimpleRoute> routes; 
    
    public NavigationResponsePacket() { }

    public NavigationResponsePacket(long id, List<SimpleRoute> routes) {
        this.id = id;
        this.routes = routes;
    }

    @Override
    public void encode(NavigationResponsePacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.id);
        buffer.writeInt(packet.routes.size());
        for (SimpleRoute route : packet.routes) {
            buffer.writeNbt(route.toNbt());
        }
    }

    @Override
    public NavigationResponsePacket decode(FriendlyByteBuf buffer) {
        long id = buffer.readLong();
        int routesCount = buffer.readInt();
        List<SimpleRoute> routes = new ArrayList<>(routesCount);
        for (int i = 0; i < routesCount; i++) {
            routes.add(SimpleRoute.fromNbt(buffer.readNbt()));
        }
        return new NavigationResponsePacket(id, routes);
    }

    @Override
    public void handle(NavigationResponsePacket packet, Supplier<NetworkEvent.Context> context) {        
        context.get().enqueueWork(() ->
        {
            NetworkManager.executeOnClient(() -> {
                InstanceManager.runClientNavigationResponseAction(packet.id, packet.routes);
            });
        });
        
        context.get().setPacketHandled(true);      
    }    
}

