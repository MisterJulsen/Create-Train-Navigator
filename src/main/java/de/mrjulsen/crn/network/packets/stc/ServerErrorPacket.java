package de.mrjulsen.crn.network.packets.stc;

import java.util.function.Supplier;

import de.mrjulsen.crn.client.ClientWrapper;
import de.mrjulsen.crn.network.NetworkManager;
import de.mrjulsen.mcdragonlib.network.IPacketBase;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

public class ServerErrorPacket implements IPacketBase<ServerErrorPacket> {
    public String message;
    
    public ServerErrorPacket() { }

    public ServerErrorPacket(String message) {
        this.message = message;
    }

    @Override
    public void encode(ServerErrorPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.message);
    }

    @Override
    public ServerErrorPacket decode(FriendlyByteBuf buffer) {        
        return new ServerErrorPacket(buffer.readUtf());
    }

    @Override
    public void handle(ServerErrorPacket packet, Supplier<NetworkEvent.Context> context) {        
        context.get().enqueueWork(() ->
        {
            NetworkManager.executeOnClient(() -> {
                ClientWrapper.handleErrorMessagePacket(packet, context);
            });
        });
        
        context.get().setPacketHandled(true);      
    }    
    
    @Override
    public NetworkDirection getDirection() {
        return NetworkDirection.PLAY_TO_CLIENT;
    }
}

