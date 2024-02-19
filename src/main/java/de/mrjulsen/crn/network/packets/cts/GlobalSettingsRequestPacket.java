package de.mrjulsen.crn.network.packets.cts;

import java.util.function.Supplier;

import de.mrjulsen.crn.data.GlobalSettingsManager;
import de.mrjulsen.crn.network.NetworkManager;
import de.mrjulsen.mcdragonlib.network.IPacketBase;
import de.mrjulsen.crn.network.packets.stc.GlobalSettingsResponsePacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

public class GlobalSettingsRequestPacket implements IPacketBase<GlobalSettingsRequestPacket> {

    public long id;

    public GlobalSettingsRequestPacket() { }
    
    public GlobalSettingsRequestPacket(long id) {
        this.id = id;
    }

    @Override
    public void encode(GlobalSettingsRequestPacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.id);
    }

    @Override
    public GlobalSettingsRequestPacket decode(FriendlyByteBuf buffer) {
        long id = buffer.readLong();
        return new GlobalSettingsRequestPacket(id);
    }

    @Override
    public void handle(GlobalSettingsRequestPacket packet, Supplier<NetworkEvent.Context> context) {        
        context.get().enqueueWork(() ->
        {
            NetworkManager.getInstance().sendToClient(new GlobalSettingsResponsePacket(packet.id, GlobalSettingsManager.getInstance().getSettingsData()), context.get().getSender());
        });
        
        context.get().setPacketHandled(true);      
    }

    @Override
    public NetworkDirection getDirection() {
        return NetworkDirection.PLAY_TO_SERVER;
    }    
}
